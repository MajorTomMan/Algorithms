package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.event.ExecutionEvent;

import java.time.Instant;
import java.util.Objects;

/** Runtime metadata wrapper around one semantic execution event. */
public record EventEnvelope(
        String runId,
        String operationId,
        long sequence,
        Instant timestamp,
        String source,
        ExecutionEvent event) {

    public EventEnvelope {
        runId = requireText(runId, "runId");
        operationId = requireText(operationId, "operationId");
        source = requireText(source, "source");
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must not be negative");
        }
        timestamp = Objects.requireNonNull(timestamp, "timestamp");
        event = Objects.requireNonNull(event, "event");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
