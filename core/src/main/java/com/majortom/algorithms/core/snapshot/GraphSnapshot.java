package com.majortom.algorithms.core.snapshot;

import com.majortom.algorithms.core.metadata.GraphDirection;
import java.util.List;
import java.util.Objects;

/** Immutable weighted graph snapshot with stable vertex and edge identities. */
public record GraphSnapshot<T>(GraphDirection direction, List<Vertex<T>> vertices, List<Edge> edges) {
  public GraphSnapshot {
    direction = Objects.requireNonNull(direction, "direction");
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
      if (id <= 0 || fromId <= 0 || toId <= 0) {
        throw new IllegalArgumentException("vertex and edge ids must be positive");
      }
      if (!Double.isFinite(weight)) {
        throw new IllegalArgumentException("edge weight must be finite");
      }
    }
  }
}
