package com.majortom.algorithms.visualization.render.viewport;

import java.util.Objects;
public record ViewportSnapshot(double width, double height, ViewportInsets insets) {
  public ViewportSnapshot {
    Objects.requireNonNull(insets, "insets");
  }
  public double usableWidth() {
    return Math.max(1.0d, width - insets.left() - insets.right());
  }
  public double usableHeight() {
    return Math.max(1.0d, height - insets.top() - insets.bottom());
  }
  public double safeCenterX() {
    return insets.left() + usableWidth() / 2.0d;
  }
  public double safeCenterY() {
    return insets.top() + usableHeight() / 2.0d;
  }
}
