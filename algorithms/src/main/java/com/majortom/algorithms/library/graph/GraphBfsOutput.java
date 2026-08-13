package com.majortom.algorithms.library.graph;

import com.majortom.algorithms.core.api.AlgorithmOutput;

import java.util.List;
import java.util.Objects;

/** BFS visit order and discovery-tree edges. */
public record GraphBfsOutput(List<Integer> visitOrder, List<IntEdge> discoveryEdges)
        implements AlgorithmOutput {

    public GraphBfsOutput {
        Objects.requireNonNull(visitOrder, "visitOrder");
        Objects.requireNonNull(discoveryEdges, "discoveryEdges");
        visitOrder = List.copyOf(visitOrder);
        discoveryEdges = List.copyOf(discoveryEdges);
    }
}
