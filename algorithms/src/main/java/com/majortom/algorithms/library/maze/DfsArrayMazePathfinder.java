package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.event.observation.ObservationEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvents;

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
        ExecutionEvents.observe(new ObservationEvent.PathFound(
                path.stream().map(DfsArrayMazePathfinder::ref).map(ObservationEvent.Reference.class::cast).toList()));
        return path;
    }

    private boolean visit(
            GridMaze maze,
            GridPoint current,
            GridPoint goal,
            Set<GridPoint> discovered,
            Map<GridPoint, GridPoint> previous) {
        ExecutionEvents.observe(new ObservationEvent.Visited(ref(current)));
        if (current.equals(goal)) {
            return true;
        }
        for (GridPoint neighbor : ArrayMazeSupport.neighbors(maze, current)) {
            ExecutionEvents.observe(new ObservationEvent.Examined(ref(current), ref(neighbor)));
            if (!discovered.add(neighbor)) {
                continue;
            }
            previous.put(neighbor, current);
            if (visit(maze, neighbor, goal, discovered, previous)) {
                return true;
            }
        }
        ExecutionEvents.observe(new ObservationEvent.Backtracked(ref(current)));
        return false;
    }

    private static ObservationEvent.CoordinateRef ref(GridPoint point) {
        return new ObservationEvent.CoordinateRef(point.row(), point.column());
    }
}
