package com.majortom.algorithms.library.sort.event;

/** Describes one array write and is sufficient to advance a replay projection. */
public record SortWrittenEvent(int index, int value) implements IntegerSortEvent {

    public SortWrittenEvent {
        if (index < 0) {
            throw new IllegalArgumentException("write index must not be negative");
        }
    }
}
