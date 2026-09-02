package com.majortom.algorithms.library.maze;


import java.util.Objects;

/** Generated immutable maze. */
public record ArrayMazeGenerationOutput(GridMaze maze) {

    public ArrayMazeGenerationOutput {
        Objects.requireNonNull(maze, "maze");
    }
}
