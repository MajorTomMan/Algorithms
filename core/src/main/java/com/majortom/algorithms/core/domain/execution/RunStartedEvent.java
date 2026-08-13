package com.majortom.algorithms.core.domain.execution;

/** Indicates that validated algorithm code is about to execute. */
public record RunStartedEvent() implements ExecutionLifecycleEvent {
}
