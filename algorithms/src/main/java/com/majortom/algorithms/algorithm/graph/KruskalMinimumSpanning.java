package com.majortom.algorithms.algorithm.graph;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.runtime.Observations;
import com.majortom.algorithms.structure.graph.Edge;
import com.majortom.algorithms.structure.graph.Vertex;
import com.majortom.algorithms.structure.graph.WeightedGraphStructure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Kruskal minimum-spanning-tree/forest implementation over the weighted graph contract. */
@Algorithm(id = "kruskal-minimum-spanning", type = Integer.class, structure = WeightedGraphStructure.class)
public final class KruskalMinimumSpanning implements MinimumSpanningAlgorithm<Integer> {

    @Override
    public void build(WeightedGraphStructure<Integer> source, WeightedGraphStructure<Integer> result) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(result, "result");
        if (source.isDirected()) {
            throw new IllegalArgumentException("Kruskal requires an undirected weighted graph");
        }
        if (result.isDirected()) {
            throw new IllegalArgumentException("Kruskal result graph must be undirected");
        }

        Map<Long, Long> parent = new HashMap<>();
        Map<Long, Integer> rank = new HashMap<>();
        for (Vertex<Integer> vertex : source.vertices()) {
            parent.put(vertex.id(), vertex.id());
            rank.put(vertex.id(), 0);
            if (result.vertex(vertex.value()) == null) {
                result.addVertex(vertex.value());
            }
        }

        List<Edge<Integer>> edges = new ArrayList<>();
        for (Edge<Integer> edge : source.edges()) {
            edges.add(edge);
        }
        edges.sort(Comparator
                .comparingDouble(source::weight)
                .thenComparingLong(Edge::id));

        for (Edge<Integer> edge : edges) {
            Observations.examined("graph.vertex", edge.from().id(), edge.to().id());
            long fromRoot = find(parent, edge.from().id());
            long toRoot = find(parent, edge.to().id());
            if (fromRoot == toRoot) {
                continue;
            }
            union(parent, rank, fromRoot, toRoot);
            Vertex<Integer> resultFrom = result.vertex(edge.from().value());
            Vertex<Integer> resultTo = result.vertex(edge.to().value());
            result.addEdge(resultFrom, resultTo, source.weight(edge));
        }
    }

    private static long find(Map<Long, Long> parent, long value) {
        long current = value;
        while (parent.get(current) != current) {
            current = parent.get(current);
        }
        long root = current;
        current = value;
        while (parent.get(current) != current) {
            long next = parent.get(current);
            parent.put(current, root);
            current = next;
        }
        return root;
    }

    private static void union(Map<Long, Long> parent, Map<Long, Integer> rank, long left, long right) {
        int leftRank = rank.get(left);
        int rightRank = rank.get(right);
        if (leftRank < rightRank) {
            parent.put(left, right);
            return;
        }
        if (leftRank > rightRank) {
            parent.put(right, left);
            return;
        }
        parent.put(right, left);
        rank.put(left, leftRank + 1);
    }
}
