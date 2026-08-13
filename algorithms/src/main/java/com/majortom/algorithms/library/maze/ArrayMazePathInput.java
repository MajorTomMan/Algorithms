package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.api.AlgorithmInput;

import java.util.Objects;

/** Immutable maze and endpoints for a pathfinding run. */
public record ArrayMazePathInput(GridMaze maze, GridPoint start, GridPoint goal) implements AlgorithmInput {

    public ArrayMazePathInput {
        Objects.requireNonNull(maze, "maze");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(goal, "goal");
        if (!maze.isOpen(start) || !maze.isOpen(goal)) {
            throw new IllegalArgumentException("path endpoints must be open maze cells");
        }
    }
}
