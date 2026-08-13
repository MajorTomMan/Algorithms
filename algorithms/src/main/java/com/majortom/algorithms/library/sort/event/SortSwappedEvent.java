package com.majortom.algorithms.library.sort.event;

/** One atomic exchange, carrying the values that exist after the exchange. */
public record SortSwappedEvent(
        int leftIndex,
        int rightIndex,
        int leftValue,
        int rightValue) implements IntegerSortEvent {

    public SortSwappedEvent {
        if (leftIndex < 0 || rightIndex < 0) {
            throw new IllegalArgumentException("swap indexes must not be negative");
        }
        if (leftIndex == rightIndex) {
            throw new IllegalArgumentException("swap indexes must be different");
        }
    }
}
