package com.majortom.algorithms.library.sort.event;

/** Identifies the pivot used for the current partition. */
public record SortPivotSelectedEvent(int index, int value) implements IntegerSortEvent {

    public SortPivotSelectedEvent {
        if (index < 0) {
            throw new IllegalArgumentException("pivot index must not be negative");
        }
    }
}
