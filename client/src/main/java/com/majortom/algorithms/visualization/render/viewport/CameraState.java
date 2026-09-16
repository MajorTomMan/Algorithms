package com.majortom.algorithms.visualization.render.viewport;
public record CameraState(double scale, double translateX, double translateY) {
  public CameraState {
    if (!(scale > 0.0d))
      throw new IllegalArgumentException("camera scale must be positive");
  }
}
