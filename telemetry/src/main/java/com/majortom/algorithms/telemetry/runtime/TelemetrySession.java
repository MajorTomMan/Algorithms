package com.majortom.algorithms.telemetry.runtime;

import com.majortom.algorithms.telemetry.api.TelemetryProfile;
import com.majortom.algorithms.telemetry.api.TelemetrySessionId;
import com.majortom.algorithms.telemetry.api.TelemetrySessionState;

/** Mutable execution boundary. UI and presentation code consume immutable snapshots only. */
public interface TelemetrySession extends AutoCloseable {
  TelemetrySessionId id();

  TelemetrySessionState state();

  boolean attachCurrentThread();

  /**
   * Establishes the exact execution boundary after optional profiler/analyzer setup.
   * Existing samples are discarded and probe baselines are reset before a new zero-point sample.
   */
  void rebase();

  /** Forces one factual sample now; useful at important execution/event boundaries. */
  void sampleNow();

  TelemetryProfile snapshot();

  void complete();

  void fail();

  void cancel();

  @Override
  default void close() {
    complete();
  }
}
