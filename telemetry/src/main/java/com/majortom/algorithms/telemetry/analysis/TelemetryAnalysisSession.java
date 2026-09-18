package com.majortom.algorithms.telemetry.analysis;

import java.util.concurrent.CompletionStage;

/** Analyzer lifetime separated from synchronous probes so enrichment can finish asynchronously. */
public interface TelemetryAnalysisSession extends AutoCloseable {
  default void markExecutionStart() {}

  default void markExecutionEnd() {}

  CompletionStage<? extends TelemetryAnalysisResult> completion();

  @Override
  void close();
}
