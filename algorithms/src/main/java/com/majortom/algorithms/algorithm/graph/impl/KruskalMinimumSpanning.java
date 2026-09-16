package com.majortom.algorithms.algorithm.graph.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.runtime.Observations;
import com.majortom.algorithms.core.snapshot.WeightedGraphSnapshot;
import com.majortom.algorithms.structure.graph.Edge;
import com.majortom.algorithms.structure.graph.Vertex;
import com.majortom.algorithms.structure.graph.WeightedGraph;
import com.majortom.algorithms.structure.graph.WeightedGraphStructure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Kruskal minimum-spanning-tree/forest implementation over the weighted graph contract. */
@Algorithm(
        id = "kruskal-minimum-spanning",
        name = "Kruskal最小生成树",
        type = Integer.class,
        structure = WeightedGraphStructure.class)
public final class KruskalMinimumSpanning {
    @AlgorithmEntry
    public WeightedGraphSnapshot<Integer> build(WeightedGraphStructure<Integer> graph) {
        Objects.requireNonNull(graph, "graph");
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Kruskal requires an undirected weighted graph");
        }

        Map<Long, Long> parent = new HashMap<>();
        Map<Long, Integer> rank = new HashMap<>();
        for (Vertex<Integer> vertex : graph.vertices()) {
            parent.put(vertex.id(), vertex.id());
            rank.put(vertex.id(), 0);
        }

        List<Edge<Integer>> edges = new ArrayList<>();
        for (Edge<Integer> edge : graph.edges()) {
            edges.add(edge);
        }
        edges.sort(Comparator.comparingDouble(graph::weight).thenComparingLong(Edge::id));

        WeightedGraph<Integer> result = new WeightedGraph<>(false);
        for (Vertex<Integer> vertex : graph.vertices()) {
            result.addVertex(vertex.value());
        }

        for (Edge<Integer> edge : edges) {
            Observations.examined("graph.vertex", edge.from().id(), edge.to().id());
            long fromRoot = find(parent, edge.from().id());
            long toRoot = find(parent, edge.to().id());
            if (fromRoot == toRoot) {
                continue;
            }
            union(parent, rank, fromRoot, toRoot);
            Vertex<Integer> from = result.vertex(edge.from().value());
            Vertex<Integer> to = result.vertex(edge.to().value());
            result.addEdge(from, to, graph.weight(edge));
        }
        return result.snapshot();
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

    private static void union(
            Map<Long, Long> parent, Map<Long, Integer> rank, long left, long right) {
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
