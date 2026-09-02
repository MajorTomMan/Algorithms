package com.majortom.algorithms.library.sort;

import com.majortom.algorithms.library.structure.ArrayStructure;

/** Selection sort over an ArrayStructure<Integer>. */
public final class IntegerSelectionSort extends AbstractIntegerSort {

    @Override
    public int compare(Integer left, Integer right) {
        return Integer.compare(left, right);
    }

    @Override
    public void sort(ArrayStructure<Integer> array) {
        for (int destination = 0; destination < array.size(); destination++) {
            selectRange(destination, array.size() - 1);
            int minimum = destination;
            for (int candidate = destination + 1; candidate < array.size(); candidate++) {
                if (compareAt(array, candidate, minimum) < 0) {
                    minimum = candidate;
                }
            }
            swap(array, destination, minimum);
            settle(array, destination);
        }
    }
}
