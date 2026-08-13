package com.majortom.algorithms.library.sort;

import com.majortom.algorithms.core.api.AlgorithmContext;
import com.majortom.algorithms.library.sort.event.SortComparedEvent;
import com.majortom.algorithms.library.sort.event.SortCompletedEvent;
import com.majortom.algorithms.library.sort.event.SortElementSettledEvent;
import com.majortom.algorithms.library.sort.event.SortInitializedEvent;
import com.majortom.algorithms.library.sort.event.SortPivotSelectedEvent;
import com.majortom.algorithms.library.sort.event.SortRangeSelectedEvent;
import com.majortom.algorithms.library.sort.event.SortSwappedEvent;
import com.majortom.algorithms.library.sort.event.SortWrittenEvent;

import java.util.ArrayList;
import java.util.List;

/** Shared event-producing operations for the integer sorting providers. */
public final class IntegerSortSupport {

    private final List<Integer> values;
    private final AlgorithmContext context;

    public IntegerSortSupport(List<Integer> input, AlgorithmContext context) {
        values = new ArrayList<>(input);
        this.context = context;
    }

    public void initialize() {
        context.emit(new SortInitializedEvent(values));
    }

    public int size() {
        return values.size();
    }

    public int valueAt(int index) {
        return values.get(index);
    }

    public int compare(int leftIndex, int rightIndex) throws InterruptedException {
        context.checkpoint();
        int left = values.get(leftIndex);
        int right = values.get(rightIndex);
        context.emit(new SortComparedEvent(leftIndex, rightIndex, left, right));
        return Integer.compare(left, right);
    }

    public int compareValue(int index, int comparedIndex, int value) throws InterruptedException {
        context.checkpoint();
        int existing = values.get(index);
        context.emit(new SortComparedEvent(index, comparedIndex, existing, value));
        return Integer.compare(existing, value);
    }

    public void write(int index, int value) throws InterruptedException {
        context.checkpoint();
        values.set(index, value);
        context.emit(new SortWrittenEvent(index, value));
    }

    public void swap(int leftIndex, int rightIndex) throws InterruptedException {
        if (leftIndex == rightIndex) {
            return;
        }
        int left = values.get(leftIndex);
        int right = values.get(rightIndex);
        context.checkpoint();
        values.set(leftIndex, right);
        values.set(rightIndex, left);
        context.emit(new SortSwappedEvent(leftIndex, rightIndex, right, left));
    }

    public void selectRange(int lowIndex, int highIndex) throws InterruptedException {
        context.checkpoint();
        context.emit(new SortRangeSelectedEvent(lowIndex, highIndex));
    }

    public void selectPivot(int index, int value) throws InterruptedException {
        context.checkpoint();
        context.emit(new SortPivotSelectedEvent(index, value));
    }

    public void settle(int index) throws InterruptedException {
        context.checkpoint();
        context.emit(new SortElementSettledEvent(index, values.get(index)));
    }

    public void settleRange(int lowIndex, int highIndex) throws InterruptedException {
        for (int index = lowIndex; index <= highIndex; index++) {
            settle(index);
        }
    }

    public List<Integer> complete() {
        List<Integer> result = List.copyOf(values);
        context.emit(new SortCompletedEvent(result));
        return result;
    }
}
