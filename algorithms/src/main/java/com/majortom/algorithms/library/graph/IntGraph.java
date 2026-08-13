package com.majortom.algorithms.library.graph;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable directed integer graph with stable insertion order. */
public record IntGraph(List<Integer> nodes, List<IntEdge> edges) {

    public static final int MAX_NODES = 100_000;
    public static final int MAX_EDGES = 500_000;

    public IntGraph {
        Objects.requireNonNull(nodes, "nodes");
        Objects.requireNonNull(edges, "edges");
        if (nodes.size() > MAX_NODES) {
            throw new IllegalArgumentException("graph must contain at most " + MAX_NODES + " nodes");
        }
        if (edges.size() > MAX_EDGES) {
            throw new IllegalArgumentException("graph must contain at most " + MAX_EDGES + " edges");
        }
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        Set<Integer> uniqueNodes = new LinkedHashSet<>(nodes);
        if (uniqueNodes.size() != nodes.size()) {
            throw new IllegalArgumentException("node IDs must be unique");
        }
        for (IntEdge edge : edges) {
            Objects.requireNonNull(edge, "edge");
            if (!uniqueNodes.contains(edge.from()) || !uniqueNodes.contains(edge.to())) {
                throw new IllegalArgumentException("every edge endpoint must be a graph node");
            }
        }
    }
}
