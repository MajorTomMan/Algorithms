package com.majortom.algorithms.core.maze.algorithms.generate;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.constants.MazeConstant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 随机 Prim 迷宫生成算法 (利落重构版)
 * 职责：基于 BaseMaze 接口生成随机树状迷宫，不再依赖具体的 ArrayMaze 实现。
 */
public class BFSMazeGenerator extends BaseMazeAlgorithms<int[][]> {

    private final Random random = new Random();

    @Override
    public void run(BaseMaze<int[][]> maze) {
        if (maze == null)
            return;

        maze.initial();
        List<int[]> walls = new ArrayList<>();
        maze.setCellState(1, 1, MazeConstant.ROAD, true);
        addWalls(maze, 1, 1, walls);

        while (!walls.isEmpty()) {

            sync(maze, null, null);

            int index = random.nextInt(walls.size());
            int[] w = walls.remove(index);

            int midR = w[0], midC = w[1];
            int tarR = w[2], tarC = w[3];

            if (maze.getCell(tarR, tarC) == MazeConstant.WALL) {
                maze.setCellState(midR, midC, MazeConstant.ROAD, true);
                maze.setCellState(tarR, tarC, MazeConstant.ROAD, true);
                addWalls(maze, tarR, tarC, walls);
            }
        }

        maze.setGenerated(true);
    }

    private void addWalls(BaseMaze<int[][]> maze, int r, int c, List<int[]> walls) {
        // 步长为 2 的探测逻辑：跳过墙体直接探测下一个潜在的路点
        int[][] dirs = { { 0, 2 }, { 0, -2 }, { 2, 0 }, { -2, 0 } };
        for (int[] d : dirs) {
            int tr = r + d[0];
            int tc = c + d[1];

            // 越界检查与状态检查
            if (!maze.isOverBorder(tr, tc) && maze.getCell(tr, tc) == MazeConstant.WALL) {
                // 存储：[中间墙行, 中间墙列, 目标点行, 目标点列]
                walls.add(new int[] { r + d[0] / 2, c + d[1] / 2, tr, tc });
            }
        }
    }
}