package com.majortom.algorithms.server.request;

import com.majortom.algorithms.core.snapshot.GraphSnapshot;

import java.util.Objects;

public record GraphBfsRequest(GraphSnapshot<Integer> graph, int startNode) {
    public GraphBfsRequest {
        graph = Objects.requireNonNull(graph, "graph");
        boolean found = graph.vertices().stream().anyMatch(vertex -> vertex.value() == startNode);
        if (!found) {
            throw new IllegalArgumentException("startNode must exist in graph");
        }
    }
}
