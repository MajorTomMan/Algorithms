package com.majortom.algorithms.visualization.runtime.linked;

import com.majortom.algorithms.core.event.structure.LinkedStructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;

import java.util.LinkedHashMap;
import java.util.Map;

/** Reduces only factual linked-list mutations into explicit node/link state. */
public final class LinkedListEventReducer implements EventReducer<LinkedListViewState> {
    private final LinkedListViewState initialState;

    public LinkedListEventReducer() {
        this(LinkedListViewState.empty());
    }

    public LinkedListEventReducer(LinkedListViewState initialState) {
        this.initialState = initialState;
    }

    @Override
    public LinkedListViewState initialState() {
        return initialState;
    }

    @Override
    public Reduction<LinkedListViewState> reduce(LinkedListViewState previous, EventEnvelope envelope) {
        Object event = envelope.event();
        if (event instanceof LinkedStructureEvent.NodeInserted inserted) {
            Map<Long, LinkedListViewState.Node> nodes = mutable(previous);
            nodes.put(inserted.nodeId(), new LinkedListViewState.Node(inserted.nodeId(), (Integer) inserted.value(), null, null));
            return changed(nodes);
        }
        if (event instanceof LinkedStructureEvent.NodeRemoved removed) {
            Map<Long, LinkedListViewState.Node> nodes = mutable(previous);
            nodes.remove(removed.nodeId());
            return changed(nodes);
        }
        if (event instanceof LinkedStructureEvent.ValueChanged changed) {
            Map<Long, LinkedListViewState.Node> nodes = mutable(previous);
            LinkedListViewState.Node node = requireNode(nodes, changed.nodeId());
            nodes.put(changed.nodeId(), node.withValue((Integer) changed.value()));
            return changed(nodes);
        }
        if (event instanceof LinkedStructureEvent.NextChanged changed) {
            Map<Long, LinkedListViewState.Node> nodes = mutable(previous);
            LinkedListViewState.Node node = requireNode(nodes, changed.nodeId());
            nodes.put(changed.nodeId(), node.withNext(changed.nextNodeId()));
            return changed(nodes);
        }
        if (event instanceof LinkedStructureEvent.PreviousChanged changed) {
            Map<Long, LinkedListViewState.Node> nodes = mutable(previous);
            LinkedListViewState.Node node = requireNode(nodes, changed.nodeId());
            nodes.put(changed.nodeId(), node.withPrevious(changed.previousNodeId()));
            return changed(nodes);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static Map<Long, LinkedListViewState.Node> mutable(LinkedListViewState state) {
        return new LinkedHashMap<>(state.nodes());
    }

    private static LinkedListViewState.Node requireNode(Map<Long, LinkedListViewState.Node> nodes, long nodeId) {
        LinkedListViewState.Node node = nodes.get(nodeId);
        if (node == null) {
            throw new IllegalStateException("Linked event references unknown node " + nodeId);
        }
        return node;
    }

    private static Reduction<LinkedListViewState> changed(Map<Long, LinkedListViewState.Node> nodes) {
        return Reduction.changed(new LinkedListViewState(nodes), EventImportance.STATE_CHANGE, true);
    }
}
