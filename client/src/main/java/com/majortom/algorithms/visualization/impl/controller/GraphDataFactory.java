package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.graph.GraphData;
import com.majortom.algorithms.structure.graph.GraphInitializer;
import com.majortom.algorithms.structure.graph.GraphLink;
import com.majortom.algorithms.visualization.impl.controller.GraphBatchParser.GraphBatch;
import com.majortom.algorithms.visualization.impl.controller.GraphBatchParser.GraphBatchEdge;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Creates example graph data; structure initialization is delegated to GraphInitializer. */
final class GraphDataFactory {
    private GraphDataFactory() {}

    static Graph<Object> randomGraph(Class<?> valueType, int nodeCount, int edgeCount,
                                     GraphDirection direction) {
        GraphBatch batch = randomGraphBatch(valueType, nodeCount, edgeCount,
                direction, new Random(0x5EEDL));
        return new GraphInitializer<Object>().create(graphData(batch, direction));
    }

    private static List<Object> defaultGraphValues(Class<?> valueType, int nodeCount) {
        List<Object> values = new ArrayList<>(nodeCount);
        for (int index = 0; index < nodeCount; index++) {
            values.add(ValueAdapters.distinctValue(valueType, index));
        }
        return List.copyOf(values);
    }

    static GraphBatch randomGraphBatch(Class<?> valueType, int nodeCount, int edgeCount,
                                       GraphDirection direction, Random random) {
        List<Object> nodes = defaultGraphValues(valueType, nodeCount);
        Set<String> edgeKeys = new LinkedHashSet<>();
        for (int node = 1; node < nodeCount; node++) {
            edgeKeys.add((node - 1) + ":" + node);
        }
        while (edgeKeys.size() < edgeCount) {
            int from = random.nextInt(nodeCount);
            int to = random.nextInt(nodeCount);
            if (from == to) {
                continue;
            }
            String key;
            if (direction.isDirected() || from < to) {
                key = from + ":" + to;
            } else {
                key = to + ":" + from;
            }
            edgeKeys.add(key);
        }
        List<GraphBatchEdge> edges = new ArrayList<>();
        for (String key : edgeKeys) {
            String[] parts = key.split(":", 2);
            edges.add(new GraphBatchEdge(
                    nodes.get(Integer.parseInt(parts[0])),
                    nodes.get(Integer.parseInt(parts[1])),
                    1.0d + random.nextInt(20)));
        }
        return new GraphBatch(List.copyOf(nodes), List.copyOf(edges));
    }

    static GraphData<Object> graphData(GraphBatch batch, GraphDirection direction) {
        List<GraphLink<Object>> links = new ArrayList<>();
        for (GraphBatchEdge edge : batch.edges()) {
            links.add(new GraphLink<>(edge.from(), edge.to(), edge.weight()));
        }
        return new GraphData<>(direction, batch.nodes(), links);
    }
}
