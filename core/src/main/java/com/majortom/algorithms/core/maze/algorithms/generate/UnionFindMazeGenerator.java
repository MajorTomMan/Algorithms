package com.majortom.algorithms.core.maze.algorithms.generate;

import com.majortom.algorithms.core.basic.UnionFind;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.constants.MazeConstant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于并查集的迷宫生成算法 (Kruskal's Algorithm)
 */
public class UnionFindMazeGenerator extends BaseMazeAlgorithms<int[][]> {

    private UnionFind uf;

    @Override
    public void run(BaseMaze<int[][]> maze) {
        if (maze == null)
            return;

        maze.initial();
        int rows = maze.getRows();
        int cols = maze.getCols();

        // 1. 初始化并查集
        uf = new UnionFind(rows * cols);
        List<int[]> walls = new ArrayList<>();

        // 2. 预处理：设置路点并收集候选墙
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if (r % 2 != 0 && c % 2 != 0) {
                    maze.setCellState(r, c, MazeConstant.ROAD, false);
                } else if ((r % 2 == 0 && c % 2 != 0) || (r % 2 != 0 && c % 2 == 0)) {
                    walls.add(new int[] { r, c });
                }
            }
        }

        // 3. 乱序墙壁
        Collections.shuffle(walls);

        // 4. 核心迭代：尝试打通墙壁
        for (int[] w : walls) {
            sync(maze, w[0], w[1]);

            int wr = w[0], wc = w[1];
            int p1, p2;

            if (wr % 2 == 0) {
                p1 = (wr - 1) * cols + wc;
                p2 = (wr + 1) * cols + wc;
            } else {
                p1 = wr * cols + (wc - 1);
                p2 = wr * cols + (wc + 1);
            }

            if (!uf.connected(p1, p2)) {
                uf.union(p1, p2);
                maze.setCellState(wr, wc, MazeConstant.ROAD, true);
            }
        }
    }
}