package com.majortom.algorithms.core.maze.algorithms.generate;

import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 深度优先搜索 (DFS) 迷宫生成策略
 * 特点：路径深邃、长路径多，分叉相对较少
 */
public class DFSMazeGenerator extends BaseMazeAlgorithms<int[][]> {

    // 步长为2（跳过中间的墙），偏移量为1（打通中间的墙）
    private static final int STEP = 2;
    private static final int MID_OFFSET = 1;

    // 方向向量定义
    private static final List<int[]> DIRECTIONS = Arrays.asList(
            new int[] { -STEP, 0 }, // 上
            new int[] { STEP, 0 }, // 下
            new int[] { 0, -STEP }, // 左
            new int[] { 0, STEP } // 右
    );

    @Override
    public void run(int[][] data) {
        if (mazeEntity == null)
            return;

        // 1. 确保起点 (1, 1) 是路
        // 参数说明：行, 列, 类型, 是否计入 action (触发节流)
        mazeEntity.setCellState(1, 1, MazeConstant.ROAD, true);

        // 2. 开始递归搜索
        dfs((ArrayMaze) mazeEntity, 1, 1);
    }

    /**
     * DFS 核心递归逻辑
     */
    private void dfs(ArrayMaze maze, int r, int c) {
        // 随机打乱方向，确保迷宫的随机性
        Collections.shuffle(DIRECTIONS);

        for (int[] dir : DIRECTIONS) {
            int nextR = r + dir[0];
            int nextC = c + dir[1];

            // 检查目标点是否在边界内，且是否还是“墙” (即未访问过)
            if (!maze.isOverBorder(nextR, nextC) && maze.getCell(nextR, nextC) == MazeConstant.WALL) {

                // 1. 打通中间的墙
                int midR = r + dir[0] / 2;
                int midC = c + dir[1] / 2;
                maze.setCellState(midR, midC, MazeConstant.ROAD, true);

                // 2. 打通目标点
                maze.setCellState(nextR, nextC, MazeConstant.ROAD, true);

                // 3. 递归进入下一个点
                dfs(maze, nextR, nextC);
            }
        }
    }
}