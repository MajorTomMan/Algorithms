package com.majortom.algorithms.library.maze;

/** Immutable validated maze dimensions shared by maze algorithms. */
public record MazeDimensions(int rows, int columns) {

    public static final int MAX_CELLS = 100_000;

    public MazeDimensions {
        if (rows < 1 || columns < 1) {
            throw new IllegalArgumentException("maze dimensions must be positive");
        }
        checkedCellCount(rows, columns);
    }

    public int cellCount() {
        return checkedCellCount(rows, columns);
    }

    static int checkedCellCount(int rows, int columns) {
        final int cells;
        try {
            cells = Math.multiplyExact(rows, columns);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("maze dimensions are too large", exception);
        }
        if (cells > MAX_CELLS) {
            throw new IllegalArgumentException("maze must contain at most " + MAX_CELLS + " cells");
        }
        return cells;
    }
}
