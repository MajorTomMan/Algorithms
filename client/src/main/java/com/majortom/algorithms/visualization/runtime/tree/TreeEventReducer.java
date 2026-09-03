package com.majortom.algorithms.visualization.runtime.tree;

import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.event.structure.TreeStructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Reduces factual tree relationship mutations and Runtime lifecycle events. */
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
            return changed(copy(previous, previous.rootId(), nodes, false));
        }
        if (event instanceof TreeStructureEvent.NodeRemoved removed) {
            Map<Long, TreeViewState.Node> nodes = mutableNodes(previous);
            nodes.remove(removed.nodeId());
            return changed(copy(previous, previous.rootId(), nodes, false));
        }
        if (event instanceof TreeStructureEvent.ValueChanged changed) {
            TreeViewState.Node node = previous.nodes().get(changed.nodeId());
            if (node == null) {
                return Reduction.unchanged(previous, EventImportance.TRANSIENT);
            }
            Map<Long, TreeViewState.Node> nodes = mutableNodes(previous);
            nodes.put(changed.nodeId(), node.withValue((Integer) changed.value()));
            return changed(copy(previous, previous.rootId(), nodes, false));
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
            return changed(copy(previous, previous.rootId(), nodes, false));
        }
        if (event instanceof TreeStructureEvent.ChildRemoved removed) {
            TreeViewState.Node parent = previous.nodes().get(removed.parentId());
            if (parent == null) {
                return Reduction.unchanged(previous, EventImportance.TRANSIENT);
            }
            List<Long> children = new ArrayList<>(parent.childIds());
            if (removed.index() >= 0 && removed.index() < children.size() && children.get(removed.index()) == removed.childId()) {
                children.remove(removed.index());
            } else {
                children.remove(removed.childId());
            }
            Map<Long, TreeViewState.Node> nodes = mutableNodes(previous);
            nodes.put(removed.parentId(), parent.withChildren(children));
            return changed(copy(previous, previous.rootId(), nodes, false));
        }
        if (event instanceof TreeStructureEvent.LeftChanged changed) {
            TreeViewState.Node node = previous.nodes().get(changed.nodeId());
            if (node == null) {
                return Reduction.unchanged(previous, EventImportance.TRANSIENT);
            }
            Map<Long, TreeViewState.Node> nodes = mutableNodes(previous);
            nodes.put(changed.nodeId(), node.withLeft(changed.childId()));
            return changed(copy(previous, previous.rootId(), nodes, false));
        }
        if (event instanceof TreeStructureEvent.RightChanged changed) {
            TreeViewState.Node node = previous.nodes().get(changed.nodeId());
            if (node == null) {
                return Reduction.unchanged(previous, EventImportance.TRANSIENT);
            }
            Map<Long, TreeViewState.Node> nodes = mutableNodes(previous);
            nodes.put(changed.nodeId(), node.withRight(changed.childId()));
            return changed(copy(previous, previous.rootId(), nodes, false));
        }
        if (event instanceof TreeStructureEvent.RootChanged changed) {
            return changed(copy(previous, changed.rootId(), previous.nodes(), false));
        }
        if (event instanceof RunCompletedEvent) {
            return Reduction.changed(copy(previous, previous.rootId(), previous.nodes(), true), EventImportance.TERMINAL, true);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static TreeViewState copy(TreeViewState previous, Long rootId, Map<Long, TreeViewState.Node> nodes, boolean completed) {
        return new TreeViewState(previous.kind(), rootId, nodes, completed);
    }

    private static Map<Long, TreeViewState.Node> mutableNodes(TreeViewState state) {
        return new LinkedHashMap<>(state.nodes());
    }

    private static Reduction<TreeViewState> changed(TreeViewState state) {
        return Reduction.changed(state, EventImportance.STATE_CHANGE, true);
    }
}
