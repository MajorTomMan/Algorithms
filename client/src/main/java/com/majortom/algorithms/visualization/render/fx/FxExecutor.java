package com.majortom.algorithms.visualization.render.fx;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public interface FxExecutor {
  CompletionStage<Void> execute(Runnable action);
  CompletionStage<Void> defer(Runnable action);
  <T> CompletionStage<T> supply(Supplier<T> supplier);
  default CompletionStage<Void> awaitPulse() {
    return defer(() -> {});
  }
  boolean isFxThread();
}
