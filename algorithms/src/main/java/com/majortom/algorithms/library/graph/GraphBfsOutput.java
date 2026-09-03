package com.majortom.algorithms.library.graph;

import java.util.List;
import java.util.Objects;

public record GraphBfsOutput(List<Integer> visitOrder, List<DiscoveryEdge> discoveryEdges) {
    public GraphBfsOutput {
        visitOrder = List.copyOf(Objects.requireNonNull(visitOrder, "visitOrder"));
        discoveryEdges = List.copyOf(Objects.requireNonNull(discoveryEdges, "discoveryEdges"));
    }

    public record DiscoveryEdge(int from, int to) {}
}
