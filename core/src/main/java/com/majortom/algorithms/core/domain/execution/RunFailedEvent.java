package com.majortom.algorithms.core.domain.execution;

import java.util.Objects;

/** Indicates validation or execution failure without exposing a Throwable as event data. */
public record RunFailedEvent(String code, String message) implements ExecutionLifecycleEvent {

    public RunFailedEvent {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }
}
