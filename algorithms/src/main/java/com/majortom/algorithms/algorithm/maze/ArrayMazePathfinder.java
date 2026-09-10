package com.majortom.algorithms.algorithm.maze;



import com.majortom.algorithms.structure.maze.GridPoint;
import com.majortom.algorithms.structure.maze.GridMaze;
import java.util.List;

/** Domain contract for pathfinding over an immutable array maze. */
public interface ArrayMazePathfinder {
    List<GridPoint> findPath(GridMaze maze, GridPoint start, GridPoint goal);
}
