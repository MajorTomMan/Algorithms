package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.runtime.Observations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Recursive depth-first pathfinder with factual visit/examine/backtrack observations. */
public final class DfsArrayMazePathfinder implements ArrayMazePathfinder {
    @Override
    public List<GridPoint> findPath(GridMaze maze, GridPoint start, GridPoint goal) {
        ArrayMazeSupport.requirePathEndpoints(maze, start, goal);
        Map<GridPoint, GridPoint> previous = new HashMap<>();
        Set<GridPoint> discovered = new HashSet<>();
        discovered.add(start);
        boolean found = visit(maze, start, goal, discovered, previous);
        if (!found) return List.of();
        List<GridPoint> path = ArrayMazeSupport.reconstruct(previous, start, goal);
        Observations.pathFound(path, GridPoint::row, GridPoint::column);
        return path;
    }

    private boolean visit(
            GridMaze maze,
            GridPoint current,
            GridPoint goal,
            Set<GridPoint> discovered,
            Map<GridPoint, GridPoint> previous) {
        Observations.visited(current.row(), current.column());
        if (current.equals(goal)) {
            return true;
        }
        for (GridPoint neighbor : ArrayMazeSupport.neighbors(maze, current)) {
            Observations.examined(current.row(), current.column(), neighbor.row(), neighbor.column());
            if (!discovered.add(neighbor)) {
                continue;
            }
            previous.put(neighbor, current);
            if (visit(maze, neighbor, goal, discovered, previous)) {
                return true;
            }
        }
        Observations.backtracked(current.row(), current.column());
        return false;
    }
}
