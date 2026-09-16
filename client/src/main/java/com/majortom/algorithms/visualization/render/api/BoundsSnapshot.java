package com.majortom.algorithms.visualization.render.api;

public record BoundsSnapshot(double minX, double minY, double width, double height) {
  public static BoundsSnapshot empty() {
    return new BoundsSnapshot(0.0d, 0.0d, 0.0d, 0.0d);
  }
  public boolean isEmpty() {
    return !(width > 0.0d) && !(height > 0.0d);
  }
  public double maxX() {
    return minX + width;
  }
  public double maxY() {
    return minY + height;
  }
  public double centerX() {
    return minX + width / 2.0d;
  }
  public double centerY() {
    return minY + height / 2.0d;
  }
}
