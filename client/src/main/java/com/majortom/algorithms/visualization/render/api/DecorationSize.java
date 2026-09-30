package com.majortom.algorithms.visualization.render.api;

/** Measured presentation size supplied to a decoration layout. */
public record DecorationSize(double width, double height) {
  public DecorationSize {
    if (width < 0.0d || height < 0.0d) {
      throw new IllegalArgumentException("decoration size must be non-negative");
    }
  }
}
