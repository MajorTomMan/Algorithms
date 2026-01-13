package com.majortom.algorithms.core.maze.algorithms.pathfinding;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import static com.majortom.algorithms.core.maze.constants.MazeConstant.*;

/**
 * 深度优先搜索 (DFS) 寻路算法 (利落重构版)
 * 职责：利用递归回溯实现路径探索。
 * 视觉特征：展示“盲目探索 -> 发现死路回退 -> 最终路径高亮”的完整过程。
 */
public class DFSMazePathfinder extends BaseMazeAlgorithms<int[][]> {

    private final int[][] dirs = { { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 } };
    private boolean found = false;

    @Override
    public void run(BaseMaze<int[][]> maze) {
        if (maze == null)
            return;

        // 1. 初始化状态
        this.found = false;
        int rows = maze.getRows();
        int cols = maze.getCols();
        int[][] data = maze.getData();

        // 2. 定位起点与终点 (使用通用接口)
        int startR = -1, startC = -1;
        int targetR = -1, targetC = -1;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int cellType = data[i][j];
                if (cellType == START) {
                    startR = i;
                    startC = j;
                } else if (cellType == END) {
                    targetR = i;
                    targetC = j;
                }
            }
        }

        // 3. 开启寻路逻辑
        if (startR != -1 && targetR != -1) {
            boolean[][] visited = new boolean[rows][cols];
            dfs(maze, startR, startC, targetR, targetC, visited);
        }
    }

    private void dfs(BaseMaze<int[][]> maze, int r, int c, int tr, int tc, boolean[][] visited) {
        // 响应中断与找到标记
        if (found || Thread.currentThread().isInterrupted())
            return;

        visited[r][c] = true;

        // 到达终点
        if (r == tr && c == tc) {
            found = true;
            return;
        }

        // 【探测阶段】染色：将当前路点设为 PATH (忧郁紫)
        int currentType = maze.getCell(r, c);
        if (currentType == ROAD) {
            // isAction=true 产生步进动画
            maze.setCellState(r, c, PATH, true);
        }

        for (int[] d : dirs) {
            int nextR = r + d[0];
            int nextC = c + d[1];

            if (!maze.isOverBorder(nextR, nextC)) {
                int type = maze.getCell(nextR, nextC);
                // 只有没走过且是路或终点才进入
                if (!visited[nextR][nextC] && (type == ROAD || type == END)) {
                    dfs(maze, nextR, nextC, tr, tc, visited);

                    if (found) {
                        // 【核心回溯】如果下方递归找到了终点，回溯时将路径染成 BACKTRACK (琥珀金)
                        if (maze.getCell(r, c) == PATH) {
                            maze.setCellState(r, c, BACKTRACK, true);
                        }
                        return;
                    }
                }
            }
        }

        // 【死路处理】如果遍历完四个方向都没找到终点，且当前是探测路径，则标记为 DEADEND
        if (!found && maze.getCell(r, c) == PATH) {
            maze.setCellState(r, c, DEADEND, true);
        }
    }
}