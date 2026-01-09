package com.majortom.algorithms.core.maze.algorithms.pathfinding;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.maze.strategies.PathfindingStrategy;

import static com.majortom.algorithms.core.maze.constants.MazeConstant.*;

/**
 * 深度优先搜索 (DFS) 寻路算法
 * 职责：从 Maze 容器中寻找状态为 START 和 END 的格子并建立路径。
 */
public class DFSMazePathfinder implements PathfindingStrategy<int[][]> {

    // 扫描方向：右、下、左、上
    private final int[][] dirs = { { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 } };

    // 成功标记：一旦找到终点则设为 true，终止所有递归
    private boolean found = false;

    @Override
    public void findPath(BaseMaze<int[][]> baseMaze) {
        // 强制转型为 ArrayMaze 以获取行列信息
        ArrayMaze maze = (ArrayMaze) baseMaze;
        this.found = false;

        // 1. 初始化坐标变量
        int startR = -1, startC = -1;
        int targetR = -1, targetC = -1;

        // 2. 遍历整个迷宫矩阵，寻找标记好的起点和终点
        for (int i = 0; i < maze.getRows(); i++) {
            for (int j = 0; j < maze.getCols(); j++) {
                int cellType = maze.getCell(i, j);
                if (cellType == START) {
                    startR = i;
                    startC = j;
                } else if (cellType == END) {
                    targetR = i;
                    targetC = j;
                }
            }
        }

        // 3. 安全检查：如果起终点都存在，则开启递归寻路
        if (startR != -1 && targetR != -1) {
            // 访问标记数组，防止在环路中死循环
            boolean[][] visited = new boolean[maze.getRows()][maze.getCols()];
            dfs(maze, startR, startC, targetR, targetC, visited);
        }
    }

    /**
     * DFS 核心递归函数
     */
    private void dfs(ArrayMaze maze, int r, int c, int tr, int tc, boolean[][] visited) {
        // A. 终止条件：已找到终点，或外部线程中断
        if (found || Thread.currentThread().isInterrupted()) {
            return;
        }

        // B. 标记当前格子已访问
        visited[r][c] = true;

        // C. 判定是否到达目标坐标
        if (r == tr && c == tc) {
            found = true;
            return;
        }

        // D. 渲染路径：只有当格子是普通路 (ROAD) 时，才改为探索色 (PATH)
        // 这样可以保护起点 (START) 不被覆盖颜色
        if (maze.getCell(r, c) == ROAD) {
            maze.setCellState(r, c, PATH, true);
        }

        // E. 递归尝试四个方向
        for (int[] d : dirs) {
            int nextR = r + d[0];
            int nextC = c + d[1];

            if (!maze.isOverBorder(nextR, nextC)) {
                int type = maze.getCell(nextR, nextC);
                if (!visited[nextR][nextC] && (type == ROAD || type == END)) {
                    dfs(maze, nextR, nextC, tr, tc, visited);
                    if (found) {
                        // 只要不是起点，就把当前格点亮为“最终路径”
                        if (maze.getCell(r, c) != START) {
                            maze.setCellState(r, c, BACKTRACK, true);
                        }
                        return;
                    }
                }
            }
        }

        // F. 回溯处理：如果运行到这一步还没找到终点，说明当前路径是死胡同
        // 条件：没找到 && 且不是起点 (START) && 且不是终点 (END)
        int currentType = maze.getCell(r, c);
        if (!found && currentType != START && currentType != END) {
            // 将该格改为死路色 (DEADEND)，在 UI 上表现为“回退”效果
            maze.setCellState(r, c, DEADEND, true);
        }
    }
}