package com.majortom.algorithms.core.domain.execution;

import java.util.Objects;

/** Indicates cooperative cancellation or thread interruption. */
public record RunCancelledEvent(String reason) implements ExecutionLifecycleEvent {

    public RunCancelledEvent {
        Objects.requireNonNull(reason, "reason");
    }
}
