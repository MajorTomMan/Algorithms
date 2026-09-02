package com.majortom.algorithms.library.sort.insertion;

import com.majortom.algorithms.library.sort.AbstractIntegerSort;
import com.majortom.algorithms.library.structure.ArrayStructure;

/** Insertion sort over an ArrayStructure<Integer>. */
public final class IntegerInsertionSort extends AbstractIntegerSort {

    @Override
    public int compare(Integer left, Integer right) {
        return Integer.compare(left, right);
    }

    @Override
    public void sort(ArrayStructure<Integer> array) {
        for (int insertionIndex = 1; insertionIndex < array.size(); insertionIndex++) {
            selectRange(0, insertionIndex);
            int insertionValue = array.get(insertionIndex);
            int scanIndex = insertionIndex - 1;
            while (scanIndex >= 0) {
                int existingValue = array.get(scanIndex);
                if (compareValue(array, scanIndex, insertionIndex, insertionValue) <= 0) {
                    break;
                }
                write(array, scanIndex + 1, existingValue);
                scanIndex--;
            }
            write(array, scanIndex + 1, insertionValue);
            settle(array, scanIndex + 1);
        }
    }
}
