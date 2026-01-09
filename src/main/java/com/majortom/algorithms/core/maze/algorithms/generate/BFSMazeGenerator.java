package com.majortom.algorithms.core.maze.algorithms.generate;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.maze.strategies.MazeGeneratorStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 基于 BFS 思想（随机 Prim）的迷宫生成策略
 * 特点：从中心向四周扩散，分支极多，视觉上具有“蔓延感”。
 */
public class BFSMazeGenerator implements MazeGeneratorStrategy<int[][]> {
    private final Random random = new Random();

    @Override
    public void generate(BaseMaze<int[][]> baseMaze) {
        // 1. 强转为 ArrayMaze 以使用 isOutOfIndex 和 getCell
        ArrayMaze maze = (ArrayMaze) baseMaze;

        // 2. 选取随机起点（必须是奇数坐标，保证迷宫有墙的间隔）
        int startR = 1;
        int startC = 1;

        // 3. 待处理的墙列表：存储格式为 {wallR, wallC, targetR, targetC}
        List<int[]> walls = new ArrayList<>();

        // 标记起点为路 (0)
        maze.setCellState(startR, startC, 0, true);
        // 将起点的四周墙加入列表
        addWalls(maze, startR, startC, walls);

        while (!walls.isEmpty()) {
            // 4. 随机取出一面墙（Prim 算法的核心：随机挑选边缘进行扩张）
            int index = random.nextInt(walls.size());
            int[] w = walls.remove(index);

            int wr = w[0], wc = w[1]; // 墙坐标
            int tr = w[2], tc = w[3]; // 目标路点坐标

            // 5. 如果穿过这面墙到达的目标点还是墙（说明未访问过），则打通
            if (maze.getCell(tr, tc) == 1) {
                maze.setCellState(wr, wc, 0, true); // 破墙
                maze.setCellState(tr, tc, 0, true); // 设目标为路

                // 6. 将新路点的邻居墙加入候选列表
                addWalls(maze, tr, tc, walls);
            }
        }
    }

    /**
     * 将 (r, c) 附近的墙加入候选列表
     */
    private void addWalls(ArrayMaze maze, int r, int c, List<int[]> walls) {
        // 跨度为 2 的四个方向
        int[][] dirs = { { 0, 2 }, { 0, -2 }, { 2, 0 }, { -2, 0 } };
        for (int[] d : dirs) {
            int tr = r + d[0];
            int tc = c + d[1];
            // 检查跨越后的目标点是否在界内且是墙
            if (!maze.isOutOfIndex(tr, tc) && maze.getCell(tr, tc) == 1) {
                // 中间的墙坐标：(r + tr)/2, (c + tc)/2
                walls.add(new int[] { r + d[0] / 2, c + d[1] / 2, tr, tc });
            }
        }
    }
}