package com.majortom.algorithms.server.request;

import com.majortom.algorithms.library.maze.GridMaze;
import com.majortom.algorithms.library.maze.GridPoint;

import java.util.Objects;

/** HTTP request body for maze pathfinding. */
public record MazePathRequest(GridMaze maze, GridPoint start, GridPoint goal) {
    public MazePathRequest {
        Objects.requireNonNull(maze, "maze");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(goal, "goal");
    }
}
