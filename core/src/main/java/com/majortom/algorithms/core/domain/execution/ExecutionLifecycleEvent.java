package com.majortom.algorithms.core.domain.execution;

import com.majortom.algorithms.core.api.AlgorithmEvent;

/** Runtime-owned lifecycle payloads. */
public sealed interface ExecutionLifecycleEvent extends AlgorithmEvent
        permits RunStartedEvent, RunCompletedEvent, RunCancelledEvent, RunFailedEvent {
}
