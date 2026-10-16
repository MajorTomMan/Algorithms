package com.majortom.algorithms.visualization.render.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 渲染控制平面的单线程调度器，用于串行维护会话与邮箱状态。
 *
 * <p>这条线程负责调度 RenderIntent，不负责计算 ELK 布局，
 * 更不能直接写 JavaFX Node。布局经 LayoutCoordinator 的后台执行器，
 * 最终场景提交经 FxExecutor。
 *
 * <p>独立于 ExecutionScheduler：算法工作线程阻塞/单步不应阻塞渲染调度。
 */
public final class RenderScheduler implements AutoCloseable {
  private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
    Thread thread = new Thread(task, "render-scheduler");
    thread.setDaemon(true);
    return thread;
  });
  public void execute(Runnable task) {
    executor.execute(task);
  }
  @Override
  public void close() {
    executor.shutdownNow();
  }
}
