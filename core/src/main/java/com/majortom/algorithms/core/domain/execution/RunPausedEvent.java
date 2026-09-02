package com.majortom.algorithms.core.domain.execution;

/** Indicates that runtime execution has entered the paused state. */
public record RunPausedEvent() implements ExecutionLifecycleEvent {
}
