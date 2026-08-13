package com.majortom.algorithms.library.sort.event;

/** Describes one indexed comparison without changing replay state. */
public record SortComparedEvent(
        int existingIndex,
        int insertionIndex,
        int existingValue,
        int insertionValue) implements IntegerSortEvent {

    public SortComparedEvent {
        if (existingIndex < 0 || insertionIndex < 0) {
            throw new IllegalArgumentException("comparison indexes must not be negative");
        }
    }

    public int leftIndex() {
        return existingIndex;
    }

    public int rightIndex() {
        return insertionIndex;
    }

    public int leftValue() {
        return existingValue;
    }

    public int rightValue() {
        return insertionValue;
    }
}
