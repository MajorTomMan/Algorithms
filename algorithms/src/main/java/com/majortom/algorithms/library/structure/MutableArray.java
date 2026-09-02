package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.structure.event.ArrayStructureEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class MutableArray<T> implements ArrayStructure<T> {
    private final ArrayList<T> values;

    public MutableArray() {
        values = new ArrayList<>();
    }

    public MutableArray(Collection<? extends T> values) {
        this.values = new ArrayList<>(Objects.requireNonNull(values, "values"));
    }

    @Override public int size() { return values.size(); }
    @Override public T get(int index) { return values.get(index); }

    @Override
    public void set(int index, T value) {
        T previous = values.set(index, value);
        ExecutionEvents.emit(new ArrayStructureEvent.Updated(index, previous, value));
    }

    @Override
    public void insert(int index, T value) {
        values.add(index, value);
        ExecutionEvents.emit(new ArrayStructureEvent.Inserted(index, value));
    }

    @Override
    public T remove(int index) {
        T removed = values.remove(index);
        ExecutionEvents.emit(new ArrayStructureEvent.Removed(index, removed));
        return removed;
    }

    @Override
    public void swap(int leftIndex, int rightIndex) {
        if (leftIndex == rightIndex) return;
        T left = values.get(leftIndex);
        T right = values.get(rightIndex);
        values.set(leftIndex, right);
        values.set(rightIndex, left);
        ExecutionEvents.emit(new ArrayStructureEvent.Swapped(leftIndex, rightIndex, right, left));
    }

    @Override public List<T> raw() { return values; }
}
