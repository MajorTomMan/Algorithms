package com.majortom.algorithms.core.maze.strategies;

import com.majortom.algorithms.core.maze.BaseMaze;

@FunctionalInterface
public interface MazeGeneratorStrategy<T> {
    void generate(BaseMaze<T> maze);
}