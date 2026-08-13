package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.api.AlgorithmInput;

/** Reproducible dimensions and seed for an array maze generator. */
public record ArrayMazeGenerationInput(int rows, int columns, long seed) implements AlgorithmInput {

    public ArrayMazeGenerationInput {
        if (rows < 3 || columns < 3 || rows % 2 == 0 || columns % 2 == 0) {
            throw new IllegalArgumentException("maze dimensions must be odd and at least 3");
        }
        MazeDimensions.checkedCellCount(rows, columns);
    }
}
