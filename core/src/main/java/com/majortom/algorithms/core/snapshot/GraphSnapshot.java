package com.majortom.algorithms.core.snapshot;

import java.util.List;
import java.util.Objects;

public record GraphSnapshot<T>(List<T> vertices, List<Edge<T>> edges) {

    public GraphSnapshot {
        vertices = List.copyOf(Objects.requireNonNull(vertices));
        edges = List.copyOf(Objects.requireNonNull(edges));
    }

    public record Edge<T>(T from, T to) {

        public Edge {
            Objects.requireNonNull(from);
            Objects.requireNonNull(to);
        }
    }
}
