package com.majortom.algorithms.core.maze.algorithms.generate;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.constants.MazeConstant;

import java.util.Random;

/**
 * 深度优先搜索 (DFS) 迷宫生成策略 (利落重构版)
 * 职责：基于递归回溯算法生成长廊型迷宫。
 * 适配说明：泛型已对齐 BaseMaze<int[][]>，消除对具体实现类的依赖。
 */
public class DFSMazeGenerator extends BaseMazeAlgorithms<int[][]> {

    private static final int STEP = 2;
    private final Random random = new Random();

    // 方向向量定义（使用数组，减少包装开销）
    private final int[][] directions = {
            { -STEP, 0 }, // 上
            { STEP, 0 }, // 下
            { 0, -STEP }, // 左
            { 0, STEP } // 右
    };

    @Override
    public void run(BaseMaze<int[][]> maze) {
        if (maze == null)
            return;

        // 1. 初始化迷宫状态（全墙）
        maze.initial();

        // 2. 确保起点 (1, 1) 是路
        maze.setCellState(1, 1, MazeConstant.ROAD, true);

        // 3. 开始递归搜索生成
        dfs(maze, 1, 1);

        // 标记生成完成
        maze.setGenerated(true);
    }

    private void dfs(BaseMaze<int[][]> maze, int r, int c) {
        // 🚩 局部洗牌算法：避免在递归中频繁创建 List 对象，提高内存效率
        int[] indexOrder = { 0, 1, 2, 3 };
        shuffleArray(indexOrder);

        for (int i : indexOrder) {
            int[] dir = directions[i];
            int nextR = r + dir[0];
            int nextC = c + dir[1];

            // 检查目标点是否在边界内，且是否还是“墙”
            if (!maze.isOverBorder(nextR, nextC) && maze.getCell(nextR, nextC) == MazeConstant.WALL) {

                // 1. 打通当前点与目标点之间的墙
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

    /**
     * 简单的 Fisher-Yates 洗牌，确保递归深度的随机性
     */
    private void shuffleArray(int[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }
}