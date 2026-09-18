package com.majortom.algorithms.telemetry.probe;

import com.majortom.algorithms.telemetry.api.TelemetryValue;
import java.util.Map;

/** Mutable probe lifetime owned by one TelemetrySession. */
public interface TelemetryProbeSession extends AutoCloseable {
  /** Allows thread-attributed probes to attach an additional platform thread to this scope. */
  default boolean attachCurrentThread() {
    return false;
  }

  /**
   * Resets probe-owned baselines immediately before the measured execution body starts.
   * Framework/bootstrap work performed before this boundary must not contaminate attributed facts.
   */
  default void rebase() {}

  /** Returns current point-in-time/cumulative sampled facts. */
  Map<String, TelemetryValue> sample();

  /** Returns final or current summary values independent of the sample timeline. */
  default Map<String, TelemetryValue> summary() {
    return sample();
  }

  @Override
  default void close() {}
}
