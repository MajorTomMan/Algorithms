package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.event.ExecutionEvent;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

final class RuntimeEventContext {

    private final String runId;
    private final String operationId;
    private final String source;
    private final EventSink eventSink;
    private final RunControl runControl;
    private final Clock clock;
    private final AtomicLong sequence = new AtomicLong();

    RuntimeEventContext(String runId, String operationId, String source, EventSink eventSink, RunControl runControl, Clock clock) {
        this.runId = req(runId, "runId");
        this.operationId = req(operationId, "operationId");
        this.source = req(source, "source");
        this.eventSink = Objects.requireNonNull(eventSink);
        this.runControl = Objects.requireNonNull(runControl);
        this.clock = Objects.requireNonNull(clock);
    }

    void emit(ExecutionEvent event) {
        Objects.requireNonNull(event);
        if (event instanceof ExecutionLifecycleEvent) throw new IllegalArgumentException("Domain code cannot emit runtime lifecycle events");
        publish(event);
    }

    void emitLifecycle(ExecutionLifecycleEvent event) {
        publish(Objects.requireNonNull(event));
    }

    void checkpoint() throws InterruptedException {
        await(false);
    }

    void domainCheckpoint() throws InterruptedException {
        await(true);
    }

    private void await(boolean domainEvent) throws InterruptedException {
        requireNotCancelled();
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Execution interrupted");
        if (domainEvent) runControl.awaitDomainEventPermission(runControl);
        else runControl.awaitPermission(runControl);
        requireNotCancelled();
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Execution interrupted");
    }

    private void requireNotCancelled() {
        if (runControl.isCancellationRequested()) throw new ExecutionCancellationException("Execution cancelled");
    }

    private synchronized void publish(ExecutionEvent event) {
        EventEnvelope envelope = new EventEnvelope(runId, operationId, sequence.getAndIncrement(), clock.instant(), source, event);
        try {
            eventSink.accept(envelope);
        } catch (RuntimeException e) {
            throw new EventDeliveryException("Execution event delivery failed", e);
        }
    }

    private static String req(String v, String n) {
        Objects.requireNonNull(v, n);
        if (v.isBlank()) throw new IllegalArgumentException(n + " must not be blank");
        return v;
    }
}
