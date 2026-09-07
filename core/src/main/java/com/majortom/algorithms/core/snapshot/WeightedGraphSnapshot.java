package com.majortom.algorithms.core.snapshot;

import java.util.List;
import java.util.Objects;

/** UI-neutral immutable weighted-graph snapshot with stable vertex/edge identity. */
public record WeightedGraphSnapshot<T>(
        boolean directed,
        List<Vertex<T>> vertices,
        List<Edge> edges) implements GraphSnapshotState<T> {

    public WeightedGraphSnapshot {
        vertices = List.copyOf(Objects.requireNonNull(vertices, "vertices"));
        edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
    }

    public record Vertex<T>(long id, T value) {
        public Vertex {
            if (id <= 0) {
                throw new IllegalArgumentException("vertex id must be positive");
            }
            value = Objects.requireNonNull(value, "value");
        }
    }

    public record Edge(long id, long fromId, long toId, double weight) {
        public Edge {
            if (id <= 0) {
                throw new IllegalArgumentException("edge id must be positive");
            }
            if (fromId <= 0 || toId <= 0) {
                throw new IllegalArgumentException("edge vertex ids must be positive");
            }
            if (!Double.isFinite(weight)) {
                throw new IllegalArgumentException("edge weight must be finite");
            }
        }
    }
}
