package com.majortom.algorithms.core.maze.algorithms.generate;

import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 随机 Prim 迷宫生成算法
 * 职责：利用广度优先的候选墙列表，生成随机树状迷宫
 */
public class BFSMazeGenerator extends BaseMazeAlgorithms<int[][], ArrayMaze> {
    private final Random random = new Random();

    @Override
    public void run(ArrayMaze maze) {
        // 1. 初始化：使用实体自身的方法，无需强转
        maze.initial();

        List<int[]> walls = new ArrayList<>();

        // 2. 选取起点 (1,1)
        // 假设 MazeConstant.PATH = 0, MazeConstant.WALL = 1
        maze.setCellState(1, 1, 0, true);
        addWalls(maze, 1, 1, walls);

        // 3. 核心迭代
        while (!walls.isEmpty()) {
            // 随机选择一根候选墙
            int index = random.nextInt(walls.size());
            int[] w = walls.remove(index);

            int tr = w[2], tc = w[3]; // 墙对面的目标点

            // 如果目标点还是墙，说明还没被联通
            if (maze.getCell(tr, tc) == 1) {
                // 打通中间的墙 (w[0], w[1]) 和 目标点 (tr, tc)
                maze.setCellState(w[0], w[1], 0, true);
                maze.setCellState(tr, tc, 0, true);

                // 将新打通点的周围墙加入候选列表
                addWalls(maze, tr, tc, walls);
            }
        }
        maze.setGenerated(true);
    }

    private void addWalls(ArrayMaze maze, int r, int c, List<int[]> walls) {
        // 步长为 2 的探测逻辑（迷宫生成的核心，确保墙与路交替）
        int[][] dirs = { { 0, 2 }, { 0, -2 }, { 2, 0 }, { -2, 0 } };
        for (int[] d : dirs) {
            int tr = r + d[0], tc = c + d[1];
            if (!maze.isOverBorder(tr, tc) && maze.getCell(tr, tc) == 1) {
                // 存储数据：[中间墙行, 中间墙列, 目标路行, 目标路列]
                walls.add(new int[] { r + d[0] / 2, c + d[1] / 2, tr, tc });
            }
        }
    }
}