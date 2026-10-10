package com.majortom.algorithms.structure.graph;

import java.util.Objects;

/** An edge description independent of runtime vertex or edge identifiers. */
public record GraphLink<T>(T from, T to, double weight) {
  public GraphLink {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    if (!Double.isFinite(weight)) {
      throw new IllegalArgumentException("edge weight must be finite");
    }
  }
}
