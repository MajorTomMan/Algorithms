package com.majortom.algorithms.telemetry.analysis;

import java.util.concurrent.CompletionStage;

/** On-demand state analysis, intentionally separate from execution telemetry (for example JOL). */
@FunctionalInterface
public interface StateAnalyzer<S, R> {
  CompletionStage<R> analyze(S state);
}
