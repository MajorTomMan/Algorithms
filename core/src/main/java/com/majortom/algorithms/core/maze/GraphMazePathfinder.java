package com.majortom.algorithms.core.maze;

/**
 * 图迷宫寻路算法标记接口。
 *
 * <p>这个接口用于把图迷宫算法归类到“寻路阶段”。实现类通常会从 {@code GraphMaze#getStartId()}
 * 出发，沿着 {@code getNeighborIds(...)} 返回的真实图边搜索，直到抵达 {@code getEndId()}。</p>
 *
 * <p>教学上可以把寻路器理解为“走路的人”：它不负责创造通道，只读取生成器留下的图结构，
 * 并通过格子状态标记 PATH、DEADEND 或 BACKTRACK，让执行帧和可视化器把搜索过程逐步播放出来。</p>
 */
public interface GraphMazePathfinder {
}
