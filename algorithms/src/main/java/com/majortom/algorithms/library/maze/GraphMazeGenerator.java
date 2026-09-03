package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.snapshot.GraphSnapshot;

/** Domain contract for graph-backed maze generation algorithms. */
public interface GraphMazeGenerator<T> {
    GraphSnapshot<T> generate(MazeDimensions dimensions, long seed);
}
