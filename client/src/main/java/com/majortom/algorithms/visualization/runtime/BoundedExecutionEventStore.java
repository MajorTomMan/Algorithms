package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.domain.execution.RunFailedEvent;
import com.majortom.algorithms.core.runtime.EventSink;
import com.majortom.algorithms.core.runtime.ExecutionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Authoritative per-run event store with one slot reserved for a limit failure event. */
final class BoundedExecutionEventStore implements EventSink {

    private static final String LIMIT_FAILURE_CODE = "client.execution.event-limit-exceeded";

    private final int maximumEventCount;
    private final List<ExecutionEvent> events = new ArrayList<>();

    private ExecutionEvent rejectedEvent;

    BoundedExecutionEventStore(int maximumEventCount) {
        if (maximumEventCount < 2) {
            throw new IllegalArgumentException("maximumEventCount must be at least 2");
        }
        this.maximumEventCount = maximumEventCount;
    }

    @Override
    public synchronized void accept(ExecutionEvent event) {
        Objects.requireNonNull(event, "event");
        if (rejectedEvent != null) {
            throw eventLimitExceeded();
        }
        if (events.size() >= maximumEventCount - 1) {
            rejectedEvent = event;
            throw eventLimitExceeded();
        }
        events.add(event);
    }

    synchronized List<ExecutionEvent> events() {
        return List.copyOf(events);
    }

    synchronized boolean limitExceeded() {
        return rejectedEvent != null;
    }

    synchronized ExecutionEvent recordLimitFailure() {
        if (rejectedEvent == null) {
            throw new IllegalStateException("The event limit has not been exceeded");
        }
        if (!events.isEmpty()
                && events.getLast().payload() instanceof RunFailedEvent failedEvent
                && LIMIT_FAILURE_CODE.equals(failedEvent.code())) {
            return events.getLast();
        }
        String message = limitMessage();
        ExecutionEvent failedEvent = new ExecutionEvent(
                rejectedEvent.runId(),
                rejectedEvent.algorithmId(),
                rejectedEvent.sequence(),
                rejectedEvent.occurredAt(),
                new RunFailedEvent(LIMIT_FAILURE_CODE, message));
        events.add(failedEvent);
        return failedEvent;
    }

    synchronized String limitMessage() {
        int retainedEvents = maximumEventCount - 1;
        return "Execution exceeded the client limit of " + retainedEvents
                + " events; one terminal failure event was retained";
    }

    static String limitFailureCode() {
        return LIMIT_FAILURE_CODE;
    }

    private EventLimitExceededException eventLimitExceeded() {
        return new EventLimitExceededException(limitMessage());
    }

    static final class EventLimitExceededException extends RuntimeException {

        EventLimitExceededException(String message) {
            super(message);
        }
    }
}
