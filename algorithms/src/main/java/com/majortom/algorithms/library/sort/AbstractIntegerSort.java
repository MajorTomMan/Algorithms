package com.majortom.algorithms.library.sort;

import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.sort.event.SortComparedEvent;
import com.majortom.algorithms.library.sort.event.SortCompletedEvent;
import com.majortom.algorithms.library.sort.event.SortElementSettledEvent;
import com.majortom.algorithms.library.sort.event.SortInitializedEvent;
import com.majortom.algorithms.library.sort.event.SortPivotSelectedEvent;
import com.majortom.algorithms.library.sort.event.SortRangeSelectedEvent;
import com.majortom.algorithms.library.sort.model.IntegerSortInput;
import com.majortom.algorithms.library.sort.model.IntegerSortOutput;
import com.majortom.algorithms.library.structure.ArrayStructure;
import com.majortom.algorithms.library.structure.MutableArray;

import java.util.List;

/** Shared event helpers for concrete integer sorting algorithms. */
public abstract class AbstractIntegerSort implements Sort<Integer> {

    public final IntegerSortOutput sort(IntegerSortInput input) {
        MutableArray<Integer> array = new MutableArray<>(input.values());
        ExecutionEvents.emit(new SortInitializedEvent(List.copyOf(array.raw())));
        sort(array);
        List<Integer> result = List.copyOf(array.raw());
        ExecutionEvents.emit(new SortCompletedEvent(result));
        return new IntegerSortOutput(result);
    }

    protected final int compareAt(ArrayStructure<Integer> array, int leftIndex, int rightIndex) {
        int left = array.get(leftIndex);
        int right = array.get(rightIndex);
        ExecutionEvents.emit(new SortComparedEvent(leftIndex, rightIndex, left, right));
        return compare(left, right);
    }

    protected final int compareValue(ArrayStructure<Integer> array, int index, int comparedIndex, int value) {
        int existing = array.get(index);
        ExecutionEvents.emit(new SortComparedEvent(index, comparedIndex, existing, value));
        return compare(existing, value);
    }

    protected final void write(ArrayStructure<Integer> array, int index, int value) {
        array.set(index, value);
    }

    protected final void swap(ArrayStructure<Integer> array, int leftIndex, int rightIndex) {
        if (leftIndex == rightIndex) {
            return;
        }
        array.swap(leftIndex, rightIndex);
    }

    protected final void selectRange(int lowIndex, int highIndex) {
        ExecutionEvents.emit(new SortRangeSelectedEvent(lowIndex, highIndex));
    }

    protected final void selectPivot(int index, int value) {
        ExecutionEvents.emit(new SortPivotSelectedEvent(index, value));
    }

    protected final void settle(ArrayStructure<Integer> array, int index) {
        ExecutionEvents.emit(new SortElementSettledEvent(index, array.get(index)));
    }

    protected final void settleRange(ArrayStructure<Integer> array, int lowIndex, int highIndex) {
        for (int index = lowIndex; index <= highIndex; index++) {
            settle(array, index);
        }
    }
}
