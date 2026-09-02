package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.runtime.DefaultExecutionControl;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.EventSink;
import com.majortom.algorithms.core.runtime.ExecutionFailure;
import com.majortom.algorithms.core.runtime.ExecutionOperation;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.runtime.ExecutionScheduler;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.visualization.runtime.Reduction;
import com.majortom.algorithms.visualization.runtime.ReductionCursor;
import com.majortom.algorithms.core.runtime.ResourceSampler;
import javafx.application.Platform;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Local Runtime execution with an independent ordered JavaFX playback queue. */
public final class LocalAlgorithmExecution implements AutoCloseable {

    public static final int DEFAULT_MAXIMUM_EVENT_COUNT = 200_000;

    private final ExecutionRuntime runtime;
    private final Consumer<Runnable> dispatcher;
    private final int maximumEventCount;
    private final Supplier<? extends ResourceSampler> resourceSamplerFactory;
    private final AtomicLong generation = new AtomicLong();
    private final Object lifecycleLock = new Object();
    private boolean closed;
    private ExecutionSession currentSession;

    public LocalAlgorithmExecution() {
        this(new ExecutionRuntime(), Platform::runLater, DEFAULT_MAXIMUM_EVENT_COUNT, LocalResourceSampler::new);
    }

    public LocalAlgorithmExecution(Supplier<? extends ResourceSampler> resourceSamplerFactory) {
        this(new ExecutionRuntime(), Platform::runLater, DEFAULT_MAXIMUM_EVENT_COUNT, resourceSamplerFactory);
    }

    LocalAlgorithmExecution(ExecutionRuntime runtime, Consumer<Runnable> dispatcher) {
        this(runtime, dispatcher, DEFAULT_MAXIMUM_EVENT_COUNT, ResourceSampler::noop);
    }

    LocalAlgorithmExecution(ExecutionRuntime runtime, Consumer<Runnable> dispatcher, int maximumEventCount) {
        this(runtime, dispatcher, maximumEventCount, ResourceSampler::noop);
    }

    LocalAlgorithmExecution(ExecutionRuntime runtime, Consumer<Runnable> dispatcher, int maximumEventCount,
            Supplier<? extends ResourceSampler> resourceSamplerFactory) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.resourceSamplerFactory = Objects.requireNonNull(resourceSamplerFactory, "resourceSamplerFactory");
        if (maximumEventCount < 2) throw new IllegalArgumentException("maximumEventCount must be at least 2");
        this.maximumEventCount = maximumEventCount;
    }

    public <S> ExecutionSession start(String operationId, ExecutionOperation<?> operation, EventReducer<S> reducer,
            Consumer<S> viewStateConsumer) {
        return start(operationId, operation, reducer, viewStateConsumer, () -> 0L);
    }

    public <S> ExecutionSession start(String operationId, ExecutionOperation<?> operation, EventReducer<S> reducer,
            Consumer<S> viewStateConsumer, LongSupplier delayMillisSupplier) {
        return start(operationId, operation, reducer, ignored -> { }, viewStateConsumer, ignored -> { }, delayMillisSupplier);
    }

    public <S> ExecutionSession start(String operationId, ExecutionOperation<?> operation, EventReducer<S> reducer,
            Consumer<com.majortom.algorithms.core.runtime.EventEnvelope> liveEventConsumer,
            Consumer<S> viewStateConsumer, Consumer<ExecutionStatistics> statisticsConsumer,
            LongSupplier delayMillisSupplier) {
        Objects.requireNonNull(operationId, "operationId");
        if (operationId.isBlank()) throw new IllegalArgumentException("operationId must not be blank");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(reducer, "reducer");
        Objects.requireNonNull(liveEventConsumer, "liveEventConsumer");
        Objects.requireNonNull(viewStateConsumer, "viewStateConsumer");
        Objects.requireNonNull(statisticsConsumer, "statisticsConsumer");
        Objects.requireNonNull(delayMillisSupplier, "delayMillisSupplier");

        synchronized (lifecycleLock) {
            requireOpen();
            long runGeneration = generation.incrementAndGet();
            if (currentSession != null) currentSession.retire();

            DefaultExecutionControl executionControl = new DefaultExecutionControl();
            ResourceSampler resourceSampler = Objects.requireNonNull(resourceSamplerFactory.get(), "resourceSamplerFactory result");
            BoundedExecutionEventStore authoritativeEvents = new BoundedExecutionEventStore(maximumEventCount);
            ReductionCursor<S> reductionCursor = new ReductionCursor<>(reducer);
            JavaFxEventSink observerSink = new JavaFxEventSink(dispatcher, event -> {
                if (generation.get() != runGeneration) return;
                synchronized (lifecycleLock) {
                    if (closed || generation.get() != runGeneration) return;
                }
                liveEventConsumer.accept(event);
                Reduction<S> reduction = reductionCursor.accept(event);
                if (reduction.visualFrame()) viewStateConsumer.accept(reduction.state());
                statisticsConsumer.accept(reductionCursor.statistics());
            }, maximumEventCount, delayMillisSupplier);
            EventSink eventSink = event -> {
                authoritativeEvents.accept(event);
                observerSink.accept(event);
                sampleResourceUsage(resourceSampler);
            };
            ExecutionScheduler scheduler = ExecutionScheduler.single("algorithm-run-" + runGeneration + "-");
            ExecutionSession session = new ExecutionSession(runGeneration, executionControl, authoritativeEvents,
                    observerSink, scheduler, resourceSampler);
            currentSession = session;
            session.start(() -> runWithEventLimit(operationId, operation, eventSink, executionControl,
                    authoritativeEvents, observerSink));
            return session;
        }
    }

    public void cancelCurrent() {
        synchronized (lifecycleLock) {
            if (currentSession != null) currentSession.cancel();
        }
    }

    @Override
    public void close() {
        synchronized (lifecycleLock) {
            if (closed) return;
            closed = true;
            generation.incrementAndGet();
            if (currentSession != null) {
                currentSession.retire();
                currentSession = null;
            }
        }
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("Local algorithm execution is closed");
    }

    private void sampleResourceUsage(ResourceSampler resourceSampler) {
        try { resourceSampler.sample(); } catch (RuntimeException ignored) { }
    }

    private ExecutionResult runWithEventLimit(String operationId, ExecutionOperation<?> operation, EventSink eventSink,
            DefaultExecutionControl executionControl, BoundedExecutionEventStore authoritativeEvents,
            JavaFxEventSink observerSink) {
        ExecutionResult result = runtime.execute(operationId, eventSink, executionControl, operation);
        if (!authoritativeEvents.limitExceeded()) return result;
        var failureEvent = authoritativeEvents.recordLimitFailure();
        observerSink.accept(failureEvent);
        ExecutionFailure failure = new ExecutionFailure(BoundedExecutionEventStore.limitFailureCode(),
                authoritativeEvents.limitMessage(), BoundedExecutionEventStore.EventLimitExceededException.class.getName());
        return ExecutionResult.failed(failure);
    }
}
