package com.majortom.algorithms.algorithm.graph.model;

import java.util.Objects;

/**
 * Road data mapped to an Edge<City> payload.
 * A bidirectional road generates two directed graph edges.
 */
public record Road(String id, City from, City to, double distance, boolean bidirectional) {
  public Road {
    if (id == null || id.isBlank())
      throw new IllegalArgumentException("road id cannot be blank");
    id = id.trim();
    Objects.requireNonNull(from, "road from");
    Objects.requireNonNull(to, "road to");
    if (!Double.isFinite(distance) || distance < 0)
      throw new IllegalArgumentException("road distance must be finite and non-negative");
    if (bidirectional && from.id().equals(to.id()))
      throw new IllegalArgumentException("bidirectional self-loop is ambiguous");
  }
}
