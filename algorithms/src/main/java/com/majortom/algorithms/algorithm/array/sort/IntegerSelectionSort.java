package com.majortom.algorithms.algorithm.array.sort;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.structure.array.ArrayStructure;

/** Selection sort over an ArrayStructure<Integer>. */
@Algorithm(id = "selection-sort", type = Integer.class, structure = ArrayStructure.class)
public final class IntegerSelectionSort extends AbstractIntegerSort {

    @Override
    public int compare(Integer left, Integer right) {
        return Integer.compare(left, right);
    }

    @Override
    public void sort(ArrayStructure<Integer> array) {
        for (int destination = 0; destination < array.size(); destination++) {
            int minimum = destination;
            for (int candidate = destination + 1; candidate < array.size(); candidate++) {
                if (compareAt(array, candidate, minimum) < 0) {
                    minimum = candidate;
                }
            }
            swap(array, destination, minimum);
        }
    }
}
