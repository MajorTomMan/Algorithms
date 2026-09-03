package com.majortom.algorithms.server.request;

import com.majortom.algorithms.library.maze.MazeDimensions;

/** HTTP request body for deterministic maze generation. */
public record MazeGenerationRequest(int rows, int columns, long seed) {
    public MazeDimensions dimensions() {
        return new MazeDimensions(rows, columns);
    }
}
