package com.majortom.algorithms.telemetry.api;

/** Semantics used by projection and presentation when interpreting sampled values. */
public enum TelemetryMetricKind {
  /** Monotonic total such as cumulative allocated bytes. */
  CUMULATIVE,
  /** Discrete monotonic count such as collection count. */
  COUNTER,
  /** Point-in-time value such as CPU load or live heap usage. */
  GAUGE
}
