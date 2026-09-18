package com.majortom.algorithms.telemetry.timeline;

/** Projection result locating the presentation cursor within original telemetry sample time. */
public record TelemetryProjection(
    long executionElapsedNanos,
    int sampleIndex,
    double sampleProgress,
    boolean beforeStart,
    boolean atEnd) {
  public TelemetryProjection {
    if (executionElapsedNanos < 0L) {
      throw new IllegalArgumentException("executionElapsedNanos must not be negative");
    }
    if (sampleIndex < -1) {
      throw new IllegalArgumentException("sampleIndex must be -1 or greater");
    }
    if (!Double.isFinite(sampleProgress) || sampleProgress < 0.0d || sampleProgress > 1.0d) {
      throw new IllegalArgumentException("sampleProgress must be in [0,1]");
    }
  }
}
