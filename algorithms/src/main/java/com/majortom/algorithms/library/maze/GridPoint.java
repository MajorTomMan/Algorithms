package com.majortom.algorithms.library.maze;

/** Zero-based row/column coordinate in a UI-neutral grid. */
public record GridPoint(int row, int column) {

    public GridPoint {
        if (row < 0 || column < 0) {
            throw new IllegalArgumentException("grid coordinates must not be negative");
        }
    }
}
