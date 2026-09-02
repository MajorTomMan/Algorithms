package com.majortom.algorithms.library.maze;


/** Grid dimensions and seed for the graph-maze BFS spanning tree. */
public record GraphMazeGenerationInput(int rows, int columns, long seed) {

    public GraphMazeGenerationInput {
        if (rows < 1 || columns < 1) {
            throw new IllegalArgumentException("graph maze dimensions must be positive");
        }
        MazeDimensions.checkedCellCount(rows, columns);
    }
}
