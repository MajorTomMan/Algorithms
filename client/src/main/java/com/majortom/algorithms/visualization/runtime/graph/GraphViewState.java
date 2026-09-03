package com.majortom.algorithms.visualization.runtime.graph;

import com.majortom.algorithms.core.snapshot.GraphSnapshot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable JavaFX-neutral Graph facts: directedness, vertices and edges. */
public record GraphViewState(boolean directed, List<Node> nodes, List<Edge> edges, boolean completed) {

    public GraphViewState {
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
    }

    public static GraphViewState initial(GraphSnapshot<Integer> graph) {
        Objects.requireNonNull(graph, "graph");
        List<Node> nodes = graph.vertices().stream()
                .map(vertex -> new Node(vertex.id(), vertex.value()))
                .toList();
        List<Edge> edges = graph.edges().stream()
                .map(edge -> new Edge(edge.id(), edge.fromId(), edge.toId()))
                .toList();
        return new GraphViewState(graph.directed(), nodes, edges, false);
    }

    public Map<Long, Node> nodesById() {
        Map<Long, Node> result = new LinkedHashMap<>();
        for (Node node : nodes) {
            result.put(node.id(), node);
        }
        return Map.copyOf(result);
    }

    public record Node(long id, int value) {}

    public record Edge(long id, long fromId, long toId) {}
}
