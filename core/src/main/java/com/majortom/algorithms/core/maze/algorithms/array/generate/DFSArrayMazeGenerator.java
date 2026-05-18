package com.majortom.algorithms.core.maze.algorithms.array.generate;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseArrayMazeAlgorithms;
import com.majortom.algorithms.core.maze.constants.MazeCellType;
import com.majortom.algorithms.core.maze.constants.MazeDefaults;
import com.majortom.algorithms.core.maze.constants.MazeDirections;

import java.util.Random;

/**
 * 深度优先搜索 (DFS) 迷宫生成策略
 *
 * <p>
 * 这个生成器负责“挖迷宫”。它从一个起始路点出发，按随机方向不断向前探测，
 * 每次跨过一堵墙找到下一个候选路点，再把中间那堵墙打通，从而形成典型的递归回溯迷宫。
 * </p>
 *
 * <p>
 * 联动上，它只关心如何把格子从墙改成路：
 * {@link BaseMaze} 负责保存格子状态并同步执行帧，
 * {@link MazeDirections} 提供网格位移规则，
 * {@link MazeCellType} 定义“墙”和“路”分别用什么状态值表达。
 * </p>
 */
public class DFSArrayMazeGenerator extends BaseArrayMazeAlgorithms<int[][]> {

    private final Random random = new Random();

    @Override
    public void run(BaseMaze<int[][]> maze) {
        if (maze == null)
            return;

        maze.initial();
        maze.setCellState(MazeDefaults.START_ROW, MazeDefaults.START_COL, MazeCellType.ROAD, true);
        dfs(maze, MazeDefaults.START_ROW, MazeDefaults.START_COL);

        maze.setGenerated(true);
    }

    /**
     * 从指定路点继续向外挖通迷宫。
     *
     * <p>
     * 参数 {@code r/c} 表示当前递归正在处理的“路点坐标”，而不是任意格子。
     * 方法会按随机顺序尝试四个双步方向：如果目标仍是墙，就把中间那堵墙和目标点一起改成道路，
     * 然后从新的路点继续递归。
     * </p>
     *
     * @param maze 当前迷宫实体，负责保存状态和同步动画
     * @param r    当前路点的行坐标
     * @param c    当前路点的列坐标
     */
    private void dfs(BaseMaze<int[][]> maze, int r, int c) {
        sync(maze, r, c);

        int[] indexOrder = { 0, 1, 2, 3 };
        shuffleArray(indexOrder);

        for (int i : indexOrder) {
            int[] dir = MazeDirections.CARVE_DIRECTIONS[i];
            int nextR = r + dir[0];
            int nextC = c + dir[1];

            if (!maze.isOverBorder(nextR, nextC) && maze.getCell(nextR, nextC) == MazeCellType.WALL) {
                int midR = r + dir[0] / MazeDirections.CARVE_STEP;
                int midC = c + dir[1] / MazeDirections.CARVE_STEP;

                maze.setCellState(midR, midC, MazeCellType.ROAD, true);
                maze.setCellState(nextR, nextC, MazeCellType.ROAD, true);
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
