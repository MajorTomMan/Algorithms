package com.majortom.algorithms.core.maze.strategies;

import com.majortom.algorithms.core.maze.BaseMaze;

/** 寻路算法策略 */
@FunctionalInterface
public interface PathfindingStrategy<T> {
    void findPath(BaseMaze<T> maze);
}
