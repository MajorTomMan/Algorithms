package com.majortom.algorithms.library.sort.insertion;

import com.majortom.algorithms.library.sort.AbstractIntegerSort;
import com.majortom.algorithms.library.sort.IntegerSortSupport;

/** Insertion sort over immutable integer input. */
public final class IntegerInsertionSort extends AbstractIntegerSort {

    @Override
    protected void sort(IntegerSortSupport sort) throws InterruptedException {
        for (int insertionIndex = 1; insertionIndex < sort.size(); insertionIndex++) {
            sort.selectRange(0, insertionIndex);
            int insertionValue = sort.valueAt(insertionIndex);
            int scanIndex = insertionIndex - 1;

            while (scanIndex >= 0) {
                int existingValue = sort.valueAt(scanIndex);
                if (sort.compareValue(scanIndex, insertionIndex, insertionValue) <= 0) {
                    break;
                }
                sort.write(scanIndex + 1, existingValue);
                scanIndex--;
            }

            sort.write(scanIndex + 1, insertionValue);
            sort.settle(scanIndex + 1);
        }
    }
}
