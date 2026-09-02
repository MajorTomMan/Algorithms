package com.majortom.algorithms.server.request;

import com.majortom.algorithms.library.graph.IntGraph;

import java.util.Objects;

public record GraphBfsRequest(IntGraph graph, int startNode) {
    public GraphBfsRequest {
        graph = Objects.requireNonNull(graph, "graph");
        if (!graph.nodes().contains(startNode)) throw new IllegalArgumentException("startNode must exist in graph");
    }
}
