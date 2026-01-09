package com.majortom.algorithms.core.maze.algorithms.generate;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.maze.strategies.MazeGeneratorStrategy;
import com.majortom.algorithms.core.maze.constants.MazeConstant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.majortom.algorithms.core.maze.constants.MazeConstant.*;

/**
 * 基于 BFS 思想（随机 Prim）的迷宫生成策略
 * 特点：从中心向四周扩散，分支极多，视觉上具有“蔓延感”。
 */
public class BFSMazeGenerator implements MazeGeneratorStrategy<int[][]> {
    private final Random random = new Random();

    // 步长常量：Prim 算法中每次跨越 2 格（一格墙，一格路）
    private static final int STEP = 2;
    private static final int MID_OFFSET = 2;

    @Override
    public void generate(BaseMaze<int[][]> baseMaze) {
        // 1. 强转为 ArrayMaze 以使用相关边界检查方法
        ArrayMaze maze = (ArrayMaze) baseMaze;

        // 2. 选取固定起点（建议坐标为奇数，以匹配迷宫格栅结构）
        int startR = 1;
        int startC = 1;

        // 3. 待处理的墙列表：存储格式为 {wallR, wallC, targetR, targetC}
        List<int[]> walls = new ArrayList<>();

        // 标记起点为路 (ROAD)
        maze.setCellState(startR, startC, ROAD, true);

        // 将起点的四周候选墙加入列表
        addWalls(maze, startR, startC, walls);

        while (!walls.isEmpty()) {
            // 4. 随机取出一面墙（体现随机 Prim 的“蔓延”特性）
            int index = random.nextInt(walls.size());
            int[] w = walls.remove(index);

            int wr = w[0], wc = w[1]; // 墙坐标
            int tr = w[2], tc = w[3]; // 目标路点坐标

            // 5. 如果穿过这面墙到达的目标点还是墙 (WALL)，则打通路径
            if (maze.getCell(tr, tc) == WALL) {
                // 将中间的墙和目标点都设为路 (ROAD)
                maze.setCellState(wr, wc, ROAD, true);
                maze.setCellState(tr, tc, ROAD, true);

                // 6. 将新路点的邻居墙加入候选列表
                addWalls(maze, tr, tc, walls);
            }
        }
    }

    /**
     * 将 (r, c) 附近的候选墙加入列表
     */
    private void addWalls(ArrayMaze maze, int r, int c, List<int[]> walls) {
        // 跨度为 STEP 的四个方向
        int[][] dirs = {
                { 0, STEP }, { 0, -STEP },
                { STEP, 0 }, { -STEP, 0 }
        };

        for (int[] d : dirs) {
            int tr = r + d[0];
            int tc = c + d[1];

            // 检查目标点是否在界内且尚未被打通（仍是 WALL）
            if (!maze.isOverBorder(tr, tc) && maze.getCell(tr, tc) == WALL) {
                // 计算中间被跨过的墙坐标
                int midR = r + d[0] / MID_OFFSET;
                int midC = c + d[1] / MID_OFFSET;

                walls.add(new int[] { midR, midC, tr, tc });
            }
        }
    }
}