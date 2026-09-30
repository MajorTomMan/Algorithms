package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;

/** Geometry policy for a decoration that follows an edge while the edge itself animates. */
public record EdgeDecorationGeometry(
    String id,
    String edgeId,
    double normalOffset) {

  public EdgeDecorationGeometry {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(edgeId, "edgeId");
  }
}
