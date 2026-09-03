package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.runtime.ExecutionEvents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Recursive depth-first pathfinder with explicit backtrack events. */
public final class DfsArrayMazePathfinder implements ArrayMazePathfinder {
    @Override
    public List<GridPoint> findPath(GridMaze maze, GridPoint start, GridPoint goal) {
        ArrayMazeSupport.requirePathEndpoints(maze, start, goal);
        Map<GridPoint, GridPoint> previous = new HashMap<>();
        Set<GridPoint> discovered = new HashSet<>();
        discovered.add(start);
        boolean found = visit(maze, start, goal, discovered, previous);
        List<GridPoint> path = found ? ArrayMazeSupport.reconstruct(previous, start, goal) : List.of();
        return path;
    }

    private boolean visit(GridMaze maze, GridPoint current, GridPoint goal,
                          Set<GridPoint> discovered, Map<GridPoint, GridPoint> previous) {
        ExecutionEvents.checkpoint();
        if (current.equals(goal)) return true;
        for (GridPoint neighbor : ArrayMazeSupport.neighbors(maze, current)) {
            if (!discovered.add(neighbor)) continue;
            previous.put(neighbor, current);
            if (visit(maze, neighbor, goal, discovered, previous)) return true;
        }
        return false;
    }
}
