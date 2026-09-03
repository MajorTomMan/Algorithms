package com.majortom.algorithms.library.maze;

import java.util.ArrayDeque;
import java.util.Random;

/** Breadth-first frontier perfect-maze generator. */
public final class BfsArrayMazeGenerator implements ArrayMazeGenerator {
    @Override
    public GridMaze generate(MazeDimensions dimensions, long seed) {
        Random random = new Random(seed);
        ArrayMazeSupport.GenerationState state = ArrayMazeSupport.initialize(dimensions);
        ArrayDeque<GridPoint> frontier = new ArrayDeque<>();
        GridPoint start = new GridPoint(1, 1);
        ArrayMazeSupport.open(dimensions, state.open(), start);
        frontier.add(start);
        while (!frontier.isEmpty()) {
            GridPoint current = frontier.removeFirst();
            for (int[] direction : ArrayMazeSupport.shuffledCellDirections(random)) {
                int nextRow = current.row() + direction[0];
                int nextColumn = current.column() + direction[1];
                if (!ArrayMazeSupport.isInner(dimensions, nextRow, nextColumn)) continue;
                GridPoint next = new GridPoint(nextRow, nextColumn);
                if (state.open()[ArrayMazeSupport.index(dimensions.columns(), next)]) continue;
                GridPoint corridor = new GridPoint(current.row() + direction[0] / 2, current.column() + direction[1] / 2);
                ArrayMazeSupport.open(dimensions, state.open(), corridor);
                ArrayMazeSupport.open(dimensions, state.open(), next);
                frontier.addLast(next);
            }
        }
        return ArrayMazeSupport.complete(dimensions, state);
    }
}
