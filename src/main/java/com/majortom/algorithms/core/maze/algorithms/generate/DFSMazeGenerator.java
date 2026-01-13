package com.majortom.algorithms.core.maze.algorithms.generate;

import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 深度优先搜索 (DFS) 迷宫生成策略
 * 适配说明：通过 BaseMazeAlgorithms<int[][], ArrayMaze> 实现类型全通透，消除强转。
 */
public class DFSMazeGenerator extends BaseMazeAlgorithms<int[][], ArrayMaze> {

    private static final int STEP = 2;

    // 方向向量定义
    private final List<int[]> directions = Arrays.asList(
            new int[] { -STEP, 0 }, // 上
            new int[] { STEP, 0 }, // 下
            new int[] { 0, -STEP }, // 左
            new int[] { 0, STEP } // 右
    );

    @Override
    public void run(ArrayMaze maze) {
        // 1. 初始化迷宫状态（全墙）
        maze.initial();

        // 2. 确保起点 (1, 1) 是路
        // 🚩 这里的 maze 直接就是 ArrayMaze 类型，不需要强转
        maze.setCellState(1, 1, MazeConstant.ROAD, true);

        // 3. 开始递归搜索
        dfs(maze, 1, 1);

        maze.setGenerated(true);
    }

    private void dfs(ArrayMaze maze, int r, int c) {
        // 随机打乱方向，确保迷宫的随机性
        Collections.shuffle(directions);

        // 🚩 注意：为了避免递归中共享同一个打乱后的 directions，
        // 建议在这里 copy 一份或者每次循环克隆，虽然目前 static 引用在单线程下能跑，
        // 但为了严谨，我们直接用局部变量的思想。
        List<int[]> currentDirs = Arrays.asList(directions.toArray(new int[0][]));
        Collections.shuffle(currentDirs);

        for (int[] dir : currentDirs) {
            int nextR = r + dir[0];
            int nextC = c + dir[1];

            // 检查目标点是否在边界内，且是否还是“墙”
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