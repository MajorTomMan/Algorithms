package com.majortom.algorithms.library.maze;

/** Central resource and overflow guard for in-memory maze values. */
final class MazeDimensions {

    static final int MAX_CELLS = 100_000;

    private MazeDimensions() {
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
