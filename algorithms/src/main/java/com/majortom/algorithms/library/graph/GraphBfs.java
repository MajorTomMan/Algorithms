package com.majortom.algorithms.library.graph;

import com.majortom.algorithms.core.runtime.Observations;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.library.basic.graph.Edge;
import com.majortom.algorithms.library.basic.graph.Vertex;
import com.majortom.algorithms.library.structure.GraphStructure;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class GraphBfs implements GraphTraversal<Integer> {
    private static final String VERTEX_DOMAIN = "graph.vertex";

    @Override
    public List<Integer> traverse(GraphStructure<Integer> graph, Integer startNode) {
        Objects.requireNonNull(graph, "graph");
        Vertex<Integer> startVertex = vertex(graph, startNode);
        if (startVertex == null) {
            throw new IllegalArgumentException("startNode must exist in graph");
        }
        ArrayDeque<Vertex<Integer>> queue = new ArrayDeque<>();
        Set<Vertex<Integer>> discovered = new HashSet<>();
        List<Integer> order = new ArrayList<>();
        queue.add(startVertex);
        discovered.add(startVertex);
        while (!queue.isEmpty()) {
            Vertex<Integer> node = queue.removeFirst();
            Observations.visited(VERTEX_DOMAIN, node.id());
            order.add(node.value());
            for (Vertex<Integer> neighbor : graph.neighbors(node)) {
                Observations.examined(VERTEX_DOMAIN, node.id(), neighbor.id());
                if (discovered.add(neighbor)) {
                    queue.addLast(neighbor);
                }
            }
        }
        return List.copyOf(order);
    }

    public static GraphSnapshot<Integer> snapshot(GraphStructure<Integer> graph) {
        Objects.requireNonNull(graph, "graph");
        List<GraphSnapshot.Vertex<Integer>> vertices = new ArrayList<>();
        for (Vertex<Integer> vertex : graph.vertices()) {
            vertices.add(new GraphSnapshot.Vertex<>(vertex.id(), vertex.value()));
        }
        List<GraphSnapshot.Edge> edges = new ArrayList<>();
        for (Edge<Integer> edge : graph.edges()) {
            edges.add(new GraphSnapshot.Edge(edge.id(), edge.from().id(), edge.to().id()));
        }
        return new GraphSnapshot<>(graph.isDirected(), vertices, edges);
    }

    private static Vertex<Integer> vertex(GraphStructure<Integer> graph, int value) {
        for (Vertex<Integer> vertex : graph.vertices()) {
            if (vertex.value() == value) {
                return vertex;
            }
        }
        return null;
    }
}
