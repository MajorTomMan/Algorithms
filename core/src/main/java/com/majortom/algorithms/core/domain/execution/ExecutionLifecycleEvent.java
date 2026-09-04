package com.majortom.algorithms.core.domain.execution;

import com.majortom.algorithms.core.event.runtime.RuntimeEvent;

/** Runtime-owned lifecycle payloads. */
public sealed interface ExecutionLifecycleEvent extends RuntimeEvent
        permits RunStartedEvent, RunPausedEvent, RunResumedEvent, RunCompletedEvent, RunCancelledEvent, RunFailedEvent {
}
