package com.majortom.algorithms.library.maze;

/** Domain contract for graph-backed maze generation algorithms. */
public interface GraphMazeGenerator<T> {
    GraphMazeGenerationOutput generate(GraphMazeGenerationInput input);
}
