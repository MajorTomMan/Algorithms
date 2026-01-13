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
 * 职责：利用并查集维护连通分量，生成全局随机、无环的完美迷宫。
 * 适配说明：单泛型重构，对齐 BaseMazeAlgorithms<int[][]>。
 */
public class UnionFindMazeGenerator extends BaseMazeAlgorithms<int[][]> {

    private UnionFind uf;

    @Override
    public void run(BaseMaze<int[][]> maze) {
        if (maze == null)
            return;

        // 1. 初始化迷宫：重置为全墙
        maze.initial();

        int rows = maze.getRows();
        int cols = maze.getCols();

        // 2. 初始化并查集：大小为迷宫总格数
        uf = new UnionFind(rows * cols);

        // 3. 准备待选墙列表
        List<int[]> walls = new ArrayList<>();

        // 4. 预处理：将所有奇数格设为“路点”，偶数交叉点存为“待选墙”
        // 这一步决定了迷宫的格点结构
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                // 初始路点：奇数行且奇数列 (isAction 为 false，静态初始化不产生动画)
                if (r % 2 != 0 && c % 2 != 0) {
                    maze.setCellState(r, c, MazeConstant.ROAD, false);
                }
                // 潜在的墙：跨接在两个路点之间的格子（一奇一偶）
                else if ((r % 2 == 0 && c % 2 != 0) || (r % 2 != 0 && c % 2 == 0)) {
                    walls.add(new int[] { r, c });
                }
            }
        }

        // 5. 乱序：打乱墙的顺序，这是 Kruskal 生成随机性的核心
        Collections.shuffle(walls);

        // 6. 核心遍历：尝试合并连通分量
        for (int[] w : walls) {
            int wr = w[0], wc = w[1];
            int p1, p2;

            if (wr % 2 == 0) {
                // 纵向墙：尝试连接 上 (wr-1) 和 下 (wr+1) 两个格点的 ID
                p1 = (wr - 1) * cols + wc;
                p2 = (wr + 1) * cols + wc;
            } else {
                // 横向墙：尝试连接 左 (wc-1) 和 右 (wc+1) 两个格点的 ID
                p1 = wr * cols + (wc - 1);
                p2 = wr * cols + (wc + 1);
            }

            // 7. 核心逻辑：如果两个点在并查集中不连通
            if (!uf.connected(p1, p2)) {
                uf.union(p1, p2);

                // 🚩 关键：打通墙壁，isAction 为 true 触发视觉同步
                // UI 上会看到随机分布的墙壁逐渐消失，直到整棵生成树完成
                maze.setCellState(wr, wc, MazeConstant.ROAD, true);
            }
        }

        // 标记生成完成，通知 UI 模块（如显示生成耗时、解锁寻路按钮等）
        maze.setGenerated(true);
    }
}