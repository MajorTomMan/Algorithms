package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.api.Algorithm;
import com.majortom.algorithms.core.api.AlgorithmContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Reproducible BFS, DFS, and union-find perfect-maze generators. */
public final class ArrayMazeGenerator
        implements Algorithm<ArrayMazeGenerationInput, ArrayMazeGenerationOutput> {

    public enum Strategy {
        BFS,
        DFS,
        UNION_FIND
    }

    private static final int[][] DIRECTIONS = {{-2, 0}, {0, 2}, {2, 0}, {0, -2}};

    private final Strategy strategy;

    public ArrayMazeGenerator(Strategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    @Override
    public ArrayMazeGenerationOutput run(ArrayMazeGenerationInput input, AlgorithmContext context)
            throws InterruptedException {
        Random random = new Random(input.seed());
        boolean[] open = new boolean[MazeDimensions.checkedCellCount(input.rows(), input.columns())];
        GridPoint entrance = new GridPoint(1, 1);
        GridPoint exit = new GridPoint(input.rows() - 2, input.columns() - 2);
        context.emit(new ArrayMazeGenerationEvent.Initialized(
                input.rows(), input.columns(), entrance, exit));
        if (strategy == Strategy.UNION_FIND) {
            generateWithUnionFind(input, context, random, open);
        } else {
            generateWithFrontier(input, context, random, open);
        }
        GridMaze maze = toMaze(input, open, entrance, exit);
        context.emit(new ArrayMazeGenerationEvent.Completed(maze));
        return new ArrayMazeGenerationOutput(maze);
    }

    private void generateWithFrontier(
            ArrayMazeGenerationInput input,
            AlgorithmContext context,
            Random random,
            boolean[] open) throws InterruptedException {
        ArrayDeque<GridPoint> frontier = new ArrayDeque<>();
        GridPoint start = new GridPoint(1, 1);
        open(input, context, open, start);
        frontier.add(start);
        while (!frontier.isEmpty()) {
            GridPoint current;
            if (strategy == Strategy.BFS) {
                current = frontier.removeFirst();
            } else {
                current = frontier.removeLast();
            }
            List<int[]> directions = shuffledDirections(random);
            for (int[] direction : directions) {
                int nextRow = current.row() + direction[0];
                int nextColumn = current.column() + direction[1];
                if (!isInner(input, nextRow, nextColumn)) {
                    continue;
                }
                GridPoint next = new GridPoint(nextRow, nextColumn);
                if (open[index(input.columns(), next)]) {
                    continue;
                }
                GridPoint corridor = moved(current, direction[0] / 2, direction[1] / 2);
                open(input, context, open, corridor);
                open(input, context, open, next);
                frontier.addLast(next);
            }
        }
    }

    private void generateWithUnionFind(
            ArrayMazeGenerationInput input,
            AlgorithmContext context,
            Random random,
            boolean[] open) throws InterruptedException {
        List<GridPoint> cells = logicalCells(input);
        for (GridPoint cell : cells) {
            open(input, context, open, cell);
        }
        List<CellEdge> edges = new ArrayList<>();
        for (GridPoint cell : cells) {
            addEdgeIfInside(input, edges, cell, 0, 2);
            addEdgeIfInside(input, edges, cell, 2, 0);
        }
        Collections.shuffle(edges, random);
        DisjointSet sets = new DisjointSet(input.rows() * input.columns());
        for (CellEdge edge : edges) {
            int left = index(input.columns(), edge.left());
            int right = index(input.columns(), edge.right());
            if (sets.union(left, right)) {
                GridPoint corridor = new GridPoint(
                        (edge.left().row() + edge.right().row()) / 2,
                        (edge.left().column() + edge.right().column()) / 2);
                open(input, context, open, corridor);
            }
        }
    }

    private void open(
            ArrayMazeGenerationInput input,
            AlgorithmContext context,
            boolean[] open,
            GridPoint point) throws InterruptedException {
        int index = index(input.columns(), point);
        if (open[index]) {
            return;
        }
        context.checkpoint();
        open[index] = true;
        context.emit(new ArrayMazeGenerationEvent.CellOpened(point));
    }

    private List<GridPoint> logicalCells(ArrayMazeGenerationInput input) {
        List<GridPoint> cells = new ArrayList<>();
        for (int row = 1; row < input.rows(); row += 2) {
            for (int column = 1; column < input.columns(); column += 2) {
                cells.add(new GridPoint(row, column));
            }
        }
        return cells;
    }

    private void addEdgeIfInside(
            ArrayMazeGenerationInput input,
            List<CellEdge> edges,
            GridPoint left,
            int rowDelta,
            int columnDelta) {
        int rightRow = left.row() + rowDelta;
        int rightColumn = left.column() + columnDelta;
        if (isInner(input, rightRow, rightColumn)) {
            edges.add(new CellEdge(left, new GridPoint(rightRow, rightColumn)));
        }
    }

    private List<int[]> shuffledDirections(Random random) {
        List<int[]> directions = new ArrayList<>(List.of(DIRECTIONS));
        Collections.shuffle(directions, random);
        return directions;
    }

    private GridMaze toMaze(
            ArrayMazeGenerationInput input,
            boolean[] open,
            GridPoint entrance,
            GridPoint exit) {
        List<Boolean> cells = new ArrayList<>(open.length);
        for (boolean cell : open) {
            cells.add(cell);
        }
        return new GridMaze(input.rows(), input.columns(), cells, entrance, exit);
    }

    private GridPoint moved(GridPoint point, int rowDelta, int columnDelta) {
        return new GridPoint(point.row() + rowDelta, point.column() + columnDelta);
    }

    private boolean isInner(ArrayMazeGenerationInput input, int row, int column) {
        return row > 0 && row < input.rows() - 1
                && column > 0 && column < input.columns() - 1;
    }

    private int index(int columns, GridPoint point) {
        return point.row() * columns + point.column();
    }

    private record CellEdge(GridPoint left, GridPoint right) {
    }

    private static final class DisjointSet {
        private final int[] parent;
        private final byte[] rank;

        private DisjointSet(int size) {
            parent = new int[size];
            rank = new byte[size];
            for (int index = 0; index < size; index++) {
                parent[index] = index;
            }
        }

        private boolean union(int left, int right) {
            int leftRoot = find(left);
            int rightRoot = find(right);
            if (leftRoot == rightRoot) {
                return false;
            }
            if (rank[leftRoot] < rank[rightRoot]) {
                parent[leftRoot] = rightRoot;
            } else if (rank[leftRoot] > rank[rightRoot]) {
                parent[rightRoot] = leftRoot;
            } else {
                parent[rightRoot] = leftRoot;
                rank[leftRoot]++;
            }
            return true;
        }

        private int find(int value) {
            int root = value;
            while (parent[root] != root) {
                root = parent[root];
            }
            int current = value;
            while (parent[current] != current) {
                int next = parent[current];
                parent[current] = root;
                current = next;
            }
            return root;
        }
    }
}
