package com.majortom.algorithms.visualization.runtime.graph;

import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.event.structure.GraphStructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;

import java.util.ArrayList;
import java.util.List;

/** Reduces factual Graph mutations and Runtime lifecycle events into GraphViewState. */
public final class GraphEventReducer implements EventReducer<GraphViewState> {
    private final GraphSnapshot<Integer> initialGraph;

    public GraphEventReducer(GraphSnapshot<Integer> graph) {
        initialGraph = graph;
    }

    @Override
    public GraphViewState initialState() {
        return GraphViewState.initial(initialGraph);
    }

    @Override
    public Reduction<GraphViewState> reduce(GraphViewState previous, EventEnvelope envelope) {
        Object event = envelope.event();
        if (event instanceof GraphStructureEvent.VertexAdded added) {
            List<GraphViewState.Node> nodes = new ArrayList<>(previous.nodes());
            nodes.add(new GraphViewState.Node(added.vertexId(), (Integer) added.value()));
            return changed(new GraphViewState(previous.directed(), nodes, previous.edges(), false));
        }
        if (event instanceof GraphStructureEvent.VertexRemoved removed) {
            List<GraphViewState.Node> nodes = previous.nodes().stream()
                    .filter(node -> node.id() != removed.vertexId())
                    .toList();
            List<GraphViewState.Edge> edges = previous.edges().stream()
                    .filter(edge -> edge.fromId() != removed.vertexId() && edge.toId() != removed.vertexId())
                    .toList();
            return changed(new GraphViewState(previous.directed(), nodes, edges, false));
        }
        if (event instanceof GraphStructureEvent.EdgeAdded added) {
            List<GraphViewState.Edge> edges = new ArrayList<>(previous.edges());
            edges.add(new GraphViewState.Edge(added.edgeId(), added.fromId(), added.toId()));
            return changed(new GraphViewState(previous.directed(), previous.nodes(), edges, false));
        }
        if (event instanceof GraphStructureEvent.EdgeRemoved removed) {
            List<GraphViewState.Edge> edges = previous.edges().stream()
                    .filter(edge -> edge.id() != removed.edgeId())
                    .toList();
            return changed(new GraphViewState(previous.directed(), previous.nodes(), edges, false));
        }
        if (event instanceof RunCompletedEvent) {
            return Reduction.changed(
                    new GraphViewState(previous.directed(), previous.nodes(), previous.edges(), true),
                    EventImportance.TERMINAL,
                    true);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static Reduction<GraphViewState> changed(GraphViewState state) {
        return Reduction.changed(state, EventImportance.STATE_CHANGE, true);
    }
}
