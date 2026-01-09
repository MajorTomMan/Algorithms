package com.majortom.algorithms.core.maze.impl;

import com.majortom.algorithms.core.maze.BaseMaze;
import java.util.Arrays;
/**
 * 数组迷宫容器实现
 */
public class ArrayMaze extends BaseMaze<int[][]> {
    private final int rows;
    private final int cols;

    public ArrayMaze(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.data = new int[rows][cols];
        initial();
    }

    @Override
    public void initial() {
        for (int i = 0; i < rows; i++) {
            Arrays.fill(this.data[i], 1); // 1 代表墙
        }
        this.actionCount = 0;
        this.compareCount = 0;
        this.sync(data, null, null);
    }

    /**
     * 核心修复：添加越界检查方法
     */
    public boolean isOutOfIndex(int r, int c) {
        return r < 0 || r >= rows || c < 0 || c >= cols;
    }

    @Override
    protected void updateInternalData(int r, int c, int type) {
        if (!isOutOfIndex(r, c)) {
            this.data[r][c] = type;
        }
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getCell(int r, int c) {
        if (isOutOfIndex(r, c))
            return 1; // 越界视为墙
        return data[r][c];
    }
}