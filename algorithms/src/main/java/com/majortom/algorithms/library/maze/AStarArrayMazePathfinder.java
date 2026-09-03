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
    public List<GridPoint> findPath(GridMaze maze, GridPoint start, GridPoint goal) {
        ArrayMazeSupport.requirePathEndpoints(maze, start, goal);
        Map<GridPoint, GridPoint> previous = new HashMap<>();
        Map<GridPoint, Integer> distance = new HashMap<>();
        Set<GridPoint> discovered = new HashSet<>();
        PriorityQueue<GridPoint> frontier = new PriorityQueue<>(Comparator
                .comparingInt((GridPoint point) -> distance.getOrDefault(point, Integer.MAX_VALUE) + manhattan(point, goal))
                .thenComparingInt(GridPoint::row).thenComparingInt(GridPoint::column));
        distance.put(start, 0);
        discovered.add(start);
        frontier.add(start);
        boolean found = false;
        while (!frontier.isEmpty()) {
            GridPoint current = frontier.remove();
            ExecutionEvents.checkpoint();
            if (current.equals(goal)) { found = true; break; }
            for (GridPoint neighbor : ArrayMazeSupport.neighbors(maze, current)) {
                int candidate = distance.get(current) + 1;
                if (candidate >= distance.getOrDefault(neighbor, Integer.MAX_VALUE)) continue;
                distance.put(neighbor, candidate);
                previous.put(neighbor, current);
                frontier.remove(neighbor);
                frontier.add(neighbor);
            }
        }
        List<GridPoint> path = found ? ArrayMazeSupport.reconstruct(previous, start, goal) : List.of();
        return path;
    }

    private int manhattan(GridPoint left, GridPoint right) {
        return Math.abs(left.row() - right.row()) + Math.abs(left.column() - right.column());
    }
}
