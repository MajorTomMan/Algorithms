package com.majortom.algorithms.algorithm.array.sort.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.runtime.Observations;
import com.majortom.algorithms.structure.array.ArrayStructure;

/** Insertion sort POC proving Array<T> reuse for String values. */
@Algorithm(id = "insertion-sort", name = "Insertion Sort", type = String.class, structure = ArrayStructure.class)
public final class StringInsertionSort {
    private int compare(String left, String right) {
        return left.compareTo(right);
    }
    @AlgorithmEntry
    public void sort(ArrayStructure<String> array) {
        for (int insertionIndex = 1; insertionIndex < array.size(); insertionIndex++) {
            String insertionValue = array.get(insertionIndex);
            int scanIndex = insertionIndex - 1;
            while (scanIndex >= 0) {
                String existingValue = array.get(scanIndex);
                Observations.compared("array", scanIndex, insertionValue);
                if (compare(existingValue, insertionValue) <= 0) break;
                array.set(scanIndex + 1, existingValue);
                scanIndex--;
            }
            array.set(scanIndex + 1, insertionValue);
        }
    }
}
