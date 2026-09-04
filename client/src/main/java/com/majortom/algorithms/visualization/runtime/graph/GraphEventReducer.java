package com.majortom.algorithms.visualization.runtime.graph;

import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.event.observation.ObservationEvent;
import com.majortom.algorithms.core.event.structure.GraphStructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;

import java.util.ArrayList;
import java.util.List;

/** Reduces factual Graph mutations, observations and Runtime lifecycle into GraphViewState. */
public final class GraphEventReducer implements EventReducer<GraphViewState> {
    private static final String VERTEX_DOMAIN = "graph.vertex";
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
            return changed(state(previous, nodes, previous.edges(), GraphViewState.Observation.none(), false));
        }
        if (event instanceof GraphStructureEvent.VertexRemoved removed) {
            List<GraphViewState.Node> nodes = previous.nodes().stream()
                    .filter(node -> node.id() != removed.vertexId())
                    .toList();
            List<GraphViewState.Edge> edges = previous.edges().stream()
                    .filter(edge -> edge.fromId() != removed.vertexId() && edge.toId() != removed.vertexId())
                    .toList();
            return changed(state(previous, nodes, edges, GraphViewState.Observation.none(), false));
        }
        if (event instanceof GraphStructureEvent.EdgeAdded added) {
            List<GraphViewState.Edge> edges = new ArrayList<>(previous.edges());
            edges.add(new GraphViewState.Edge(added.edgeId(), added.fromId(), added.toId()));
            return changed(state(previous, previous.nodes(), edges, GraphViewState.Observation.none(), false));
        }
        if (event instanceof GraphStructureEvent.EdgeRemoved removed) {
            List<GraphViewState.Edge> edges = previous.edges().stream()
                    .filter(edge -> edge.id() != removed.edgeId())
                    .toList();
            return changed(state(previous, previous.nodes(), edges, GraphViewState.Observation.none(), false));
        }
        if (event instanceof ObservationEvent.Visited visited) {
            Long nodeId = graphNodeId(visited.ref());
            if (nodeId != null) {
                return observation(state(previous, previous.nodes(), previous.edges(),
                        GraphViewState.Observation.visited(nodeId), false));
            }
        }
        if (event instanceof ObservationEvent.Examined examined) {
            Long fromId = graphNodeId(examined.fromRef());
            Long toId = graphNodeId(examined.toRef());
            if (fromId != null && toId != null) {
                return observation(state(previous, previous.nodes(), previous.edges(),
                        GraphViewState.Observation.examined(fromId, toId), false));
            }
        }
        if (event instanceof RunCompletedEvent) {
            return Reduction.changed(
                    state(previous, previous.nodes(), previous.edges(), GraphViewState.Observation.none(), true),
                    EventImportance.TERMINAL,
                    true);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static GraphViewState state(
            GraphViewState previous,
            List<GraphViewState.Node> nodes,
            List<GraphViewState.Edge> edges,
            GraphViewState.Observation observation,
            boolean completed) {
        return new GraphViewState(previous.directed(), nodes, edges, observation, completed);
    }

    private static Long graphNodeId(ObservationEvent.Reference reference) {
        if (reference instanceof ObservationEvent.EntityRef entity && VERTEX_DOMAIN.equals(entity.domain())) {
            return entity.id();
        }
        return null;
    }

    private static Reduction<GraphViewState> changed(GraphViewState state) {
        return Reduction.changed(state, EventImportance.STATE_CHANGE, true);
    }

    private static Reduction<GraphViewState> observation(GraphViewState state) {
        return Reduction.changed(state, EventImportance.TRANSIENT, true);
    }
}
