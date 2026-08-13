package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.api.Algorithm;
import com.majortom.algorithms.core.api.AlgorithmContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/** BFS, DFS, and A* search over immutable array mazes. */
public final class ArrayMazePathfinder implements Algorithm<ArrayMazePathInput, ArrayMazePathOutput> {

    public enum Strategy {
        BFS,
        DFS,
        ASTAR
    }

    private static final int[][] DIRECTIONS = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
    private final Strategy strategy;

    public ArrayMazePathfinder(Strategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    @Override
    public ArrayMazePathOutput run(ArrayMazePathInput input, AlgorithmContext context)
            throws InterruptedException {
        context.emit(new ArrayMazePathEvent.Initialized(input.maze(), input.start(), input.goal()));
        if (strategy == Strategy.DFS) {
            return depthFirst(input, context);
        }
        return breadthFirstOrAStar(input, context);
    }

    private ArrayMazePathOutput breadthFirstOrAStar(
            ArrayMazePathInput input,
            AlgorithmContext context) throws InterruptedException {
        Map<GridPoint, GridPoint> previous = new HashMap<>();
        Map<GridPoint, Integer> distance = new HashMap<>();
        Set<GridPoint> discovered = new HashSet<>();
        Queue<GridPoint> frontier = createFrontier(input.goal(), distance);
        frontier.add(input.start());
        discovered.add(input.start());
        distance.put(input.start(), 0);
        context.emit(new ArrayMazePathEvent.Discovered(input.start(), null));
        int visitedCount = 0;
        boolean found = false;
        while (!frontier.isEmpty()) {
            GridPoint current = remove(frontier);
            context.checkpoint();
            context.emit(new ArrayMazePathEvent.Entered(current, previous.get(current)));
            visitedCount++;
            if (current.equals(input.goal())) {
                found = true;
                break;
            }
            for (GridPoint neighbor : neighbors(input.maze(), current)) {
                int candidateDistance = distance.get(current) + 1;
                if (strategy == Strategy.ASTAR && candidateDistance < distance.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    distance.put(neighbor, candidateDistance);
                    previous.put(neighbor, current);
                    frontier.remove(neighbor);
                    frontier.add(neighbor);
                    discovered.add(neighbor);
                    context.emit(new ArrayMazePathEvent.Discovered(neighbor, current));
                } else if (strategy != Strategy.ASTAR && discovered.add(neighbor)) {
                    distance.put(neighbor, candidateDistance);
                    previous.put(neighbor, current);
                    frontier.add(neighbor);
                    context.emit(new ArrayMazePathEvent.Discovered(neighbor, current));
                }
            }
        }
        List<GridPoint> path = List.of();
        if (found) {
            path = reconstruct(previous, input.start(), input.goal());
        }
        confirmPath(path, context);
        context.emit(new ArrayMazePathEvent.Completed(path));
        return new ArrayMazePathOutput(path, visitedCount);
    }

    private ArrayMazePathOutput depthFirst(
            ArrayMazePathInput input,
            AlgorithmContext context) throws InterruptedException {
        Map<GridPoint, GridPoint> previous = new HashMap<>();
        Set<GridPoint> discovered = new HashSet<>();
        int[] visitedCount = {0};
        discovered.add(input.start());
        context.emit(new ArrayMazePathEvent.Discovered(input.start(), null));
        boolean found = visitDepthFirst(
                input.maze(), input.start(), null, input.goal(), discovered, previous,
                visitedCount, context);
        List<GridPoint> path = List.of();
        if (found) {
            path = reconstruct(previous, input.start(), input.goal());
        }
        confirmPath(path, context);
        context.emit(new ArrayMazePathEvent.Completed(path));
        return new ArrayMazePathOutput(path, visitedCount[0]);
    }

    private boolean visitDepthFirst(
            GridMaze maze,
            GridPoint current,
            GridPoint parent,
            GridPoint goal,
            Set<GridPoint> discovered,
            Map<GridPoint, GridPoint> previous,
            int[] visitedCount,
            AlgorithmContext context) throws InterruptedException {
        context.checkpoint();
        context.emit(new ArrayMazePathEvent.Entered(current, parent));
        visitedCount[0]++;
        if (current.equals(goal)) {
            return true;
        }
        boolean advanced = false;
        for (GridPoint neighbor : neighbors(maze, current)) {
            if (!discovered.add(neighbor)) {
                continue;
            }
            advanced = true;
            previous.put(neighbor, current);
            context.emit(new ArrayMazePathEvent.Discovered(neighbor, current));
            if (visitDepthFirst(
                    maze, neighbor, current, goal, discovered, previous, visitedCount, context)) {
                return true;
            }
        }
        context.emit(new ArrayMazePathEvent.DeadEndReached(current));
        context.emit(new ArrayMazePathEvent.Backtracked(current, parent));
        return false;
    }

    private void confirmPath(List<GridPoint> path, AlgorithmContext context)
            throws InterruptedException {
        for (int index = 0; index < path.size(); index++) {
            context.checkpoint();
            context.emit(new ArrayMazePathEvent.PathConfirmed(path.get(index), index, path.size()));
        }
    }

    private Queue<GridPoint> createFrontier(GridPoint goal, Map<GridPoint, Integer> distance) {
        if (strategy == Strategy.ASTAR) {
            return new PriorityQueue<>(Comparator
                    .comparingInt((GridPoint point) -> distance.getOrDefault(point, Integer.MAX_VALUE)
                            + manhattan(point, goal))
                    .thenComparingInt(GridPoint::row)
                    .thenComparingInt(GridPoint::column));
        }
        return new ArrayDeque<>();
    }

    private GridPoint remove(Queue<GridPoint> frontier) {
        if (strategy == Strategy.DFS) {
            return ((ArrayDeque<GridPoint>) frontier).removeLast();
        }
        return frontier.remove();
    }

    private List<GridPoint> neighbors(GridMaze maze, GridPoint point) {
        List<GridPoint> neighbors = new ArrayList<>(4);
        for (int[] direction : DIRECTIONS) {
            int row = point.row() + direction[0];
            int column = point.column() + direction[1];
            if (row < 0 || column < 0 || row >= maze.rows() || column >= maze.columns()) {
                continue;
            }
            GridPoint neighbor = new GridPoint(row, column);
            if (maze.isOpen(neighbor)) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    private List<GridPoint> reconstruct(
            Map<GridPoint, GridPoint> previous,
            GridPoint start,
            GridPoint goal) {
        ArrayDeque<GridPoint> path = new ArrayDeque<>();
        GridPoint current = goal;
        path.addFirst(current);
        while (!current.equals(start)) {
            current = previous.get(current);
            path.addFirst(current);
        }
        return List.copyOf(path);
    }

    private int manhattan(GridPoint left, GridPoint right) {
        return Math.abs(left.row() - right.row()) + Math.abs(left.column() - right.column());
    }
}
