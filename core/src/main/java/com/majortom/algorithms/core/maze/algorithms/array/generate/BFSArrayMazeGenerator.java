package com.majortom.algorithms.core.maze.algorithms.array.generate;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseArrayMazeAlgorithms;
import com.majortom.algorithms.core.maze.constants.MazeCellType;
import com.majortom.algorithms.core.maze.constants.MazeDefaults;
import com.majortom.algorithms.core.maze.constants.MazeDirections;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 随机 Prim 迷宫生成算法 (利落重构版)
 * 职责：基于 BaseMaze 接口生成随机树状迷宫，不再依赖具体的 ArrayMaze 实现。
 */
public class BFSArrayMazeGenerator extends BaseArrayMazeAlgorithms<int[][]> {

    private final Random random = new Random();

    @Override
    public void run(BaseMaze<int[][]> maze) {
        if (maze == null)
            return;

        maze.initial();
        List<int[]> walls = new ArrayList<>();
        maze.setCellState(MazeDefaults.START_ROW, MazeDefaults.START_COL, MazeCellType.ROAD, true);
        addWalls(maze, MazeDefaults.START_ROW, MazeDefaults.START_COL, walls);

        while (!walls.isEmpty()) {

            sync(maze, null, null);

            int index = random.nextInt(walls.size());
            int[] w = walls.remove(index);

            int midR = w[0], midC = w[1];
            int tarR = w[2], tarC = w[3];

            if (maze.getCell(tarR, tarC) == MazeCellType.WALL) {
                maze.setCellState(midR, midC, MazeCellType.ROAD, true);
                maze.setCellState(tarR, tarC, MazeCellType.ROAD, true);
                addWalls(maze, tarR, tarC, walls);
            }
        }

        maze.setGenerated(true);
    }

    private void addWalls(BaseMaze<int[][]> maze, int r, int c, List<int[]> walls) {
        // 步长为 2 的探测逻辑：跳过墙体直接探测下一个潜在的路点
        for (int[] d : MazeDirections.CARVE_DIRECTIONS) {
            int tr = r + d[0];
            int tc = c + d[1];

            // 越界检查与状态检查
            if (!maze.isOverBorder(tr, tc) && maze.getCell(tr, tc) == MazeCellType.WALL) {
                // 存储：[中间墙行, 中间墙列, 目标点行, 目标点列]
                walls.add(new int[] {
                        r + d[0] / MazeDirections.CARVE_STEP,
                        c + d[1] / MazeDirections.CARVE_STEP,
                        tr,
                        tc
                });
            }
        }
    }
}
