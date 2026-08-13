package com.majortom.algorithms.library.sort.event;

/** Marks an index whose value is in its final sorted position. */
public record SortElementSettledEvent(int index, int value) implements IntegerSortEvent {

    public SortElementSettledEvent {
        if (index < 0) {
            throw new IllegalArgumentException("settled index must not be negative");
        }
    }
}
