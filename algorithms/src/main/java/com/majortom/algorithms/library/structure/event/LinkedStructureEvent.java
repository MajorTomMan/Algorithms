package com.majortom.algorithms.library.structure.event;

import com.majortom.algorithms.core.event.ExecutionEvent;

public sealed interface LinkedStructureEvent extends ExecutionEvent
        permits LinkedStructureEvent.Inserted, LinkedStructureEvent.Removed, LinkedStructureEvent.Updated {
    record Inserted(int index, Object value) implements LinkedStructureEvent {}
    record Removed(int index, Object value) implements LinkedStructureEvent {}
    record Updated(int index, Object previousValue, Object value) implements LinkedStructureEvent {}
}
