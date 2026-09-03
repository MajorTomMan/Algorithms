package com.majortom.algorithms.library.sort;

import com.majortom.algorithms.library.structure.ArrayStructure;

import java.util.ArrayDeque;

/** In-place three-way quicksort with deterministic pivot selection. */
public final class IntegerQuickSort extends AbstractIntegerSort {

    @Override
    public int compare(Integer left, Integer right) {
        return Integer.compare(left, right);
    }

    @Override
    public void sort(ArrayStructure<Integer> array) {
        ArrayDeque<Range> pending = new ArrayDeque<>();
        pending.push(new Range(0, array.size() - 1));
        while (!pending.isEmpty()) {
            Range range = pending.pop();
            if (range.low() >= range.high()) {
                if (range.low() >= 0 && range.low() < array.size()) {
                }
                continue;
            }
            EqualRange equal = partitionThreeWay(array, range.low(), range.high());
            if (range.low() < equal.low() - 1) {
                pending.push(new Range(range.low(), equal.low() - 1));
            }
            if (equal.high() + 1 < range.high()) {
                pending.push(new Range(equal.high() + 1, range.high()));
            }
        }
    }

    private EqualRange partitionThreeWay(ArrayStructure<Integer> array, int low, int high) {
        int pivotSource = low + (high - low) / 2;
        int pivot = array.get(pivotSource);
        int lower = low;
        int index = low;
        int upper = high;
        while (index <= upper) {
            int comparison = compareValue(array, index, pivot);
            if (comparison < 0) {
                swap(array, lower, index);
                lower++;
                index++;
            } else if (comparison > 0) {
                swap(array, index, upper);
                upper--;
            } else {
                index++;
            }
        }
        return new EqualRange(lower, upper);
    }

    private record Range(int low, int high) {
    }

    private record EqualRange(int low, int high) {
    }
}
