package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.runtime.DefaultExecutionControl;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionAnchorRecorder;
import com.majortom.algorithms.core.runtime.ExecutionAnchorTimeline;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionScheduler;
import com.majortom.algorithms.core.runtime.ResourceSampler;
import com.majortom.algorithms.core.runtime.ResourceUsage;
import com.majortom.algorithms.visualization.execution.ExecutionHandle;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 单次本地算法执行的资源与控制所有者，实现跨线程的 ExecutionHandle。
 *
 * <p>会话同时拥有真实 worker Future、暂停/步进 RunControl、有界事件记录、
 * UI 观察队列、执行时间锚点以及两个完成阶段：runtimeCompletion 表示
 * 算法逻辑结束，presentationCompletion 表示观察/呈现也已完成。
 * 两个 Future 不可混为一谈，否则快速算法可能在最终帧出现前被视为完成。
 *
 * <p>关闭时需要取消后台执行并停止观察队列；
 * 操作性暂停/单步改变执行许可，不意味着在 JavaFX 上播放帧。
 */
public final class ExecutionSession implements ExecutionHandle {
  private final long generation;
  private final DefaultExecutionControl executionControl;
  private final BoundedExecutionEventStore authoritativeEvents;
  private final JavaFxEventSink observerSink;
  private final ExecutionAnchorRecorder executionAnchorRecorder;
  private final ExecutionScheduler scheduler;
  private final ResourceSampler resourceSampler;
  private final CompletableFuture<ExecutionResult> runtimeCompletion = new CompletableFuture<>();
  private final CompletableFuture<ExecutionResult> presentationCompletion =
      new CompletableFuture<>();
  private final AtomicBoolean started = new AtomicBoolean();
  private final AtomicBoolean cancellationRequested = new AtomicBoolean();
  private final AtomicBoolean closed = new AtomicBoolean();

  private volatile Future<?> workerFuture;
  private volatile Optional<Duration> totalDuration = Optional.empty();
  private volatile ResourceUsage resourceUsage = ResourceUsage.empty();

  ExecutionSession(long generation, DefaultExecutionControl executionControl,
      BoundedExecutionEventStore authoritativeEvents, JavaFxEventSink observerSink,
      ExecutionAnchorRecorder executionAnchorRecorder,
      ExecutionScheduler scheduler, ResourceSampler resourceSampler) {
    this.generation = generation;
    this.executionControl = Objects.requireNonNull(executionControl, "executionControl");
    this.authoritativeEvents = Objects.requireNonNull(authoritativeEvents, "authoritativeEvents");
    this.observerSink = Objects.requireNonNull(observerSink, "observerSink");
    this.executionAnchorRecorder = Objects.requireNonNull(executionAnchorRecorder, "executionAnchorRecorder");
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    this.resourceSampler = Objects.requireNonNull(resourceSampler, "resourceSampler");
  }

  void start(Supplier<ExecutionResult> task) {
    Objects.requireNonNull(task, "task");
    workerFuture = scheduler.submit(() -> {
      long startedAtNanos = System.nanoTime();
      started.set(true);
      resourceSampler.start();
      ExecutionResult result = null;
      Throwable failure = null;
      try {
        result = task.get();
      } catch (Throwable throwable) {
        failure = throwable;
      } finally {
        resourceSampler.stop();
        resourceUsage = resourceSampler.sample();
        totalDuration = Optional.of(elapsedSince(startedAtNanos));
        finishAfterPlayback(result, failure);
        scheduler.close();
      }
    });
  }

  public long generation() {
    return generation;
  }
  public List<EventEnvelope> events() {
    return authoritativeEvents.events();
  }
  public Optional<ExecutionAnchorTimeline> executionAnchors() {
    return executionAnchorRecorder.snapshot();
  }
  public CompletableFuture<ExecutionResult> runtimeCompletion() {
    return runtimeCompletion;
  }
  public CompletableFuture<ExecutionResult> presentationCompletion() {
    return presentationCompletion;
  }
  public Optional<Duration> totalDuration() {
    return totalDuration;
  }
  public ResourceUsage resourceUsage() {
    return resourceUsage;
  }
  public boolean isCancellationRequested() {
    return cancellationRequested.get();
  }
  public boolean isClosed() {
    return closed.get();
  }

  public void pauseExecution() {
    requireOpen();
    observerSink.pause();
    executionControl.pause();
  }

  public void resumeExecution() {
    requireOpen();
    executionControl.resume();
    observerSink.resume();
  }

  public void stepExecution() {
    requireOpen();
    executionControl.step();
    observerSink.step();
  }

  public void cancel() {
    if (!cancellationRequested.compareAndSet(false, true)) {
      return;
    }
    observerSink.close();
    executionControl.cancel();
    Future<?> localFuture = workerFuture;
    if (localFuture != null) {
      localFuture.cancel(true);
    }
    scheduler.shutdownNow();
    if (!started.get()) {
      ExecutionResult cancelled = ExecutionResult.cancelled();
      runtimeCompletion.complete(cancelled);
      presentationCompletion.complete(cancelled);
    }
  }

  public Optional<RuntimeException> dispatcherFailure() {
    return observerSink.dispatcherFailure();
  }
  public Optional<RuntimeException> observerFailure() {
    return observerSink.observerFailure();
  }
  public void closeObserver() {
    observerSink.close();
  }

  @Override
  public void close() {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    observerSink.close();
    if (!runtimeCompletion.isDone()) {
      cancel();
    } else {
      scheduler.close();
    }
  }

  private void finishAfterPlayback(ExecutionResult result, Throwable failure) {
    if (failure != null) {
      runtimeCompletion.completeExceptionally(failure);
      presentationCompletion.completeExceptionally(failure);
      return;
    }
    runtimeCompletion.complete(result);
    observerSink.drained().whenComplete((ignored, playbackFailure) -> {
      // A presentation/dispatcher error must not change the authoritative runtime result.
      Throwable presentationFailure = playbackFailure != null ? playbackFailure
          : observerSink.dispatcherFailure().<Throwable>map(error -> error)
              .orElseGet(() -> observerSink.observerFailure().orElse(null));
      if (presentationFailure != null) presentationCompletion.completeExceptionally(presentationFailure);
      else presentationCompletion.complete(result);
    });
  }

  private void requireOpen() {
    if (closed.get()) {
      throw new IllegalStateException("Execution session is closed");
    }
  }

  private Duration elapsedSince(long startedAtNanos) {
    return Duration.ofNanos(Math.max(0L, System.nanoTime() - startedAtNanos));
  }
}
