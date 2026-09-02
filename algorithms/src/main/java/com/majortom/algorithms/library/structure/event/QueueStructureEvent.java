package com.majortom.algorithms.library.structure.event;

import com.majortom.algorithms.core.event.ExecutionEvent;

public sealed interface QueueStructureEvent extends ExecutionEvent
        permits QueueStructureEvent.Enqueued, QueueStructureEvent.Dequeued {
    record Enqueued(Object value) implements QueueStructureEvent {}
    record Dequeued(Object value) implements QueueStructureEvent {}
}
