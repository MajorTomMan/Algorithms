package com.majortom.algorithms.library.maze;

/** Domain contract for array-backed maze generation algorithms. */
public interface ArrayMazeGenerator {
    ArrayMazeGenerationOutput generate(ArrayMazeGenerationInput input);
}
