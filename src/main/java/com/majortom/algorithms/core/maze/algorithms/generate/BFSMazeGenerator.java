package com.majortom.algorithms.core.maze.algorithms.generate;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BFSMazeGenerator extends BaseMazeAlgorithms<int[][]> {
    private final Random random = new Random();

    @Override
    public void run(int[][] data) {
        if (mazeEntity == null)
            return;

        ArrayMaze maze = (ArrayMaze) mazeEntity;
        maze.initial(); // 清空迷宫为全墙

        List<int[]> walls = new ArrayList<>();
        // 标记起点 (1,1) 为路
        maze.setCellState(1, 1, 0, true);
        addWalls(maze, 1, 1, walls);

        while (!walls.isEmpty()) {
            int index = random.nextInt(walls.size());
            int[] w = walls.remove(index);

            int tr = w[2], tc = w[3];
            if (maze.getCell(tr, tc) == 1) { // 如果目标点是墙
                maze.setCellState(w[0], w[1], 0, true); // 打通中间墙
                maze.setCellState(tr, tc, 0, true); // 打通目标路
                addWalls(maze, tr, tc, walls);
            }
        }
    }

    private void addWalls(ArrayMaze maze, int r, int c, List<int[]> walls) {
        int[][] dirs = { { 0, 2 }, { 0, -2 }, { 2, 0 }, { -2, 0 } };
        for (int[] d : dirs) {
            int tr = r + d[0], tc = c + d[1];
            if (!maze.isOverBorder(tr, tc) && maze.getCell(tr, tc) == 1) {
                walls.add(new int[] { r + d[0] / 2, c + d[1] / 2, tr, tc });
            }
        }
    }
}