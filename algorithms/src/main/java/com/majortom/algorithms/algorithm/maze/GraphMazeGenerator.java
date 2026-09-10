package com.majortom.algorithms.algorithm.maze;


import com.majortom.algorithms.structure.maze.MazeDimensions;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;

/** Domain contract for graph-backed maze generation algorithms. */
public interface GraphMazeGenerator<T> {
    GraphSnapshot<T> generate(MazeDimensions dimensions, long seed);
}
