package com.majortom.algorithms.visualization.runtime.graph;

import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.core.snapshot.GraphSnapshotState;
import com.majortom.algorithms.core.snapshot.WeightedGraphSnapshot;
import com.majortom.algorithms.visualization.runtime.VisualValue;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable JavaFX-neutral Graph facts plus persistent visited and current observation state. */
public record GraphViewState(boolean directed, List<Node> nodes, List<Edge> edges, Set<Long> visitedNodeIds,
        Observation observation, boolean completed) {
    public GraphViewState {
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
        visitedNodeIds = Set.copyOf(Objects.requireNonNull(visitedNodeIds, "visitedNodeIds"));
        observation = Objects.requireNonNull(observation, "observation");
    }

    public static GraphViewState initial(GraphSnapshotState<?> graph) {
        Objects.requireNonNull(graph, "graph");
        if (graph instanceof GraphSnapshot<?> basic) return initialBasic(basic);
        if (graph instanceof WeightedGraphSnapshot<?> weighted) return initialWeighted(weighted);
        throw new IllegalArgumentException("unsupported graph snapshot type: " + graph.getClass().getName());
    }

    public static <T> GraphViewState initial(GraphSnapshot<T> graph) { return initialBasic(graph); }
    public static <T> GraphViewState initial(WeightedGraphSnapshot<T> graph) { return initialWeighted(graph); }

    private static GraphViewState initialBasic(GraphSnapshot<?> graph) {
        List<Node> nodes = graph.vertices().stream().map(vertex -> new Node(vertex.id(), VisualValue.of(vertex.value()))).toList();
        List<Edge> edges = graph.edges().stream().map(edge -> new Edge(edge.id(), edge.fromId(), edge.toId(), null)).toList();
        return new GraphViewState(graph.directed(), nodes, edges, Set.of(), Observation.none(), false);
    }

    private static GraphViewState initialWeighted(WeightedGraphSnapshot<?> graph) {
        List<Node> nodes = graph.vertices().stream().map(vertex -> new Node(vertex.id(), VisualValue.of(vertex.value()))).toList();
        List<Edge> edges = graph.edges().stream().map(edge -> new Edge(edge.id(), edge.fromId(), edge.toId(), edge.weight())).toList();
        return new GraphViewState(graph.directed(), nodes, edges, Set.of(), Observation.none(), false);
    }

    public Map<Long, Node> nodesById() {
        Map<Long, Node> result = new LinkedHashMap<>();
        for (Node node : nodes) result.put(node.id(), node);
        return Map.copyOf(result);
    }

    public Set<Long> visitedWith(long nodeId) {
        LinkedHashSet<Long> next = new LinkedHashSet<>(visitedNodeIds); next.add(nodeId); return Set.copyOf(next);
    }

    public record Node(long id, VisualValue value) {
        public Node { value = Objects.requireNonNull(value, "value"); }
    }
    public record Edge(long id, long fromId, long toId, Double weight) {
        public Edge(long id, long fromId, long toId) { this(id, fromId, toId, null); }
        public Edge withWeight(Double weight) { return new Edge(id, fromId, toId, weight); }
    }
    public record Observation(Type type, Long firstNodeId, Long secondNodeId) {
        public Observation { Objects.requireNonNull(type, "type"); }
        public static Observation none() { return new Observation(Type.NONE, null, null); }
        public static Observation visited(long nodeId) { return new Observation(Type.VISITED, nodeId, null); }
        public static Observation examined(long fromId, long toId) { return new Observation(Type.EXAMINED, fromId, toId); }
    }
    public enum Type { NONE, VISITED, EXAMINED }
}
