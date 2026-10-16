package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.runtime.DefaultExecutionControl;
import com.majortom.algorithms.core.runtime.EventSink;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionFailure;
import com.majortom.algorithms.core.runtime.ExecutionAnchorRecorder;
import com.majortom.algorithms.core.runtime.ExecutionOperation;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.runtime.ExecutionScheduler;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.core.runtime.ResourceSampler;
import com.majortom.algorithms.visualization.render.fx.FxDispatch;
import com.majortom.algorithms.visualization.render.runtime.RenderRuntime;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;
import com.majortom.algorithms.visualization.runtime.ReductionCursor;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * core.ExecutionRuntime 到 JavaFX 客户端之间的本地执行适配层。
 *
 * <p>每次 start 创建新的 generation、ExecutionScheduler、RunControl、
 * BoundedExecutionEventStore、ExecutionAnchorRecorder 与 JavaFxEventSink。
 * worker 负责实际执行；同一权威事件先进入有界存储/时间锚点，再经观察者队列
 * 交付给 Reducer 和 UI。渲染积压不能反过来改变已记录的事实顺序。
 *
 * <p>generation 隔离被替换或取消的旧会话，避免过期异步回调覆盖新运行状态。
 * 可批量交付视觉通知以减小 JavaFX 负担，但不会合并权威事件流；
 * 超过事件数上限时明确返回失败，不能默默截断历史还标记成功。
 *
 * <p>算法生命周期、事件序号归属 core；重放帧和 FX 线程归属 client。
 */
public final class LocalAlgorithmExecution implements AutoCloseable {
  public static final int DEFAULT_MAXIMUM_EVENT_COUNT = ExecutionLimits.DEFAULT_MAXIMUM_EVENT_COUNT;
  private static final int RESOURCE_SAMPLE_INTERVAL = 128;

  private final ExecutionRuntime runtime;
  private final Consumer<Runnable> dispatcher;
  private final int maximumEventCount;
  private final Supplier<? extends ResourceSampler> resourceSamplerFactory;
  private final AtomicLong generation = new AtomicLong();
  private final Object lifecycleLock = new Object();
  private boolean closed;
  private ExecutionSession currentSession;

  public LocalAlgorithmExecution() {
    this(new ExecutionRuntime(), FxDispatch::defer, DEFAULT_MAXIMUM_EVENT_COUNT,
        LocalResourceSampler::new);
  }

  public LocalAlgorithmExecution(Supplier<? extends ResourceSampler> resourceSamplerFactory) {
    this(new ExecutionRuntime(), FxDispatch::defer, DEFAULT_MAXIMUM_EVENT_COUNT,
        resourceSamplerFactory);
  }

  LocalAlgorithmExecution(ExecutionRuntime runtime, Consumer<Runnable> dispatcher) {
    this(runtime, dispatcher, DEFAULT_MAXIMUM_EVENT_COUNT, ResourceSampler::noop);
  }

  LocalAlgorithmExecution(
      ExecutionRuntime runtime, Consumer<Runnable> dispatcher, int maximumEventCount) {
    this(runtime, dispatcher, maximumEventCount, ResourceSampler::noop);
  }

  LocalAlgorithmExecution(ExecutionRuntime runtime, Consumer<Runnable> dispatcher,
      int maximumEventCount, Supplier<? extends ResourceSampler> resourceSamplerFactory) {
    this.runtime = Objects.requireNonNull(runtime, "runtime");
    this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    this.resourceSamplerFactory =
        Objects.requireNonNull(resourceSamplerFactory, "resourceSamplerFactory");
    if (maximumEventCount < 2)
      throw new IllegalArgumentException("maximumEventCount must be at least 2");
    this.maximumEventCount = maximumEventCount;
  }

  public <S> ExecutionSession start(String operationId, ExecutionOperation<?> operation,
      EventReducer<S> reducer, Consumer<S> viewStateConsumer) {
    return start(operationId, operation, reducer, viewStateConsumer, () -> 0L);
  }

  public <S> ExecutionSession start(String operationId, ExecutionOperation<?> operation,
      EventReducer<S> reducer, Consumer<S> viewStateConsumer, LongSupplier delayMillisSupplier) {
    return start(operationId, operation, reducer,
        ignored -> {}, viewStateConsumer, ignored -> {}, delayMillisSupplier);
  }

  public <S> ExecutionSession start(String operationId, ExecutionOperation<?> operation,
      EventReducer<S> reducer, Consumer<EventEnvelope> liveEventConsumer,
      Consumer<S> viewStateConsumer, Consumer<ExecutionStatistics> statisticsConsumer,
      LongSupplier delayMillisSupplier) {
    Objects.requireNonNull(liveEventConsumer, "liveEventConsumer");
    return startInternal(operationId, operation, reducer,
        events -> events.forEach(liveEventConsumer), viewStateConsumer,
        statisticsConsumer, delayMillisSupplier, 1);
  }

  public <S> ExecutionSession startBatched(String operationId, ExecutionOperation<?> operation,
      EventReducer<S> reducer, Consumer<List<EventEnvelope>> liveEventsConsumer,
      Consumer<S> viewStateConsumer, Consumer<ExecutionStatistics> statisticsConsumer,
      LongSupplier delayMillisSupplier) {
    return startInternal(operationId, operation, reducer, liveEventsConsumer,
        viewStateConsumer, statisticsConsumer, delayMillisSupplier, 256);
  }

  /**
   * 建立一次完整运行的“记录通道”和“观察通道”。
   * 前者是权威事实，后者允许批处理/延迟以保护 JavaFX 响应；
   * 两者必须保持顺序，不能用 UI 队列取代事件存储。
   */
  private <S> ExecutionSession startInternal(String operationId, ExecutionOperation<?> operation,
      EventReducer<S> reducer, Consumer<List<EventEnvelope>> liveEventsConsumer,
      Consumer<S> viewStateConsumer, Consumer<ExecutionStatistics> statisticsConsumer,
      LongSupplier delayMillisSupplier, int maxBatchSize) {
    Objects.requireNonNull(operationId, "operationId");
    if (operationId.isBlank())
      throw new IllegalArgumentException("operationId must not be blank");
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(reducer, "reducer");
    Objects.requireNonNull(liveEventsConsumer, "liveEventsConsumer");
    Objects.requireNonNull(viewStateConsumer, "viewStateConsumer");
    Objects.requireNonNull(statisticsConsumer, "statisticsConsumer");
    Objects.requireNonNull(delayMillisSupplier, "delayMillisSupplier");

    synchronized (lifecycleLock) {
      requireOpen();
      long runGeneration = generation.incrementAndGet();
      if (currentSession != null)
        currentSession.close();

      DefaultExecutionControl executionControl = new DefaultExecutionControl();
      ResourceSampler resourceSampler =
          Objects.requireNonNull(resourceSamplerFactory.get(), "resourceSamplerFactory result");
      BoundedExecutionEventStore authoritativeEvents =
          new BoundedExecutionEventStore(maximumEventCount);
      ExecutionAnchorRecorder executionAnchorRecorder = new ExecutionAnchorRecorder();
      ReductionCursor<S> reductionCursor = new ReductionCursor<>(reducer);
      JavaFxEventSink observerSink = new JavaFxEventSink(dispatcher, events -> {
        if (generation.get() != runGeneration)
          return;
        synchronized (lifecycleLock) {
          if (closed || generation.get() != runGeneration)
            return;
        }
        S newestVisibleState = null;
        boolean hasVisualFrame = false;
        for (EventEnvelope event : events) {
          Reduction<S> reduction = reductionCursor.accept(event);
          if (reduction.visualFrame()) {
            newestVisibleState = reduction.state();
            hasVisualFrame = true;
          }
        }
        if (hasVisualFrame)
          viewStateConsumer.accept(newestVisibleState);
        statisticsConsumer.accept(reductionCursor.statistics());
        liveEventsConsumer.accept(events);
      }, maximumEventCount, delayMillisSupplier, RenderRuntime.clock(), maxBatchSize);
      // A JVM MXBean query for every observation can cost more than the
      // algorithm step itself. Periodic best-effort sampling is enough for
      // peak-memory estimation; ExecutionSession.stop() samples the final state.
      final int[] eventsSinceSample = {0};
      EventSink eventSink = event -> {
        authoritativeEvents.accept(event);
        executionAnchorRecorder.accept(event);
        observerSink.accept(event);
        if (++eventsSinceSample[0] >= RESOURCE_SAMPLE_INTERVAL) {
          eventsSinceSample[0] = 0;
          resourceSampler.sample();
        }
      };
      ExecutionScheduler scheduler =
          ExecutionScheduler.single("algorithm-run-" + runGeneration + "-");
      ExecutionSession session = new ExecutionSession(runGeneration, executionControl,
          authoritativeEvents, observerSink, executionAnchorRecorder, scheduler, resourceSampler);
      currentSession = session;
      session.start(()
                        -> runWithEventLimit(operationId, operation, eventSink, executionControl,
                            authoritativeEvents, observerSink, executionAnchorRecorder));
      return session;
    }
  }

  public void cancelCurrent() {
    synchronized (lifecycleLock) {
      if (currentSession != null)
        currentSession.cancel();
    }
  }

  @Override
  public void close() {
    synchronized (lifecycleLock) {
      if (closed)
        return;
      closed = true;
      generation.incrementAndGet();
      if (currentSession != null) {
        currentSession.close();
        currentSession = null;
      }
    }
  }

  private void requireOpen() {
    if (closed)
      throw new IllegalStateException("Local algorithm execution is closed");
  }

  private ExecutionResult runWithEventLimit(String operationId, ExecutionOperation<?> operation,
      EventSink eventSink, DefaultExecutionControl executionControl,
      BoundedExecutionEventStore authoritativeEvents, JavaFxEventSink observerSink,
      ExecutionAnchorRecorder executionAnchorRecorder) {
    ExecutionResult result = runtime.execute(operationId, eventSink, executionControl, operation);
    if (!authoritativeEvents.limitExceeded())
      return result;
    var failureEvent = authoritativeEvents.recordLimitFailure();
    executionAnchorRecorder.accept(failureEvent);
    observerSink.accept(failureEvent);
    ExecutionFailure failure = new ExecutionFailure(BoundedExecutionEventStore.limitFailureCode(),
        authoritativeEvents.limitMessage(),
        BoundedExecutionEventStore.EventLimitExceededException.class.getName());
    return ExecutionResult.failed(failure);
  }
}
