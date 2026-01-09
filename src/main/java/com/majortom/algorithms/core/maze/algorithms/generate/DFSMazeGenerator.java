package com.majortom.algorithms.core.maze.algorithms.generate;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.maze.strategies.MazeGeneratorStrategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.majortom.algorithms.core.maze.constants.MazeConstant.*;

/**
 * 深度优先搜索 (DFS) 迷宫生成策略
 * 特点：路径深邃、长路径多，具有极强的探险感。
 */
public class DFSMazeGenerator implements MazeGeneratorStrategy<int[][]> {

    // 步长与偏移常量
    private static final int STEP = 2;
    private static final int MID_OFFSET = 1;

    // 方向向量定义：{rowOffset, colOffset}
    private static final int[][] DIRECTIONS = {
            { -STEP, 0 }, // 上
            { STEP, 0 }, // 下
            { 0, -STEP }, // 左
            { 0, STEP } // 右
    };

    @Override
    public void generate(BaseMaze<int[][]> baseMaze) {
        // 1. 强转为 ArrayMaze 以使用具体方法
        ArrayMaze maze = (ArrayMaze) baseMaze;

        // 2. 初始起点通常设为 (1, 1)
        dfs(maze, 1, 1);
    }

    /**
     * DFS 核心递归逻辑
     */
    private void dfs(ArrayMaze maze, int r, int c) {
        // 1. 将当前单元格设为路 (ROAD)
        maze.setCellState(r, c, ROAD, true);

        // 2. 准备并随机化方向索引
        Integer[] dirIndexes = { 0, 1, 2, 3 };
        List<Integer> dirList = Arrays.asList(dirIndexes);
        Collections.shuffle(dirList);

        for (int index : dirList) {
            int[] d = DIRECTIONS[index];
            int nextR = r + d[0];
            int nextC = c + d[1];

            // 3. 边界检查及未访问状态确认（WALL 代表未访问过）
            if (!maze.isOverBorder(nextR, nextC) && maze.getCell(nextR, nextC) == WALL) {

                // 4. 计算并打通两个路点之间的“墙”
                // 根据偏移方向确定中间墙的坐标
                int midR = r + (d[0] == 0 ? 0 : d[0] / STEP * MID_OFFSET);
                int midC = c + (d[1] == 0 ? 0 : d[1] / STEP * MID_OFFSET);

                maze.setCellState(midR, midC, ROAD, true);

                // 5. 递归探索
                dfs(maze, nextR, nextC);
            }
        }
    }
}