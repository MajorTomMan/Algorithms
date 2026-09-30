package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;

/** Absolute geometry for a presentation decoration in factual world coordinates. */
public record DecorationGeometry(
    String id,
    double x,
    double y,
    double width,
    double height) {

  public DecorationGeometry {
    Objects.requireNonNull(id, "id");
    if (width < 0.0d || height < 0.0d) {
      throw new IllegalArgumentException("decoration size must be non-negative: " + id);
    }
  }
}
