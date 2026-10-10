package com.majortom.algorithms.structure.graph;

import com.majortom.algorithms.core.metadata.GraphDirection;
import java.util.List;
import java.util.Objects;

/**
 * Transport-neutral graph initialization data. Unlike GraphSnapshot, it does not
 * contain runtime vertex/edge IDs and can be serialized as an ordinary data model.
 */
public record GraphData<T>(
    GraphDirection direction, List<T> vertices, List<GraphLink<T>> edges) {
  public GraphData {
    direction = Objects.requireNonNull(direction, "direction");
    vertices = List.copyOf(Objects.requireNonNull(vertices, "vertices"));
    edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
  }
}
