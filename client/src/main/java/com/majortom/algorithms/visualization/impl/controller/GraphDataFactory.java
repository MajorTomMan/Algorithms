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

    /**
     * Generate the canonical graph model, including sample parallel edges and self-loops.
     * The simple-graph variant is useful for compatibility fixtures, not for storage.
     */
    static GraphBatch randomGraphBatch(Class<?> valueType, int nodeCount, int edgeCount,
                                       GraphDirection direction, Random random) {
        return randomGraphBatch(valueType, nodeCount, edgeCount, direction, random, true, true);
    }

    static GraphBatch randomGraphBatch(Class<?> valueType, int nodeCount, int edgeCount,
                                       GraphDirection direction, Random random,
                                       boolean allowParallel, boolean allowLoops) {
        Objects.requireNonNull(valueType, "valueType");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(random, "random");
        if (nodeCount < 0 || edgeCount < 0)
            throw new IllegalArgumentException("Graph sizes cannot be negative");
        if (nodeCount == 0) {
            if (edgeCount != 0)
                throw new IllegalArgumentException("An empty graph cannot have edges");
            return new GraphBatch(List.of(), List.of());
        }

        long maxUnique = direction.isDirected()
                ? (long) nodeCount * (allowLoops ? nodeCount : nodeCount - 1)
                : (long) nodeCount * (nodeCount - 1) / 2
                    + (allowLoops ? nodeCount : 0);
        if ((!allowParallel && edgeCount > maxUnique)
                || (allowParallel && maxUnique == 0 && edgeCount > 0))
            throw new IllegalArgumentException("Requested edge count is impossible");

        List<Object> nodes = defaultGraphValues(valueType, nodeCount);
        List<GraphBatchEdge> edges = new ArrayList<>(edgeCount);
        Set<Long> uniquePairs = allowParallel ? null : new LinkedHashSet<>();
        // Prefer a visible chain when enough edges were requested.
        for (int i = 1; i < nodeCount && edges.size() < edgeCount; i++)
            addRandomEdge(edges, uniquePairs, nodes, i - 1, i, direction, random);
        // Deliberately showcase a parallel edge and a self-loop in the default mode.
        if (allowParallel && !edges.isEmpty() && edges.size() < edgeCount)
            edges.add(edges.getFirst());
        if (allowLoops && edges.size() < edgeCount)
            addRandomEdge(edges, uniquePairs, nodes, 0, 0, direction, random);

        long misses = 0;
        long retryLimit = Math.max(64L, maxUnique * 2L);
        while (edges.size() < edgeCount) {
            int from = random.nextInt(nodeCount);
            int to = random.nextInt(nodeCount);
            if (!allowLoops && from == to) continue;
            if (addRandomEdge(edges, uniquePairs, nodes, from, to, direction, random)) {
                misses = 0;
            } else if (++misses >= retryLimit) {
                // When sampling near the unique-edge capacity, enumerate the remainder.
                // A bounded deterministic fallback guarantees termination.
                for (int left = 0; left < nodeCount && edges.size() < edgeCount; left++)
                    for (int right = 0; right < nodeCount && edges.size() < edgeCount; right++) {
                        if (left == right && !allowLoops) continue;
                        addRandomEdge(edges, uniquePairs, nodes, left, right, direction, random);
                    }
                break;
            }
        }
        return new GraphBatch(List.copyOf(nodes), List.copyOf(edges));
    }

    private static boolean addRandomEdge(List<GraphBatchEdge> edges, Set<Long> uniquePairs,
                                      List<Object> nodes, int from, int to,
                                      GraphDirection direction, Random random) {
        if (uniquePairs != null) {
            int left = from, right = to;
            if (!direction.isDirected() && left > right) {
                left = to;
                right = from;
            }
            long key = ((long) left << 32) | (right & 0xffffffffL);
            if (!uniquePairs.add(key)) return false;
        }
        edges.add(new GraphBatchEdge(nodes.get(from), nodes.get(to), 1.0d + random.nextInt(20)));
        return true;
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
