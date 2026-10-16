package com.majortom.algorithms.core.runtime;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runtime 侧可替换的后台调度器，与客户端渲染调度器互不依赖。
 *
 * <p>提供 daemon 单线程和显式有界队列线程池。ExecutionRuntime 负责同步
 * 的执行生命周期，不负责线程创建；UI 客户端按运行会话持有此调度器。
 * 线程池满载时拒绝任务，以免内存因无界排队耗尽。
 *
 * <p>此类绝不直接调用 JavaFX；UI 交付使用客户端 FxDispatch。
 * {@link #shutdownNow()} 是中断请求，不能替代 RunControl 的协作取消。
 */
public final class ExecutionScheduler implements AutoCloseable {
  private final ExecutorService executor;

  private ExecutionScheduler(ExecutorService executor) {
    this.executor = Objects.requireNonNull(executor, "executor");
  }

  public static ExecutionScheduler single(String threadPrefix) {
    AtomicLong sequence = new AtomicLong();
    return new ExecutionScheduler(Executors.newSingleThreadExecutor(
        runnable -> daemonThread(runnable, threadPrefix + sequence.incrementAndGet())));
  }

  public static ExecutionScheduler bounded(
      String threadPrefix, int corePoolSize, int maximumPoolSize, int queueCapacity) {
    if (corePoolSize <= 0 || maximumPoolSize < corePoolSize || queueCapacity <= 0) {
      throw new IllegalArgumentException("Invalid scheduler bounds");
    }
    AtomicLong sequence = new AtomicLong();
    ExecutorService executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 60L,
        TimeUnit.SECONDS, new ArrayBlockingQueue<>(queueCapacity),
        runnable
        -> daemonThread(runnable, threadPrefix + sequence.incrementAndGet()),
        new ThreadPoolExecutor.AbortPolicy());
    return new ExecutionScheduler(executor);
  }

  public Future<?> submit(Runnable task) {
    return executor.submit(Objects.requireNonNull(task, "task"));
  }

  public void execute(Runnable task) {
    executor.execute(Objects.requireNonNull(task, "task"));
  }

  public void shutdownNow() {
    executor.shutdownNow();
  }

  @Override
  public void close() {
    executor.shutdown();
  }

  private static Thread daemonThread(Runnable runnable, String name) {
    Thread thread = new Thread(runnable, name);
    thread.setDaemon(true);
    return thread;
  }
}
