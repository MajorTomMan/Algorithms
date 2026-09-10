package com.majortom.algorithms.algorithm.maze;



import com.majortom.algorithms.structure.maze.MazeDimensions;
import com.majortom.algorithms.structure.maze.GridMaze;
/** Domain contract for array-backed maze generation algorithms. */
public interface ArrayMazeGenerator {
    GridMaze generate(MazeDimensions dimensions, long seed);
}
