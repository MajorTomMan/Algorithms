package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.api.AlgorithmEvent;

import java.time.Instant;
import java.util.Objects;

/** Runtime-owned envelope that prevents algorithms from forging run metadata or sequence numbers. */
public record ExecutionEvent(
        String runId,
        String algorithmId,
        long sequence,
        Instant occurredAt,
        AlgorithmEvent payload) {

    public ExecutionEvent {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(algorithmId, "algorithmId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(payload, "payload");
        if (runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        if (algorithmId.isBlank()) {
            throw new IllegalArgumentException("algorithmId must not be blank");
        }
        if (sequence < 0L) {
            throw new IllegalArgumentException("sequence must not be negative");
        }
    }
}
