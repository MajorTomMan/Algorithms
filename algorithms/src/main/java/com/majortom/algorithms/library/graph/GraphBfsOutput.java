package com.majortom.algorithms.library.graph;


import java.util.List;
import java.util.Objects;

/** BFS visit order and discovery-tree edges. */
public record GraphBfsOutput(List<Integer> visitOrder, List<IntEdge> discoveryEdges)
        {

    public GraphBfsOutput {
        Objects.requireNonNull(visitOrder, "visitOrder");
        Objects.requireNonNull(discoveryEdges, "discoveryEdges");
        visitOrder = List.copyOf(visitOrder);
        discoveryEdges = List.copyOf(discoveryEdges);
    }
}
