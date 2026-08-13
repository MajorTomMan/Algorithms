package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.api.AlgorithmContext;
import com.majortom.algorithms.core.api.AlgorithmEvent;
import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Default context responsible for checkpoint ordering and event envelopes. */
final class DefaultAlgorithmContext implements AlgorithmContext {

    private final String runId;
    private final String algorithmId;
    private final EventSink eventSink;
    private final CancellationToken cancellationToken;
    private final ExecutionGate executionGate;
    private final Clock clock;
    private final AtomicLong sequence = new AtomicLong();

    DefaultAlgorithmContext(
            String runId,
            String algorithmId,
            EventSink eventSink,
            CancellationToken cancellationToken,
            ExecutionGate executionGate,
            Clock clock) {
        this.runId = requireText(runId, "runId");
        this.algorithmId = requireText(algorithmId, "algorithmId");
        this.eventSink = Objects.requireNonNull(eventSink, "eventSink");
        this.cancellationToken = Objects.requireNonNull(cancellationToken, "cancellationToken");
        this.executionGate = Objects.requireNonNull(executionGate, "executionGate");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String runId() {
        return runId;
    }

    @Override
    public String algorithmId() {
        return algorithmId;
    }

    @Override
    public void emit(AlgorithmEvent event) {
        Objects.requireNonNull(event, "event");
        if (event instanceof ExecutionLifecycleEvent) {
            throw new IllegalArgumentException("Algorithms cannot emit runtime lifecycle events");
        }
        emitRuntimeEvent(event);
    }

    void emitLifecycle(ExecutionLifecycleEvent event) {
        emitRuntimeEvent(Objects.requireNonNull(event, "event"));
    }

    @Override
    public void checkpoint() throws InterruptedException {
        requireNotCancelled();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Algorithm execution interrupted");
        }
        executionGate.awaitPermission(cancellationToken);
        requireNotCancelled();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Algorithm execution interrupted");
        }
    }

    private void requireNotCancelled() {
        if (cancellationToken.isCancellationRequested()) {
            throw new AlgorithmCancellationException("Algorithm execution cancelled");
        }
    }

    private synchronized void emitRuntimeEvent(AlgorithmEvent event) {
        long eventSequence = sequence.getAndIncrement();
        ExecutionEvent executionEvent = new ExecutionEvent(
                runId,
                algorithmId,
                eventSequence,
                clock.instant(),
                event);
        try {
            eventSink.accept(executionEvent);
        } catch (RuntimeException exception) {
            throw new EventDeliveryException("Execution event delivery failed", exception);
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
