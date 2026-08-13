package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.api.AlgorithmOutput;

import java.util.Objects;

/** Generated immutable maze. */
public record ArrayMazeGenerationOutput(GridMaze maze) implements AlgorithmOutput {

    public ArrayMazeGenerationOutput {
        Objects.requireNonNull(maze, "maze");
    }
}
