package com.majortom.algorithms.visualization.render.fx;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * 全客户端统一的 JavaFX 线程入口。
 *
 * <p>{@link #execute(Runnable)} 若已处于 FX 线程可立即执行；
 * {@link #defer(Runnable)} 始终推迟至下一个 FX 任务轮次。
 * RenderFramework 和控制器共享一个 FxExecutor，
 * 避免后台算法线程直接修改 JavaFX 场景图。
 *
 * <p>这是线程切换门面，不授予调用者直接改写渲染会话、布局或摄像机的权力。
 * 任务失败通过 Future/异常处理通道交付，不能悄悄吞掉异常。
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
