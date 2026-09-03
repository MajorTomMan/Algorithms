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
    public ArrayMazePathOutput findPath(ArrayMazePathInput input) {
        Map<GridPoint, GridPoint> previous = new HashMap<>();
        Set<GridPoint> discovered = new HashSet<>();
        int[] visitedCount = {0};
        discovered.add(input.start());
        boolean found = visit(input.maze(), input.start(), null, input.goal(), discovered, previous, visitedCount);
        List<GridPoint> path = found ? ArrayMazeSupport.reconstruct(previous, input.start(), input.goal()) : List.of();
        return new ArrayMazePathOutput(path, visitedCount[0]);
    }

    private boolean visit(GridMaze maze, GridPoint current, GridPoint parent, GridPoint goal,
                          Set<GridPoint> discovered, Map<GridPoint, GridPoint> previous, int[] visitedCount) {
        ExecutionEvents.checkpoint();
        visitedCount[0]++;
        if (current.equals(goal)) return true;
        for (GridPoint neighbor : ArrayMazeSupport.neighbors(maze, current)) {
            if (!discovered.add(neighbor)) continue;
            previous.put(neighbor, current);
            if (visit(maze, neighbor, current, goal, discovered, previous, visitedCount)) return true;
        }
        return false;
    }
}
