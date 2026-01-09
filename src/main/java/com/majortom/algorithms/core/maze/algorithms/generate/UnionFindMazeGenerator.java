package com.majortom.algorithms.core.maze.algorithms.generate;

import com.majortom.algorithms.core.basic.UnionFind;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.maze.strategies.MazeGeneratorStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于并查集的迷宫生成算法（Kruskal's Algorithm 变体）
 * 
 */
public class UnionFindMazeGenerator implements MazeGeneratorStrategy<int[][]> {

    private UnionFind uf;

    /**
     * 实现策略接口的 generate 方法
     * 
     * @param baseMaze 传入的迷宫容器
     */
    @Override
    public void generate(BaseMaze<int[][]> baseMaze) {
        // 1. 类型转换：确保我们可以调用 ArrayMaze 特有的 getRows/getCols
        ArrayMaze maze = (ArrayMaze) baseMaze;
        int rows = maze.getRows();
        int cols = maze.getCols();

        // 2. 初始化并查集，大小为所有格子的映射总数
        uf = new UnionFind(rows * cols);

        // 3. 准备待选墙列表
        List<int[]> walls = new ArrayList<>();

        // 遍历内部网格（跳过最外层边界）
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                // 情况 A：奇数行/奇数列 —— 它们是初始的“孤岛”路点
                if (r % 2 != 0 && c % 2 != 0) {
                    // 设为路，不计入操作步数统计
                    maze.setCellState(r, c, 0, false);
                }
                // 情况 B：潜在的墙（坐标为一奇一偶）
                else if ((r % 2 == 0 && c % 2 != 0) || (r % 2 != 0 && c % 2 == 0)) {
                    walls.add(new int[] { r, c });
                }
            }
        }

        // 4. 核心：打乱墙的顺序，实现随机迷宫生成
        Collections.shuffle(walls);

        // 5. 遍历墙，尝试连通两个区域
        for (int[] w : walls) {
            int wr = w[0], wc = w[1];
            int p1, p2;

            if (wr % 2 == 0) { // 偶数行墙：连接上方 (wr-1, wc) 和 下方 (wr+1, wc)
                p1 = (wr - 1) * cols + wc;
                p2 = (wr + 1) * cols + wc;
            } else { // 奇数行墙（必为偶数列）：连接左方 (wr, wc-1) 和 右方 (wr, wc+1)
                p1 = wr * cols + (wc - 1);
                p2 = wr * cols + (wc + 1);
            }

            // 6. 使用你写的 UnionFind 进行判断与合并
            if (!uf.connected(p1, p2)) {
                uf.union(p1, p2);
                // 拆墙：设为通路，并标记为一次“动作”以触发统计和 UI 刷新
                maze.setCellState(wr, wc, 0, true);
            }
        }
    }
}