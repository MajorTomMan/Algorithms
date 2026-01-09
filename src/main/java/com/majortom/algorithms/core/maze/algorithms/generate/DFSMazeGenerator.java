package com.majortom.algorithms.core.maze.algorithms.generate;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.maze.strategies.MazeGeneratorStrategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 深度优先搜索 (DFS) 迷宫生成策略
 * 特点：路径深邃、长路径多，具有极强的探险感。
 */
public class DFSMazeGenerator implements MazeGeneratorStrategy<int[][]> {

    @Override
    public void generate(BaseMaze<int[][]> baseMaze) {
        // 1. 强转为 ArrayMaze 以使用 isOutOfIndex 等具体方法
        ArrayMaze maze = (ArrayMaze) baseMaze;

        // 2. 初始起点通常设为 (1, 1)
        // 注意：为了保证递归顺利进行，这里调用私有递归方法，并将容器传入
        dfs(maze, 1, 1);
    }

    /**
     * DFS 核心递归逻辑
     */
    private void dfs(ArrayMaze maze, int r, int c) {
        // 1. 将当前单元格设为路径 (0)，触发一次操作统计
        maze.setCellState(r, c, 0, true);

        // 2. 准备并随机化方向
        Integer[] directions = { 0, 1, 2, 3 };
        List<Integer> dirList = Arrays.asList(directions);
        Collections.shuffle(dirList);

        for (int dir : dirList) {
            // 计算跨过墙的目标点（步长为 2）
            int nextR = r, nextC = c;
            int midR = r, midC = c;

            if (dir == 0) {
                nextR = r - 2;
                midR = r - 1;
            } // 上
            else if (dir == 1) {
                nextR = r + 2;
                midR = r + 1;
            } // 下
            else if (dir == 2) {
                nextC = c - 2;
                midC = c - 1;
            } // 左
            else if (dir == 3) {
                nextC = c + 2;
                midC = c + 1;
            } // 右

            // 3. 边界检查及未访问状态确认（1 为墙，代表未访问过）
            if (!maze.isOutOfIndex(nextR, nextC) && maze.getCell(nextR, nextC) == 1) {
                // 4. 打通两个路点之间的“墙”
                maze.setCellState(midR, midC, 0, true);

                // 5. 递归探索
                dfs(maze, nextR, nextC);
            }
        }
    }
}