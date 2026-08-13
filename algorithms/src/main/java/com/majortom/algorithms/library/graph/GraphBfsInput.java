package com.majortom.algorithms.library.graph;

import com.majortom.algorithms.core.api.AlgorithmInput;

import java.util.Objects;

/** Graph and start node for breadth-first traversal. */
public record GraphBfsInput(IntGraph graph, int startNode) implements AlgorithmInput {

    public GraphBfsInput {
        Objects.requireNonNull(graph, "graph");
        if (!graph.nodes().contains(startNode)) {
            throw new IllegalArgumentException("startNode must exist in graph");
        }
    }
}
