package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.runtime.DefaultExecutionControl;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionScheduler;
import com.majortom.algorithms.core.runtime.ResourceSampler;
import com.majortom.algorithms.core.runtime.ResourceUsage;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** Handle and authoritative record for one local Runtime run plus independent live playback. */
public final class ExecutionSession implements AutoCloseable {

    private final long generation;
    private final DefaultExecutionControl executionControl;
    private final BoundedExecutionEventStore authoritativeEvents;
    private final JavaFxEventSink observerSink;
    private final ExecutionScheduler scheduler;
    private final ResourceSampler resourceSampler;
    private final CompletableFuture<ExecutionResult> runtimeCompletion = new CompletableFuture<>();
    private final CompletableFuture<ExecutionResult> presentationCompletion = new CompletableFuture<>();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean cancellationRequested = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    private volatile Future<?> workerFuture;
    private volatile Optional<Duration> totalDuration = Optional.empty();
    private volatile ResourceUsage resourceUsage = ResourceUsage.empty();

    ExecutionSession(long generation, DefaultExecutionControl executionControl,
            BoundedExecutionEventStore authoritativeEvents, JavaFxEventSink observerSink,
            ExecutionScheduler scheduler, ResourceSampler resourceSampler) {
        this.generation = generation;
        this.executionControl = Objects.requireNonNull(executionControl, "executionControl");
        this.authoritativeEvents = Objects.requireNonNull(authoritativeEvents, "authoritativeEvents");
        this.observerSink = Objects.requireNonNull(observerSink, "observerSink");
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

    public long generation() { return generation; }
    public List<EventEnvelope> events() { return authoritativeEvents.events(); }
    public CompletableFuture<ExecutionResult> runtimeCompletion() { return runtimeCompletion; }
    public CompletableFuture<ExecutionResult> presentationCompletion() { return presentationCompletion; }
    public Optional<Duration> totalDuration() { return totalDuration; }
    public ResourceUsage resourceUsage() { return resourceUsage; }
    public boolean isCancellationRequested() { return cancellationRequested.get(); }
    public boolean isClosed() { return closed.get(); }

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

    public Optional<RuntimeException> dispatcherFailure() { return observerSink.dispatcherFailure(); }
    public Optional<RuntimeException> observerFailure() { return observerSink.observerFailure(); }
    public void closeObserver() { observerSink.close(); }

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

    void retire() { close(); }

    private void finishAfterPlayback(ExecutionResult result, Throwable failure) {
        if (failure != null) {
            runtimeCompletion.completeExceptionally(failure);
            presentationCompletion.completeExceptionally(failure);
            return;
        }
        runtimeCompletion.complete(result);
        observerSink.drained().whenComplete((ignored, playbackFailure) -> {
            if (playbackFailure != null) presentationCompletion.completeExceptionally(playbackFailure);
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
