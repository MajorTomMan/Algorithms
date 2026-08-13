package com.majortom.algorithms.library.sort.event;

/** Selects the inclusive range currently processed by the algorithm. */
public record SortRangeSelectedEvent(int lowIndex, int highIndex) implements IntegerSortEvent {

    public SortRangeSelectedEvent {
        if (lowIndex < 0 || highIndex < lowIndex) {
            throw new IllegalArgumentException("sort range must be non-negative and ordered");
        }
    }
}
