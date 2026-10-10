package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.structure.graph.Edge;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.graph.Vertex;
import com.majortom.algorithms.visualization.impl.controller.GraphBatchParser.GraphBatch;
import com.majortom.algorithms.visualization.impl.controller.GraphBatchParser.GraphBatchEdge;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/** Builds sample graphs and translates manually entered data into the canonical snapshot. */
final class GraphDataFactory {
    private GraphDataFactory() {}

    static Graph<Object> randomGraph(Class<?> valueType, int nodeCount, int edgeCount,
                                     GraphDirection direction) {
        GraphBatch batch = randomGraphBatch(valueType, nodeCount, edgeCount,
                direction, new Random(0x5EEDL));
        return Graph.fromSnapshot(snapshot(batch, direction));
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

    static GraphSnapshot<Object> snapshot(GraphBatch batch, GraphDirection direction) {
        Objects.requireNonNull(batch, "batch");
        Objects.requireNonNull(direction, "direction");

        Map<Object, Vertex<Object>> verticesByValue = new LinkedHashMap<>();
        List<GraphSnapshot.Vertex<Object>> vertices = new ArrayList<>();
        for (Object value : batch.nodes()) {
            if (verticesByValue.containsKey(value)) {
                throw new IllegalArgumentException("duplicate graph vertex: " + value);
            }
            Vertex<Object> vertex = new Vertex<>(value);
            verticesByValue.put(value, vertex);
            vertices.add(new GraphSnapshot.Vertex<>(vertex.id(), value));
        }

        List<GraphSnapshot.Edge> edges = new ArrayList<>();
        for (GraphBatchEdge source : batch.edges()) {
            Vertex<Object> from = verticesByValue.get(source.from());
            Vertex<Object> to = verticesByValue.get(source.to());
            if (from == null || to == null) {
                throw new IllegalArgumentException("edge references an undeclared vertex");
            }
            Edge<Object> edge = new Edge<>(from, to, source.weight());
            edges.add(new GraphSnapshot.Edge(
                    edge.id(), from.id(), to.id(), edge.weight()));
        }
        return new GraphSnapshot<>(direction, vertices, edges);
    }
}
