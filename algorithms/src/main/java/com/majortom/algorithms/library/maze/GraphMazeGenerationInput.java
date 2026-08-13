package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.api.AlgorithmInput;

/** Grid dimensions and seed for the graph-maze BFS spanning tree. */
public record GraphMazeGenerationInput(int rows, int columns, long seed) implements AlgorithmInput {

    public GraphMazeGenerationInput {
        if (rows < 1 || columns < 1) {
            throw new IllegalArgumentException("graph maze dimensions must be positive");
        }
        MazeDimensions.checkedCellCount(rows, columns);
    }
}
