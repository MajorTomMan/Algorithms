package com.majortom.algorithms.telemetry.api;

/** Lifecycle of one telemetry session and its optional asynchronous enrichment. */
public enum TelemetrySessionState {
  CREATED,
  RECORDING,
  COMPLETED,
  FAILED,
  CANCELLED,
  ENRICHING,
  READY;

  /** The observed execution has ended, even if asynchronous enrichment is still in progress. */
  public boolean executionEnded() {
    return this == COMPLETED || this == FAILED || this == CANCELLED
        || this == ENRICHING || this == READY;
  }

  /** Immutable profiles in these states are safe to retain in history. */
  public boolean storable() {
    return this == COMPLETED || this == FAILED || this == CANCELLED || this == READY;
  }
}
