package com.majortom.algorithms.library.maze;

import java.util.ArrayDeque;
import java.util.Random;

/** Depth-first frontier perfect-maze generator. */
public final class DfsArrayMazeGenerator implements ArrayMazeGenerator {
    @Override
    public ArrayMazeGenerationOutput generate(ArrayMazeGenerationInput input) {
        Random random = new Random(input.seed());
        ArrayMazeSupport.GenerationState state = ArrayMazeSupport.initialize(input);
        ArrayDeque<GridPoint> frontier = new ArrayDeque<>();
        GridPoint start = new GridPoint(1, 1);
        ArrayMazeSupport.open(input, state.open(), start);
        frontier.add(start);
        while (!frontier.isEmpty()) {
            GridPoint current = frontier.removeLast();
            for (int[] direction : ArrayMazeSupport.shuffledCellDirections(random)) {
                int nextRow = current.row() + direction[0];
                int nextColumn = current.column() + direction[1];
                if (!ArrayMazeSupport.isInner(input, nextRow, nextColumn)) continue;
                GridPoint next = new GridPoint(nextRow, nextColumn);
                if (state.open()[ArrayMazeSupport.index(input.columns(), next)]) continue;
                GridPoint corridor = new GridPoint(current.row() + direction[0] / 2, current.column() + direction[1] / 2);
                ArrayMazeSupport.open(input, state.open(), corridor);
                ArrayMazeSupport.open(input, state.open(), next);
                frontier.addLast(next);
            }
        }
        return new ArrayMazeGenerationOutput(ArrayMazeSupport.complete(input, state));
    }
}
