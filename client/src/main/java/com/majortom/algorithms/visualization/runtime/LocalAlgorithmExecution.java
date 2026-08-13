package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.api.AlgorithmInput;
import com.majortom.algorithms.core.api.AlgorithmInvoker;
import com.majortom.algorithms.core.runtime.DefaultAlgorithmRunner;
import com.majortom.algorithms.core.runtime.DefaultExecutionControl;
import com.majortom.algorithms.core.runtime.EventSink;
import com.majortom.algorithms.core.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.ExecutionFailure;
import com.majortom.algorithms.core.runtime.ExecutionEvent;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.Reduction;
import com.majortom.algorithms.core.runtime.ReductionCursor;
import javafx.application.Platform;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Desktop lifecycle boundary for local algorithm execution. */
public final class LocalAlgorithmExecution implements AutoCloseable {

    public static final int DEFAULT_MAXIMUM_EVENT_COUNT = 200_000;

    private final DefaultAlgorithmRunner runner;
    private final Consumer<Runnable> dispatcher;
    private final int maximumEventCount;
    private final AtomicLong generation = new AtomicLong();
    private final Object lifecycleLock = new Object();

    private boolean closed;
    private ExecutionSession currentSession;

    public LocalAlgorithmExecution() {
        this(new DefaultAlgorithmRunner(), Platform::runLater, DEFAULT_MAXIMUM_EVENT_COUNT);
    }

    LocalAlgorithmExecution(DefaultAlgorithmRunner runner, Consumer<Runnable> dispatcher) {
        this(runner, dispatcher, DEFAULT_MAXIMUM_EVENT_COUNT);
    }

    LocalAlgorithmExecution(
            DefaultAlgorithmRunner runner,
            Consumer<Runnable> dispatcher,
            int maximumEventCount) {
        this.runner = Objects.requireNonNull(runner, "runner");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        if (maximumEventCount < 2) {
            throw new IllegalArgumentException("maximumEventCount must be at least 2");
        }
        this.maximumEventCount = maximumEventCount;
    }

    public <S> ExecutionSession start(
            AlgorithmInvoker invoker,
            AlgorithmInput input,
            EventReducer<S> reducer,
            Consumer<S> viewStateConsumer) {
        return start(invoker, input, reducer, viewStateConsumer, () -> 0L);
    }

    public <S> ExecutionSession start(
            AlgorithmInvoker invoker,
            AlgorithmInput input,
            EventReducer<S> reducer,
            Consumer<S> viewStateConsumer,
            LongSupplier delayMillisSupplier) {
        Objects.requireNonNull(invoker, "invoker");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(reducer, "reducer");
        Objects.requireNonNull(viewStateConsumer, "viewStateConsumer");
        Objects.requireNonNull(delayMillisSupplier, "delayMillisSupplier");

        synchronized (lifecycleLock) {
            requireOpen();
            long runGeneration = generation.incrementAndGet();
            if (currentSession != null) {
                currentSession.retire();
            }

            DefaultExecutionControl executionControl = new DefaultExecutionControl();
            BoundedExecutionEventStore authoritativeEvents = new BoundedExecutionEventStore(maximumEventCount);
            InterruptibleEventPacer pacer = new InterruptibleEventPacer(delayMillisSupplier);
            ReductionCursor<S> reductionCursor = new ReductionCursor<>(reducer);
            JavaFxEventSink observerSink = new JavaFxEventSink(dispatcher, event -> {
                if (generation.get() != runGeneration) {
                    return;
                }
                synchronized (lifecycleLock) {
                    if (closed || generation.get() != runGeneration) {
                        return;
                    }
                }
                Reduction<S> reduction = reductionCursor.accept(event);
                if (reduction.visualFrame()) {
                    viewStateConsumer.accept(reduction.state());
                }
            });
            EventSink eventSink = event -> {
                authoritativeEvents.accept(event);
                observerSink.accept(event);
                pacer.awaitAfter(event);
            };
            ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "algorithm-run-" + runGeneration);
                thread.setDaemon(true);
                return thread;
            });
            ExecutionSession session = new ExecutionSession(
                    runGeneration,
                    executionControl,
                    authoritativeEvents,
                    observerSink,
                    pacer,
                    executor);
            currentSession = session;
            session.start(() -> runWithEventLimit(invoker, input, eventSink, executionControl,
                    authoritativeEvents, observerSink));
            return session;
        }
    }

    public void cancelCurrent() {
        synchronized (lifecycleLock) {
            if (currentSession != null) {
                currentSession.cancel();
            }
        }
    }

    @Override
    public void close() {
        synchronized (lifecycleLock) {
            if (closed) {
                return;
            }
            closed = true;
            generation.incrementAndGet();
            if (currentSession != null) {
                currentSession.retire();
                currentSession = null;
            }
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Local algorithm execution is closed");
        }
    }

    private ExecutionResult runWithEventLimit(
            AlgorithmInvoker invoker,
            AlgorithmInput input,
            EventSink eventSink,
            DefaultExecutionControl executionControl,
            BoundedExecutionEventStore authoritativeEvents,
            JavaFxEventSink observerSink) {
        ExecutionResult result = runner.run(invoker, input, eventSink, executionControl);
        if (!authoritativeEvents.limitExceeded()) {
            observerSink.awaitDrained();
            return result;
        }
        ExecutionEvent failureEvent = authoritativeEvents.recordLimitFailure();
        observerSink.accept(failureEvent);
        observerSink.awaitDrained();
        ExecutionFailure failure = new ExecutionFailure(
                BoundedExecutionEventStore.limitFailureCode(),
                authoritativeEvents.limitMessage(),
                BoundedExecutionEventStore.EventLimitExceededException.class.getName());
        return ExecutionResult.failed(failure);
    }
}
