package com.majortom.algorithms.algorithm.array.sort.insertion;

import com.majortom.algorithms.algorithm.array.sort.AbstractIntegerSort;
import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.structure.array.ArrayStructure;

/** Insertion sort over an ArrayStructure<Integer>. */
@Algorithm(id = "insertion-sort", module = "array", type = Integer.class, structure = ArrayStructure.class)
public final class IntegerInsertionSort extends AbstractIntegerSort {

    @Override
    public int compare(Integer left, Integer right) {
        return Integer.compare(left, right);
    }

    @Override
    public void sort(ArrayStructure<Integer> array) {
        for (int insertionIndex = 1; insertionIndex < array.size(); insertionIndex++) {
            int insertionValue = array.get(insertionIndex);
            int scanIndex = insertionIndex - 1;
            while (scanIndex >= 0) {
                int existingValue = array.get(scanIndex);
                if (compareValue(array, scanIndex, insertionValue) <= 0) {
                    break;
                }
                write(array, scanIndex + 1, existingValue);
                scanIndex--;
            }
            write(array, scanIndex + 1, insertionValue);
        }
    }
}
