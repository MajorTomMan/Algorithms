package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.runtime.ExecutionEvents;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class ArrayMazeSupport {
    static final int[][] CELL_DIRECTIONS = {{-2, 0}, {0, 2}, {2, 0}, {0, -2}};
    static final int[][] PATH_DIRECTIONS = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};

    private ArrayMazeSupport() {
    }

    static GenerationState initialize(ArrayMazeGenerationInput input) {
        GridPoint entrance = new GridPoint(1, 1);
        GridPoint exit = new GridPoint(input.rows() - 2, input.columns() - 2);
        return new GenerationState(new boolean[MazeDimensions.checkedCellCount(input.rows(), input.columns())], entrance, exit);
    }

    static void open(ArrayMazeGenerationInput input, boolean[] open, GridPoint point) {
        int index = index(input.columns(), point);
        if (open[index]) {
            return;
        }
        ExecutionEvents.checkpoint();
        open[index] = true;
    }

    static GridMaze complete(ArrayMazeGenerationInput input, GenerationState state) {
        List<Boolean> cells = new ArrayList<>(state.open().length);
        for (boolean cell : state.open()) {
            cells.add(cell);
        }
        return new GridMaze(input.rows(), input.columns(), cells, state.entrance(), state.exit());
    }

    static List<int[]> shuffledCellDirections(Random random) {
        List<int[]> directions = new ArrayList<>(List.of(CELL_DIRECTIONS));
        Collections.shuffle(directions, random);
        return directions;
    }

    static boolean isInner(ArrayMazeGenerationInput input, int row, int column) {
        return row > 0 && row < input.rows() - 1 && column > 0 && column < input.columns() - 1;
    }

    static int index(int columns, GridPoint point) {
        return point.row() * columns + point.column();
    }

    static List<GridPoint> neighbors(GridMaze maze, GridPoint point) {
        List<GridPoint> neighbors = new ArrayList<>(4);
        for (int[] direction : PATH_DIRECTIONS) {
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

    static List<GridPoint> reconstruct(Map<GridPoint, GridPoint> previous, GridPoint start, GridPoint goal) {
        ArrayDeque<GridPoint> path = new ArrayDeque<>();
        GridPoint current = goal;
        path.addFirst(current);
        while (!current.equals(start)) {
            current = previous.get(current);
            if (current == null) {
                return List.of();
            }
            path.addFirst(current);
        }
        return List.copyOf(path);
    }

    record GenerationState(boolean[] open, GridPoint entrance, GridPoint exit) {
    }
}
