package com.majortom.algorithms.library.sort;

import com.majortom.algorithms.core.logging.Log;
import com.majortom.algorithms.library.basic.Array;
import com.majortom.algorithms.library.sort.model.IntegerSortInput;
import com.majortom.algorithms.library.sort.model.IntegerSortOutput;
import com.majortom.algorithms.library.structure.ArrayStructure;

import java.util.List;

/** Shared direct helpers for concrete integer sorting algorithms. */
public abstract class AbstractIntegerSort implements Sort<Integer> {

    public final IntegerSortOutput sort(IntegerSortInput input) {
        Log.i("SORT", getClass().getSimpleName() + " start, size=" + input.values().size());
        Array<Integer> array = new Array<>(input.values());
        sort(array);
        IntegerSortOutput output = new IntegerSortOutput(copy(array));
        Log.i("SORT", getClass().getSimpleName() + " completed");
        return output;
    }

    private List<Integer> copy(ArrayStructure<Integer> array) {
        List<Integer> values = new java.util.ArrayList<>(array.size());
        for (Integer value : array) {
            values.add(value);
        }
        return List.copyOf(values);
    }

    protected final int compareAt(ArrayStructure<Integer> array, int leftIndex, int rightIndex) {
        return compare(array.get(leftIndex), array.get(rightIndex));
    }

    protected final int compareValue(ArrayStructure<Integer> array, int index, int value) {
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
