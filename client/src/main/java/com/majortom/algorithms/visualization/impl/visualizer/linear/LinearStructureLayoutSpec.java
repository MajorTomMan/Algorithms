package com.majortom.algorithms.visualization.impl.visualizer.linear;

import com.majortom.algorithms.visualization.render.api.LinearLayoutDirection;
import java.util.Objects;

/** Immutable geometry policy shared by linear capture and FX renderers. */
public record LinearStructureLayoutSpec(
    String structure,
    LinearLayoutDirection direction,
    double minWidth,
    double height,
    double horizontalPadding,
    double layoutPadding) {

  public LinearStructureLayoutSpec {
    Objects.requireNonNull(structure, "structure");
    Objects.requireNonNull(direction, "direction");
    if (!(minWidth > 0.0d) || !(height > 0.0d)) {
      throw new IllegalArgumentException("linear item size must be positive");
    }
    if (horizontalPadding < 0.0d || layoutPadding < 0.0d) {
      throw new IllegalArgumentException("linear padding must be non-negative");
    }
  }
}
