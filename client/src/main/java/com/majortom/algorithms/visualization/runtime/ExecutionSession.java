package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.runtime.DefaultExecutionControl;
import com.majortom.algorithms.core.runtime.ExecutionEvent;
import com.majortom.algorithms.core.runtime.ExecutionResult;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** Handle and authoritative record for one local algorithm run. */
public final class ExecutionSession implements AutoCloseable {

    private final long generation;
    private final DefaultExecutionControl executionControl;
    private final BoundedExecutionEventStore authoritativeEvents;
    private final JavaFxEventSink observerSink;
    private final InterruptibleEventPacer pacer;
    private final ExecutorService executor;
    private final CompletableFuture<ExecutionResult> completion = new CompletableFuture<>();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean cancellationRequested = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    private volatile Future<?> workerFuture;

    ExecutionSession(
            long generation,
            DefaultExecutionControl executionControl,
            BoundedExecutionEventStore authoritativeEvents,
            JavaFxEventSink observerSink,
            InterruptibleEventPacer pacer,
            ExecutorService executor) {
        this.generation = generation;
        this.executionControl = Objects.requireNonNull(executionControl, "executionControl");
        this.authoritativeEvents = Objects.requireNonNull(authoritativeEvents, "authoritativeEvents");
        this.observerSink = Objects.requireNonNull(observerSink, "observerSink");
        this.pacer = Objects.requireNonNull(pacer, "pacer");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    void start(Supplier<ExecutionResult> task) {
        Objects.requireNonNull(task, "task");
        workerFuture = executor.submit(() -> {
            started.set(true);
            try {
                completion.complete(task.get());
            } catch (Throwable throwable) {
                completion.completeExceptionally(throwable);
            } finally {
                executor.shutdown();
            }
        });
    }

    public long generation() {
        return generation;
    }

    public List<ExecutionEvent> events() {
        return authoritativeEvents.events();
    }

    public CompletableFuture<ExecutionResult> completion() {
        return completion;
    }

    public boolean isCancellationRequested() {
        return cancellationRequested.get();
    }

    public boolean isClosed() {
        return closed.get();
    }

    public void pauseExecution() {
        requireOpen();
        executionControl.pause();
        pacer.pause();
    }

    public void resumeExecution() {
        requireOpen();
        pacer.resume();
        executionControl.resume();
    }

    public void cancel() {
        if (!cancellationRequested.compareAndSet(false, true)) {
            return;
        }
        observerSink.close();
        pacer.cancel();
        executionControl.cancel();
        Future<?> localFuture = workerFuture;
        if (localFuture != null) {
            localFuture.cancel(true);
        }
        executor.shutdownNow();
        if (!started.get()) {
            completion.complete(ExecutionResult.cancelled());
        }
    }

    public Optional<RuntimeException> dispatcherFailure() {
        return observerSink.dispatcherFailure();
    }

    public Optional<RuntimeException> observerFailure() {
        return observerSink.observerFailure();
    }

    /** Invalidates and clears queued live-observer work before authoritative final rendering. */
    public void closeObserver() {
        observerSink.close();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        observerSink.close();
        pacer.cancel();
        if (!completion.isDone()) {
            cancel();
        } else {
            executor.shutdown();
        }
    }

    void retire() {
        close();
    }

    private void requireOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Execution session is closed");
        }
    }
}
