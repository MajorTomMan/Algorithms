package com.majortom.algorithms.core.snapshot;
import com.majortom.algorithms.core.metadata.GraphDirection;
import java.util.List;
import java.util.Objects;
/** Stable topology snapshot; callers must supply immutable or frozen value payloads for replay. */
public record GraphSnapshot<T>(GraphDirection direction, List<Vertex<T>> vertices,
                                    List<Edge> edges) {
  public GraphSnapshot {
    direction = Objects.requireNonNull(direction, "direction");
    vertices = List.copyOf(Objects.requireNonNull(vertices, "vertices"));
    edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
  }
  public record Vertex<T>(long id, T value) {
    public Vertex {
      if (id <= 0) throw new IllegalArgumentException("vertex ID must be positive");
      Objects.requireNonNull(value, "value");
    }
  }
  public record Edge(long id, long fromId, long toId, Object value, double weight) {
    public Edge(long id, long fromId, long toId, double weight) {
      this(id, fromId, toId, null, weight);
    }
    public Edge {
      if (id <= 0 || fromId <= 0 || toId <= 0 || !Double.isFinite(weight))
        throw new IllegalArgumentException("invalid multi-graph edge");
    }
  }
}
