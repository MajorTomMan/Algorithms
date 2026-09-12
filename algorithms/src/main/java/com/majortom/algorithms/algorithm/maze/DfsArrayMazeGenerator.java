package com.majortom.algorithms.algorithm.maze;




import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.structure.maze.MazeDimensions;
import com.majortom.algorithms.structure.maze.GridPoint;
import com.majortom.algorithms.structure.maze.GridMaze;
import com.majortom.algorithms.core.runtime.Observations;

import java.util.Random;

/** Recursive-backtracker perfect-maze generator. */
@Algorithm(id = "maze-generator-dfs", module = "maze", type = Boolean.class)
public final class DfsArrayMazeGenerator implements ArrayMazeGenerator {
    @Override
    public GridMaze generate(MazeDimensions dimensions, long seed) {
        Random random = new Random(seed);
        ArrayMazeSupport.GenerationState state = ArrayMazeSupport.initialize(dimensions);
        GridPoint start = new GridPoint(1, 1);
        ArrayMazeSupport.open(dimensions, state.open(), start);
        carve(dimensions, state.open(), start, random);
        return ArrayMazeSupport.complete(dimensions, state);
    }

    private void carve(MazeDimensions dimensions, boolean[] open, GridPoint current, Random random) {
        for (int[] direction : ArrayMazeSupport.shuffledCellDirections(random)) {
            int nextRow = current.row() + direction[0];
            int nextColumn = current.column() + direction[1];
            if (!ArrayMazeSupport.isInner(dimensions, nextRow, nextColumn)) {
                continue;
            }
            GridPoint next = new GridPoint(nextRow, nextColumn);
            Observations.examined(current.row(), current.column(), nextRow, nextColumn);
            if (open[ArrayMazeSupport.index(dimensions.columns(), next)]) {
                continue;
            }
            GridPoint corridor = new GridPoint(
                    current.row() + direction[0] / 2,
                    current.column() + direction[1] / 2);
            ArrayMazeSupport.open(dimensions, open, corridor);
            ArrayMazeSupport.open(dimensions, open, next);
            carve(dimensions, open, next, random);
            Observations.backtracked(current.row(), current.column());
        }
    }
}
