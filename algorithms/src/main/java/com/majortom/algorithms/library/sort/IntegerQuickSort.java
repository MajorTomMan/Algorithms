package com.majortom.algorithms.library.sort;

import java.util.ArrayDeque;

/** In-place two-way quicksort with deterministic pivot selection. */
public final class IntegerQuickSort extends AbstractIntegerSort {

    @Override
    protected void sort(IntegerSortSupport sort) throws InterruptedException {
        quicksortIteratively(sort);
    }

    private void quicksortIteratively(IntegerSortSupport sort) throws InterruptedException {
        ArrayDeque<Range> pending = new ArrayDeque<>();
        pending.push(new Range(0, sort.size() - 1));
        while (!pending.isEmpty()) {
            Range range = pending.pop();
            if (range.low() >= range.high()) {
                if (range.low() >= 0 && range.low() < sort.size()) {
                    sort.settle(range.low());
                }
                continue;
            }
            sort.selectRange(range.low(), range.high());
            EqualRange equal = partitionThreeWay(sort, range.low(), range.high());
            sort.settleRange(equal.low(), equal.high());
            if (range.low() < equal.low() - 1) {
                pending.push(new Range(range.low(), equal.low() - 1));
            }
            if (equal.high() + 1 < range.high()) {
                pending.push(new Range(equal.high() + 1, range.high()));
            }
        }
    }

    private EqualRange partitionThreeWay(IntegerSortSupport sort, int low, int high)
            throws InterruptedException {
        int pivotSource = low + (high - low) / 2;
        int pivot = sort.valueAt(pivotSource);
        sort.selectPivot(pivotSource, pivot);
        int lower = low;
        int index = low;
        int upper = high;
        while (index <= upper) {
            int comparison = sort.compareValue(index, pivotSource, pivot);
            if (comparison < 0) {
                sort.swap(lower, index);
                lower++;
                index++;
            } else if (comparison > 0) {
                sort.swap(index, upper);
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
