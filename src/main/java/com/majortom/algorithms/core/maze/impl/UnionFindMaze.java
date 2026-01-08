
package com.majortom.algorithms.core.maze.impl;

import com.majortom.algorithms.core.basic.UnionFind;
import com.majortom.algorithms.core.maze.BaseMaze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于并查集（Kruskal算法）的迷宫生成实现
 * 特点：生成的迷宫分岔极多，没有明显的长廊，呈现出一种自然的破碎美感。
 */
public class UnionFindMaze extends BaseMaze {

    private final UnionFind uf;

    public UnionFindMaze(int rows, int cols) {
        super(rows, cols);
        // 并查集大小为整个地图的单元格总数
        this.uf = new UnionFind(rows * cols);
    }

    @Override
    public void generate() {
        // 1. 预处理：收集所有可以被拆除的“候选墙”
        // 在网格系统中，只有坐标为 (奇, 偶) 或 (偶, 奇) 的点才是潜在的墙壁
        List<int[]> wallCandidates = new ArrayList<>();
        for (int r = 1; r < getRows() - 1; r++) {
            for (int c = 1; c < getCols() - 1; c++) {
                if ((r % 2 == 1 && c % 2 == 0) || (r % 2 == 0 && c % 2 == 1)) {
                    wallCandidates.add(new int[]{r, c});
                }
            }
        }

        // 2. 核心逻辑：随机打乱墙壁顺序，实现随机生成树
        Collections.shuffle(wallCandidates);

        // 3. 遍历每一面墙，尝试打通
        for (int[] wall : wallCandidates) {
            int wr = wall[0];
            int wc = wall[1];

            // 确定这堵墙连接的两个相邻单元格 (r1, c1) 和 (r2, c2)
            int r1, c1, r2, c2;
            if (wr % 2 == 1) { // 垂直方向的墙，连接左右两个单元格
                r1 = wr; c1 = wc - 1;
                r2 = wr; c2 = wc + 1;
            } else { // 水平方向的墙，连接上下两个单元格
                r1 = wr - 1; c1 = wc;
                r2 = wr + 1; c2 = wc;
            }

            // 将二维坐标映射为并查集的一维索引
            int id1 = r1 * getCols() + c1;
            int id2 = r2 * getCols() + c2;

            // 如果这两个单元格当前还不连通（分属于不同的集合）
            if (uf.find(id1) != uf.find(id2)) {
                // 将它们在逻辑上合并
                uf.union(id1, id2);
                
                // 在物理地图上打通：将两个单元格及其之间的墙壁设为 PATH
                setCell(r1, c1, PATH); // 触发步进回调
                setCell(r2, c2, PATH); // 触发步进回调
                setCell(wr, wc, PATH); // 触发步进回调
            }
        }

        // 4. 初始化起点与终点位置
        this.startRow = 1; this.startCol = 1;
        this.endRow = (getRows() % 2 == 0) ? getRows() - 2 : getRows() - 2;
        this.endCol = (getCols() % 2 == 0) ? getCols() - 2 : getCols() - 2;
        
        setCell(startRow, startCol, START);
        setCell(endRow, endCol, END);
    }

    @Override
    public boolean isConnected() {
        // 检查起点和终点在并查集中是否处于同一个集合
        int startId = startRow * getCols() + startCol;
        int endId = endRow * getCols() + endCol;
        return uf.connected(startId, endId);
    }
}