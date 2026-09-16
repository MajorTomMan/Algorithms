package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;
public record ElementGeometry(String id, double x, double y, double width, double height) {
  public ElementGeometry {
    Objects.requireNonNull(id, "id");
  }
}
