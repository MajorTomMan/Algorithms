package com.majortom.algorithms.library.sort;

import com.majortom.algorithms.core.event.observation.ObservationEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.structure.ArrayStructure;

/** Shared direct helpers for concrete integer sorting algorithms. */
public abstract class AbstractIntegerSort implements Sort<Integer> {

    protected final int compareAt(ArrayStructure<Integer> array, int leftIndex, int rightIndex) {
        ExecutionEvents.observe(new ObservationEvent.Compared(
                new ObservationEvent.IndexRef("array", leftIndex),
                new ObservationEvent.IndexRef("array", rightIndex)));
        return compare(array.get(leftIndex), array.get(rightIndex));
    }

    protected final int compareValue(ArrayStructure<Integer> array, int index, int value) {
        ExecutionEvents.observe(new ObservationEvent.Compared(
                new ObservationEvent.IndexRef("array", index),
                new ObservationEvent.ValueRef(value)));
        return compare(array.get(index), value);
    }

    protected final void write(ArrayStructure<Integer> array, int index, int value) {
        array.set(index, value);
    }

    protected final void swap(ArrayStructure<Integer> array, int leftIndex, int rightIndex) {
        if (leftIndex != rightIndex) {
            array.swap(leftIndex, rightIndex);
        }
    }
}
