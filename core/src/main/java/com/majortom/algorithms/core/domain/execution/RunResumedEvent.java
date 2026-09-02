package com.majortom.algorithms.core.domain.execution;

/** Indicates that runtime execution has left the paused state. */
public record RunResumedEvent() implements ExecutionLifecycleEvent {
}
