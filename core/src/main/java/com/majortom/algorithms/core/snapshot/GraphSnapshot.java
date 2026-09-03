package com.majortom.algorithms.core.snapshot;

import java.util.List;
import java.util.Objects;

/** UI-neutral immutable graph snapshot with stable vertex/edge identity. */
public record GraphSnapshot<T>(boolean directed, List<Vertex<T>> vertices, List<Edge> edges) {
    public GraphSnapshot {
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

    public record Edge(long id, long fromId, long toId) {
        public Edge {
            if (id <= 0) {
                throw new IllegalArgumentException("edge id must be positive");
            }
            if (fromId <= 0 || toId <= 0) {
                throw new IllegalArgumentException("edge vertex ids must be positive");
            }
        }
    }
}
