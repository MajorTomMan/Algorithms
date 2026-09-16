package com.majortom.algorithms.visualization.render.fx;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Application-wide JavaFX dispatch facade.
 *
 * <p>Code outside this package never calls {@code FxDispatch.defer()} directly. RenderFramework
 * and ordinary UI shell code share the same JavaFX entry point without sharing render-session
 * authority.</p>
 */
public final class FxDispatch {
  private static final FxExecutor EXECUTOR = new FxExecutorImpl();

  private FxDispatch() {}

  public static FxExecutor executor() {
    return EXECUTOR;
  }

  /** Executes immediately on the FX thread, otherwise schedules onto it. */
  public static void execute(Runnable action) {
    observe(EXECUTOR.execute(action));
  }

  /** Always schedules for a later JavaFX pulse/turn. */
  public static void defer(Runnable action) {
    observe(EXECUTOR.defer(action));
  }

  public static boolean isFxThread() {
    return EXECUTOR.isFxThread();
  }

  private static void observe(CompletionStage<Void> stage) {
    stage.whenComplete((ignored, failure) -> {
      if (failure == null)
        return;
      Throwable cause = failure instanceof CompletionException && failure.getCause() != null
          ? failure.getCause()
          : failure;
      Thread thread = Thread.currentThread();
      Thread.UncaughtExceptionHandler handler = thread.getUncaughtExceptionHandler();
      if (handler != null)
        handler.uncaughtException(thread, cause);
    });
  }
}
