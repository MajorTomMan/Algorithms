package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;

/** JavaFX-neutral measured decoration input anchored to resolved structure geometry. */
public record DecorationInput(
    String id,
    String anchorId,
    Kind kind,
    double width,
    double height) {

  public DecorationInput {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(anchorId, "anchorId");
    Objects.requireNonNull(kind, "kind");
    if (!(width > 0.0d) || !(height > 0.0d)) {
      throw new IllegalArgumentException("decoration size must be positive: " + id);
    }
  }

  public enum Kind {
    NODE_BELOW,
    EDGE_LABEL
  }
}
