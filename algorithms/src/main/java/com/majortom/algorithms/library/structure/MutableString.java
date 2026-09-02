package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.structure.event.StringStructureEvent;

import java.util.Objects;

public final class MutableString implements StringStructure {
    private final StringBuilder value;

    public MutableString() { this(""); }
    public MutableString(String value) { this.value = new StringBuilder(Objects.requireNonNull(value, "value")); }

    @Override public int length() { return value.length(); }
    @Override public char charAt(int index) { return value.charAt(index); }

    @Override
    public void replace(String newValue) {
        Objects.requireNonNull(newValue, "value");
        String previous = value.toString();
        value.setLength(0);
        value.append(newValue);
        ExecutionEvents.emit(new StringStructureEvent.Replaced(previous, newValue));
    }

    @Override
    public void insert(int index, String inserted) {
        Objects.requireNonNull(inserted, "value");
        value.insert(index, inserted);
        ExecutionEvents.emit(new StringStructureEvent.Inserted(index, inserted));
    }

    @Override
    public String remove(int index, int length) {
        if (length < 0 || index < 0 || index + length > value.length()) throw new IndexOutOfBoundsException();
        String removed = value.substring(index, index + length);
        value.delete(index, index + length);
        ExecutionEvents.emit(new StringStructureEvent.Removed(index, removed));
        return removed;
    }

    @Override
    public char update(int index, char newValue) {
        char previous = value.charAt(index);
        value.setCharAt(index, newValue);
        ExecutionEvents.emit(new StringStructureEvent.Updated(index, previous, newValue));
        return previous;
    }

    @Override public String raw() { return value.toString(); }
}
