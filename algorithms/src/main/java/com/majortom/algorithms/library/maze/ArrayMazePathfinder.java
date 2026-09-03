package com.majortom.algorithms.library.maze;

import java.util.List;

/** Domain contract for pathfinding over an immutable array maze. */
public interface ArrayMazePathfinder {
    List<GridPoint> findPath(GridMaze maze, GridPoint start, GridPoint goal);
}
