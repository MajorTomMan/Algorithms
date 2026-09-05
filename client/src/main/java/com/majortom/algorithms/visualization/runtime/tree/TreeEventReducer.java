package com.majortom.algorithms.visualization.runtime.tree;

import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.event.observation.ObservationEvent;
import com.majortom.algorithms.core.event.structure.TreeStructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Reduces factual tree mutations plus factual runtime observations into replayable presentation state. */
public final class TreeEventReducer implements EventReducer<TreeViewState> {
    private final TreeViewState initialState;

    public TreeEventReducer() {
        this(TreeViewState.empty(TreeViewState.Kind.BINARY));
    }

    public TreeEventReducer(TreeViewState initialState) {
        this.initialState = Objects.requireNonNull(initialState, "initialState");
    }

    @Override
    public TreeViewState initialState() {
        return initialState;
    }

    @Override
    public Reduction<TreeViewState> reduce(TreeViewState previous, EventEnvelope envelope) {
        Object event = envelope.event();
        if (event instanceof TreeStructureEvent.NodeInserted inserted) {
            Map<Long, TreeViewState.Node> nodes = mutableNodes(previous);
            TreeViewState.Node node = previous.kind() == TreeViewState.Kind.GENERAL
                    ? TreeViewState.Node.general(inserted.nodeId(), (Integer) inserted.value(), List.of())
                    : TreeViewState.Node.binary(inserted.nodeId(), (Integer) inserted.value(), null, null);
            nodes.put(inserted.nodeId(), node);
            return changed(copy(previous, previous.rootId(), nodes,
                    Set.of(inserted.nodeId()), Set.of(), previous.visitedNodeIds(), false));
        }
        if (event instanceof TreeStructureEvent.NodeRemoved removed) {
            Map<Long, TreeViewState.Node> nodes = mutableNodes(previous);
            nodes.remove(removed.nodeId());
            Set<Long> visited = new LinkedHashSet<>(previous.visitedNodeIds());
            visited.remove(removed.nodeId());
            return changed(copy(previous, previous.rootId(), nodes, Set.of(), Set.of(), visited, false));
        }
        if (event instanceof TreeStructureEvent.ValueChanged changed) {
            TreeViewState.Node node = previous.nodes().get(changed.nodeId());
            if (node == null) {
                return Reduction.unchanged(previous, EventImportance.TRANSIENT);
            }
            Map<Long, TreeViewState.Node> nodes = mutableNodes(previous);
            nodes.put(changed.nodeId(), node.withValue((Integer) changed.value()));
            return changed(copy(previous, previous.rootId(), nodes,
                    Set.of(changed.nodeId()), Set.of(), previous.visitedNodeIds(), false));
        }
        if (event instanceof TreeStructureEvent.ChildInserted inserted) {
            TreeViewState.Node parent = previous.nodes().get(inserted.parentId());
            if (parent == null) {
                return Reduction.unchanged(previous, EventImportance.TRANSIENT);
            }
            List<Long> children = new ArrayList<>(parent.childIds());
            int index = Math.max(0, Math.min(inserted.index(), children.size()));
            children.add(index, inserted.childId());
            Map<Long, TreeViewState.Node> nodes = mutableNodes(previous);
            nodes.put(inserted.parentId(), parent.withChildren(children));
            return changed(copy(previous, previous.rootId(), nodes,
                    Set.of(inserted.parentId()), existing(previous, inserted.childId()),
                    previous.visitedNodeIds(), false));
        }
        if (event instanceof TreeStructureEvent.ChildRemoved removed) {
            TreeViewState.Node parent = previous.nodes().get(removed.parentId());
            if (parent == null) {
                return Reduction.unchanged(previous, EventImportance.TRANSIENT);
            }
            List<Long> children = new ArrayList<>(parent.childIds());
            if (removed.index() >= 0 && removed.index() < children.size()
                    && children.get(removed.index()) == removed.childId()) {
                children.remove(removed.index());
            } else {
                children.remove(removed.childId());
            }
            Map<Long, TreeViewState.Node> nodes = mutableNodes(previous);
            nodes.put(removed.parentId(), parent.withChildren(children));
            return changed(copy(previous, previous.rootId(), nodes,
                    Set.of(removed.parentId()), existing(previous, removed.childId()),
                    previous.visitedNodeIds(), false));
        }
        if (event instanceof TreeStructureEvent.LeftChanged changed) {
            return relationChanged(previous, changed.nodeId(), changed.childId(), true);
        }
        if (event instanceof TreeStructureEvent.RightChanged changed) {
            return relationChanged(previous, changed.nodeId(), changed.childId(), false);
        }
        if (event instanceof TreeStructureEvent.RootChanged changed) {
            Set<Long> current = changed.rootId() == null ? Set.of() : Set.of(changed.rootId());
            Set<Long> observed = changed.previousRootId() == null
                    ? Set.of() : existing(previous, changed.previousRootId());
            return changed(copy(previous, changed.rootId(), previous.nodes(), current, observed,
                    previous.visitedNodeIds(), false));
        }
        if (event instanceof ObservationEvent.Visited visited) {
            Long id = treeEntityId(visited.ref());
            if (id == null || !previous.nodes().containsKey(id)) {
                return Reduction.unchanged(previous, EventImportance.TRANSIENT);
            }
            Set<Long> visitedIds = new LinkedHashSet<>(previous.visitedNodeIds());
            visitedIds.add(id);
            return changed(copy(previous, previous.rootId(), previous.nodes(), Set.of(id), Set.of(), visitedIds, false));
        }
        if (event instanceof ObservationEvent.Compared compared) {
            Set<Long> observed = treeEntityIds(compared.leftRef(), compared.rightRef());
            if (observed.isEmpty()) {
                return Reduction.unchanged(previous, EventImportance.TRANSIENT);
            }
            return changed(copy(previous, previous.rootId(), previous.nodes(), Set.of(), observed,
                    previous.visitedNodeIds(), false));
        }
        if (event instanceof ObservationEvent.Examined examined) {
            Long from = treeEntityId(examined.fromRef());
            Long to = treeEntityId(examined.toRef());
            Set<Long> current = from != null && previous.nodes().containsKey(from) ? Set.of(from) : Set.of();
            Set<Long> observed = to != null && previous.nodes().containsKey(to) ? Set.of(to) : Set.of();
            if (current.isEmpty() && observed.isEmpty()) {
                return Reduction.unchanged(previous, EventImportance.TRANSIENT);
            }
            return changed(copy(previous, previous.rootId(), previous.nodes(), current, observed,
                    previous.visitedNodeIds(), false));
        }
        if (event instanceof RunCompletedEvent) {
            return Reduction.changed(copy(previous, previous.rootId(), previous.nodes(), Set.of(), Set.of(),
                    previous.visitedNodeIds(), true), EventImportance.TERMINAL, true);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static Reduction<TreeViewState> relationChanged(TreeViewState previous, long nodeId, Long childId, boolean left) {
        TreeViewState.Node node = previous.nodes().get(nodeId);
        if (node == null) {
            return Reduction.unchanged(previous, EventImportance.TRANSIENT);
        }
        Map<Long, TreeViewState.Node> nodes = mutableNodes(previous);
        nodes.put(nodeId, left ? node.withLeft(childId) : node.withRight(childId));
        return changed(copy(previous, previous.rootId(), nodes,
                Set.of(nodeId), childId == null ? Set.of() : existing(previous, childId),
                previous.visitedNodeIds(), false));
    }

    private static TreeViewState copy(TreeViewState previous, Long rootId, Map<Long, TreeViewState.Node> nodes,
            Set<Long> current, Set<Long> observed, Set<Long> visited, boolean completed) {
        return new TreeViewState(previous.kind(), rootId, nodes, current, observed, visited, completed);
    }

    private static Set<Long> existing(TreeViewState state, long id) {
        return state.nodes().containsKey(id) ? Set.of(id) : Set.of();
    }

    private static Long treeEntityId(ObservationEvent.Reference reference) {
        if (reference instanceof ObservationEvent.EntityRef entity
                && "tree".equalsIgnoreCase(entity.domain())) {
            return entity.id();
        }
        return null;
    }

    private static Set<Long> treeEntityIds(ObservationEvent.Reference first, ObservationEvent.Reference second) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        Long firstId = treeEntityId(first);
        Long secondId = treeEntityId(second);
        if (firstId != null) ids.add(firstId);
        if (secondId != null) ids.add(secondId);
        return Set.copyOf(ids);
    }

    private static Map<Long, TreeViewState.Node> mutableNodes(TreeViewState state) {
        return new LinkedHashMap<>(state.nodes());
    }

    private static Reduction<TreeViewState> changed(TreeViewState state) {
        return Reduction.changed(state, EventImportance.STATE_CHANGE, true);
    }
}
