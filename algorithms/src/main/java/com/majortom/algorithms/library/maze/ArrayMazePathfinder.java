package com.majortom.algorithms.library.maze;

/** Domain contract for pathfinding over an immutable array maze. */
public interface ArrayMazePathfinder {
    ArrayMazePathOutput findPath(ArrayMazePathInput input);
}
