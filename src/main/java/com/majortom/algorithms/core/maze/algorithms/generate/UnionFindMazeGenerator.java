package com.majortom.algorithms.core.maze.algorithms.generate;

import com.majortom.algorithms.core.basic.UnionFind;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于并查集的迷宫生成算法 (Kruskal's Algorithm)
 * 适配说明：通过双泛型约束实现类型安全，利用 BaseStructure 自动同步 UI。
 */
public class UnionFindMazeGenerator extends BaseMazeAlgorithms<int[][], ArrayMaze> {

    private UnionFind uf;

    @Override
    public void run(ArrayMaze maze) {
        // 1. 初始化迷宫：重置为全墙
        maze.initial();

        int rows = maze.getRows();
        int cols = maze.getCols();

        // 2. 初始化并查集
        uf = new UnionFind(rows * cols);

        // 3. 准备待选墙列表
        List<int[]> walls = new ArrayList<>();

        // 4. 预处理：将所有奇数格设为“路点”，偶数交叉点存为“待选墙”
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                // 初始路点：奇数行且奇数列（这里 isAction 为 false，不产生步进动画）
                if (r % 2 != 0 && c % 2 != 0) {
                    maze.setCellState(r, c, MazeConstant.ROAD, false);
                }
                // 潜在的墙：跨接在两个路点之间的格子（一奇一偶）
                else if ((r % 2 == 0 && c % 2 != 0) || (r % 2 != 0 && c % 2 == 0)) {
                    walls.add(new int[] { r, c });
                }
            }
        }

        // 5. 乱序：决定迷宫生成的随机性
        Collections.shuffle(walls);

        // 6. 核心遍历：尝试打通墙壁
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

            // 7. 核心逻辑：如果不连通，则合并并打通
            if (!uf.connected(p1, p2)) {
                uf.union(p1, p2);

                // 💡 关键点：isAction 设为 true，这样每次打通墙都会触发 SyncListener
                // 并产生一次 Thread.sleep 或等待 UI 信号，形成完美的生成动画
                maze.setCellState(wr, wc, MazeConstant.ROAD, true);
            }
        }

        maze.setGenerated(true);
    }
}