package com.majortom.algorithms.library.sort;

/** Selection sort over immutable integer input. */
public final class IntegerSelectionSort extends AbstractIntegerSort {

    @Override
    protected void sort(IntegerSortSupport sort) throws InterruptedException {
        for (int destination = 0; destination < sort.size(); destination++) {
            sort.selectRange(destination, sort.size() - 1);
            int minimum = destination;
            for (int candidate = destination + 1; candidate < sort.size(); candidate++) {
                if (sort.compare(candidate, minimum) < 0) {
                    minimum = candidate;
                }
            }
            sort.swap(destination, minimum);
            sort.settle(destination);
        }
    }
}
