package com.majortom.algorithms.core.domain.execution;

import com.majortom.algorithms.core.event.ExecutionEvent;

/** Runtime-owned lifecycle payloads. */
public sealed interface ExecutionLifecycleEvent extends ExecutionEvent
        permits RunStartedEvent, RunPausedEvent, RunResumedEvent, RunCompletedEvent, RunCancelledEvent, RunFailedEvent {
}
