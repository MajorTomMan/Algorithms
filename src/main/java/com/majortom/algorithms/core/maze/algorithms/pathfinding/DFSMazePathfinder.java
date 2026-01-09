package com.majortom.algorithms.core.maze.algorithms.pathfinding;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.maze.strategies.PathfindingStrategy;

/**
 * 深度优先搜索 (DFS) 寻路策略
 * 特点：路径深邃，具有极强的方向探索感，但不保证最短路径。
 */
public class DFSMazePathfinder implements PathfindingStrategy<int[][]> {

    // 定义四个方向：右、下、左、上
    private final int[][] dirs = { { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 } };
    private boolean found = false;

    @Override
    public void findPath(BaseMaze<int[][]> baseMaze) {
        ArrayMaze maze = (ArrayMaze) baseMaze;
        found = false;

        // 记录访问状态，防止走回头路死循环
        boolean[][] visited = new boolean[maze.getRows()][maze.getCols()];

        // 从起点 (1, 1) 开始递归寻路，目标设为右下角的 (rows-2, cols-2)
        dfs(maze, 1, 1, maze.getRows() - 2, maze.getCols() - 2, visited);
    }

    private void dfs(ArrayMaze maze, int r, int c, int targetR, int targetC, boolean[][] visited) {
        // 如果已经找到终点或当前线程被中断（点击了重置），则停止递归
        if (found || Thread.currentThread().isInterrupted())
            return;

        // 1. 标记当前点已访问，并更新 UI 状态
        visited[r][c] = true;
        // 状态 2 可以定义为“正在探索的路径”，在 MazePanel 里可以涂成蓝色或紫色
        maze.setCellState(r, c, 2, true);

        // 2. 检查是否到达终点
        if (r == targetR && c == targetC) {
            found = true;
            return;
        }

        // 3. 尝试向四个方向探索
        for (int[] d : dirs) {
            int nextR = r + d[0];
            int nextC = c + d[1];

            // 检查：界内 + 是路(0) + 未访问
            if (!maze.isOutOfIndex(nextR, nextC) &&
                    maze.getCell(nextR, nextC) == 0 &&
                    !visited[nextR][nextC]) {

                dfs(maze, nextR, nextC, targetR, targetC, visited);

                if (found)
                    return; // 找到后直接回退，不执行后面的重置
            }
        }

        // 4. 【回溯逻辑】
        // 如果四个方向都走不通，说明此路是死胡同，将状态改回通路 (0) 或标记为“已探索的死路” (4)
        if (!found) {
            // 在 UI 上撤销这一点的路径显示，表现出“退回去”的效果
            maze.setCellState(r, c, 4, true);
        }
    }
}