package com.majortom.algorithms.library.structure.event;

import com.majortom.algorithms.core.event.ExecutionEvent;

public sealed interface StackStructureEvent extends ExecutionEvent
        permits StackStructureEvent.Pushed, StackStructureEvent.Popped {
    record Pushed(Object value) implements StackStructureEvent {}
    record Popped(Object value) implements StackStructureEvent {}
}
