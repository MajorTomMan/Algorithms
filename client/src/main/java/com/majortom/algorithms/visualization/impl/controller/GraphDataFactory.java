package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.structure.graph.WeightedGraph;
import com.majortom.algorithms.visualization.impl.controller.GraphBatchParser.GraphBatch;
import com.majortom.algorithms.visualization.impl.controller.GraphBatchParser.GraphBatchEdge;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Creates graph sample data and converts immutable batches to weighted adjacency. */
final class GraphDataFactory {
    private GraphDataFactory() {}

    static WeightedGraph<Object> randomWeightedGraph(Class<?> valueType, int nodeCount, int edgeCount, boolean directed) {
        WeightedGraph<Object> result = new WeightedGraph<>(directed);
        GraphBatch batch = randomGraphBatch(valueType, nodeCount, edgeCount, directed, new Random(0x5EEDL));
        result.initializeWeighted(weightedAdjacency(batch, directed));
        return result;
    }

    private static List<Object> defaultGraphValues(Class<?> valueType, int nodeCount) {
        List<Object> values = new ArrayList<>(nodeCount);
        for (int index = 0; index < nodeCount; index++) {
            values.add(ValueAdapters.distinctValue(valueType, index));
        }
        return List.copyOf(values);
    }

    static GraphBatch randomGraphBatch(Class<?> valueType, int nodeCount, int edgeCount,
            boolean directed, Random random) {
        List<Object> nodes = defaultGraphValues(valueType, nodeCount);
        Set<String> edges = new LinkedHashSet<>();
        for (int node = 1; node < nodeCount; node++) {
            edges.add((node - 1) + ":" + node);
        }
        while (edges.size() < edgeCount) {
            int from = random.nextInt(nodeCount);
            int to = random.nextInt(nodeCount);
            if (from == to) {
                continue;
            }
            String key;
            if (directed || from < to) {
                key = from + ":" + to;
            } else {
                key = to + ":" + from;
            }
            edges.add(key);
        }
        List<GraphBatchEdge> batchEdges = new ArrayList<>();
        for (String edge : edges) {
            String[] parts = edge.split(":", 2);
            batchEdges.add(new GraphBatchEdge(
                    nodes.get(Integer.parseInt(parts[0])),
                    nodes.get(Integer.parseInt(parts[1])),
                    1.0d + random.nextInt(20)));
        }
        return new GraphBatch(List.copyOf(nodes), List.copyOf(batchEdges));
    }

    static java.util.Map<Object, java.util.Map<Object, Double>> weightedAdjacency(
            GraphBatch batch, boolean directed) {
        java.util.LinkedHashMap<Object, java.util.Map<Object, Double>> adjacency = new java.util.LinkedHashMap<>();
        for (Object node : batch.nodes()) {
            adjacency.put(node, new java.util.LinkedHashMap<>());
        }
        for (GraphBatchEdge edge : batch.edges()) {
            adjacency.get(edge.from()).put(edge.to(), edge.weight());
            if (!directed) {
                adjacency.get(edge.to()).put(edge.from(), edge.weight());
            }
        }
        return adjacency;
    }

}
