package com.majortom.algorithms.core.maze.algorithms.pathfinding;

import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import static com.majortom.algorithms.core.maze.constants.MazeConstant.*;

/**
 * 深度优先搜索 (DFS) 寻路算法
 * 适配说明：对接双泛型架构，利用递归回溯实现路径染色，提供“死路回退”动画。
 */
public class DFSMazePathfinder extends BaseMazeAlgorithms<int[][], ArrayMaze> {

    private final int[][] dirs = { { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 } };
    private boolean found = false;

    @Override
    public void run(ArrayMaze maze) {
        // 1. 初始化状态
        this.found = false;
        int rows = maze.getRows();
        int cols = maze.getCols();
        int[][] data = maze.getData();

        // 2. 定位起点与终点
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

    private void dfs(ArrayMaze maze, int r, int c, int tr, int tc, boolean[][] visited) {
        if (found || Thread.currentThread().isInterrupted())
            return;

        visited[r][c] = true;

        if (r == tr && c == tc) {
            found = true;
            return;
        }

        // 探测阶段染色：保护起点和终点
        int currentType = maze.getCell(r, c);
        if (currentType == ROAD) {
            maze.setCellState(r, c, PATH, true);
        }

        for (int[] d : dirs) {
            int nextR = r + d[0];
            int nextC = c + d[1];

            if (!maze.isOverBorder(nextR, nextC)) {
                int type = maze.getCell(nextR, nextC);
                if (!visited[nextR][nextC] && (type == ROAD || type == END)) {
                    dfs(maze, nextR, nextC, tr, tc, visited);

                    if (found) {
                        // 【核心回溯】找到终点后，沿递归栈返回，将正确路径染为 BACKTRACK
                        if (maze.getCell(r, c) == PATH) {
                            maze.setCellState(r, c, BACKTRACK, true);
                        }
                        return;
                    }
                }
            }
        }

        // 【回退处理】如果没有找到终点且不是特殊格点，渲染为 DEADEND
        if (!found && maze.getCell(r, c) == PATH) {
            maze.setCellState(r, c, DEADEND, true);
        }
    }
}