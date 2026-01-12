package com.majortom.algorithms.core.maze.algorithms.generate;

import com.majortom.algorithms.core.basic.UnionFind;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于并查集的迷宫生成算法（Kruskal's Algorithm）
 * 特点：生成的迷宫分支非常均匀，没有明显的生长中心，视觉上比 DFS 更细碎。
 */
public class UnionFindMazeGenerator extends BaseMazeAlgorithms<int[][]> {

    private UnionFind uf;

    @Override
    public void run(int[][] data) {
        // 1. 核心上下文检查
        if (mazeEntity == null)
            return;

        ArrayMaze maze = (ArrayMaze) mazeEntity;
        int rows = maze.getRows();
        int cols = maze.getCols();

        // 2. 初始化并查集
        uf = new UnionFind(rows * cols);

        // 3. 准备待选墙列表
        List<int[]> walls = new ArrayList<>();

        // 4. 预处理：将所有奇数格作为初始“孤岛”
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                // 初始路点：奇数行且奇数列
                if (r % 2 != 0 && c % 2 != 0) {
                    maze.setCellState(r, c, MazeConstant.ROAD, false);
                }
                // 潜在的墙：跨接在两个路点之间的格子（一奇一偶）
                else if ((r % 2 == 0 && c % 2 != 0) || (r % 2 != 0 && c % 2 == 0)) {
                    walls.add(new int[] { r, c });
                }
            }
        }

        // 5. 乱序墙列表：这是 Kruskal 算法随机性的来源
        Collections.shuffle(walls);

        // 6. 遍历墙，尝试合并集合
        for (int[] w : walls) {
            int wr = w[0], wc = w[1];
            int p1, p2;

            if (wr % 2 == 0) {
                // 纵向墙：连接 上 (wr-1) 和 下 (wr+1)
                p1 = (wr - 1) * cols + wc;
                p2 = (wr + 1) * cols + wc;
            } else {
                // 横向墙：连接 左 (wc-1) 和 右 (wc+1)
                p1 = wr * cols + (wc - 1);
                p2 = wr * cols + (wc + 1);
            }

            // 7. 核心逻辑：如果不连通，则打通这面墙
            if (!uf.connected(p1, p2)) {
                uf.union(p1, p2);
                // 设为路并触发 UI 同步（这里开启 Action 计数，产生动画效果）
                maze.setCellState(wr, wc, MazeConstant.ROAD, true);
            }
        }
    }
}