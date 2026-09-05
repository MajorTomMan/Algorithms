package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.event.observation.ObservationEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvents;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

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
                .thenComparingInt(GridPoint::row)
                .thenComparingInt(GridPoint::column));
        distance.put(start, 0);
        discovered.add(start);
        frontier.add(start);
        boolean found = false;
        while (!frontier.isEmpty()) {
            GridPoint current = frontier.remove();
            ExecutionEvents.observe(new ObservationEvent.Visited(ref(current)));
            if (current.equals(goal)) {
                found = true;
                break;
            }
            for (GridPoint neighbor : ArrayMazeSupport.neighbors(maze, current)) {
                ExecutionEvents.observe(new ObservationEvent.Examined(ref(current), ref(neighbor)));
                int candidate = distance.get(current) + 1;
                if (candidate >= distance.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    continue;
                }
                discovered.add(neighbor);
                distance.put(neighbor, candidate);
                previous.put(neighbor, current);
                frontier.remove(neighbor);
                frontier.add(neighbor);
            }
        }
        if (!found) return List.of();
        List<GridPoint> path = ArrayMazeSupport.reconstruct(previous, start, goal);
        ExecutionEvents.observe(new ObservationEvent.PathFound(
                path.stream().map(AStarArrayMazePathfinder::ref).map(ObservationEvent.Reference.class::cast).toList()));
        return path;
    }

    private static ObservationEvent.CoordinateRef ref(GridPoint point) {
        return new ObservationEvent.CoordinateRef(point.row(), point.column());
    }

    private int manhattan(GridPoint left, GridPoint right) {
        return Math.abs(left.row() - right.row()) + Math.abs(left.column() - right.column());
    }
}
