package com.majortom.algorithms.library.structure.event;

import com.majortom.algorithms.core.event.ExecutionEvent;

public sealed interface StringStructureEvent extends ExecutionEvent
        permits StringStructureEvent.Replaced, StringStructureEvent.Inserted,
        StringStructureEvent.Removed, StringStructureEvent.Updated {
    record Replaced(String previousValue, String value) implements StringStructureEvent {}
    record Inserted(int index, String value) implements StringStructureEvent {}
    record Removed(int index, String value) implements StringStructureEvent {}
    record Updated(int index, char previousValue, char value) implements StringStructureEvent {}
}
