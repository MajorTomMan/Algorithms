package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.runtime.ExecutionEvents;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;

/** A* pathfinder using Manhattan distance on the maze grid. */
public final class AStarArrayMazePathfinder implements ArrayMazePathfinder {
    @Override
    public ArrayMazePathOutput findPath(ArrayMazePathInput input) {
        ExecutionEvents.emit(new ArrayMazePathEvent.Initialized(input.maze(), input.start(), input.goal()));
        Map<GridPoint, GridPoint> previous = new HashMap<>();
        Map<GridPoint, Integer> distance = new HashMap<>();
        Set<GridPoint> discovered = new HashSet<>();
        PriorityQueue<GridPoint> frontier = new PriorityQueue<>(Comparator
                .comparingInt((GridPoint point) -> distance.getOrDefault(point, Integer.MAX_VALUE) + manhattan(point, input.goal()))
                .thenComparingInt(GridPoint::row).thenComparingInt(GridPoint::column));
        distance.put(input.start(), 0);
        discovered.add(input.start());
        frontier.add(input.start());
        ExecutionEvents.emit(new ArrayMazePathEvent.Discovered(input.start(), null));
        int visitedCount = 0;
        boolean found = false;
        while (!frontier.isEmpty()) {
            GridPoint current = frontier.remove();
            ExecutionEvents.checkpoint();
            ExecutionEvents.emit(new ArrayMazePathEvent.Entered(current, previous.get(current)));
            visitedCount++;
            if (current.equals(input.goal())) { found = true; break; }
            for (GridPoint neighbor : ArrayMazeSupport.neighbors(input.maze(), current)) {
                int candidate = distance.get(current) + 1;
                if (candidate >= distance.getOrDefault(neighbor, Integer.MAX_VALUE)) continue;
                distance.put(neighbor, candidate);
                previous.put(neighbor, current);
                frontier.remove(neighbor);
                frontier.add(neighbor);
                if (discovered.add(neighbor)) ExecutionEvents.emit(new ArrayMazePathEvent.Discovered(neighbor, current));
            }
        }
        List<GridPoint> path = found ? ArrayMazeSupport.reconstruct(previous, input.start(), input.goal()) : List.of();
        ArrayMazeSupport.confirmPath(path);
        ExecutionEvents.emit(new ArrayMazePathEvent.Completed(path));
        return new ArrayMazePathOutput(path, visitedCount);
    }

    private int manhattan(GridPoint left, GridPoint right) {
        return Math.abs(left.row() - right.row()) + Math.abs(left.column() - right.column());
    }
}
