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
        ExecutionEvents.emit(new ArrayMazePathEvent.Initialized(input.maze(), input.start(), input.goal()));
        Map<GridPoint, GridPoint> previous = new HashMap<>();
        Set<GridPoint> discovered = new HashSet<>();
        int[] visitedCount = {0};
        discovered.add(input.start());
        ExecutionEvents.emit(new ArrayMazePathEvent.Discovered(input.start(), null));
        boolean found = visit(input.maze(), input.start(), null, input.goal(), discovered, previous, visitedCount);
        List<GridPoint> path = found ? ArrayMazeSupport.reconstruct(previous, input.start(), input.goal()) : List.of();
        ArrayMazeSupport.confirmPath(path);
        ExecutionEvents.emit(new ArrayMazePathEvent.Completed(path));
        return new ArrayMazePathOutput(path, visitedCount[0]);
    }

    private boolean visit(GridMaze maze, GridPoint current, GridPoint parent, GridPoint goal,
                          Set<GridPoint> discovered, Map<GridPoint, GridPoint> previous, int[] visitedCount) {
        ExecutionEvents.checkpoint();
        ExecutionEvents.emit(new ArrayMazePathEvent.Entered(current, parent));
        visitedCount[0]++;
        if (current.equals(goal)) return true;
        for (GridPoint neighbor : ArrayMazeSupport.neighbors(maze, current)) {
            if (!discovered.add(neighbor)) continue;
            previous.put(neighbor, current);
            ExecutionEvents.emit(new ArrayMazePathEvent.Discovered(neighbor, current));
            if (visit(maze, neighbor, current, goal, discovered, previous, visitedCount)) return true;
        }
        ExecutionEvents.emit(new ArrayMazePathEvent.DeadEndReached(current));
        ExecutionEvents.emit(new ArrayMazePathEvent.Backtracked(current, parent));
        return false;
    }
}
