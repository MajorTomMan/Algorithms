package com.majortom.algorithms.core.event.structure;

import com.majortom.algorithms.core.event.ExecutionEvent;

public sealed interface StringStructureEvent extends ExecutionEvent
        permits StringStructureEvent.Replaced, StringStructureEvent.Inserted,
        StringStructureEvent.Removed, StringStructureEvent.Updated {

    record Replaced(int index, java.lang.String previousValue, java.lang.String value) implements StringStructureEvent {}

    record Inserted(int index, java.lang.String value) implements StringStructureEvent {}

    record Removed(int index, java.lang.String value) implements StringStructureEvent {}

    record Updated(int index, char previousValue, char value) implements StringStructureEvent {}
}
