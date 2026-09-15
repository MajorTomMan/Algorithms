package com.majortom.algorithms.algorithm.maze.generator.impl;

import com.majortom.algorithms.structure.maze.MazeStructure;
import com.majortom.algorithms.algorithm.maze.MazeAlgorithm;
import com.majortom.algorithms.algorithm.maze.MazeModel;
import com.majortom.algorithms.algorithm.maze.MazeRole;
import com.majortom.algorithms.algorithm.maze.ArrayMazeSupport;
import com.majortom.algorithms.algorithm.maze.ArrayMazeSupport.GenerationState;
import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.structure.maze.MazeDimensions;
import com.majortom.algorithms.structure.maze.GridPoint;
import com.majortom.algorithms.structure.maze.GridMaze;
import java.util.ArrayDeque;
import java.util.Random;

/** Breadth-first frontier perfect-maze generator. */
@Algorithm(id = "maze-generator-bfs", name = "随机广度优先", type = Boolean.class, structure = MazeStructure.class)
@MazeAlgorithm(role = MazeRole.GENERATOR, model = MazeModel.ARRAY)
public final class BfsArrayMazeGenerator {
    @AlgorithmEntry
    public GridMaze generate(MazeStructure maze) {
        MazeDimensions dimensions = maze.dimensions();
        Random random = new Random();
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
