package com.majortom.algorithms.core.maze;

import java.util.Random;

import com.majortom.algorithms.core.base.BaseAlgorithm;

/**
 * 适配后的迷宫算法基类
 * 继承自 BaseAlgorithm，数据快照为 int[][]
 */
public abstract class BaseMaze extends BaseAlgorithm<int[][]> {
    // 单元格类型定义保持不变
    public static final int PATH = 0;
    public static final int WALL = 1;
    public static final int START = 2;
    public static final int END = 3;

    protected final int rows;
    protected final int cols;
    protected final int[][] map;
    protected final Random random = new Random();

    // 记录起终点坐标
    protected int startRow, startCol;
    protected int endRow, endCol;

    public BaseMaze(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.map = new int[rows][cols];
        initial();
    }

    /**
     * 初始化地图，默认填充为墙
     */
    public void initial() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                map[r][c] = WALL;
            }
        }
    }

    /**
     * 核心适配：更新单元格状态并触发同步
     * 对应原来的 setCell，但现在改用 sync 机制
     */
    protected void setCell(int r, int c, int type) {
        if (!isOutOfIndex(r, c)) {
            map[r][c] = type;

            // 逻辑映射：
            // 拆墙/设路径 -> actionCount (操作)
            // 设起点/终点 -> compareCount (探测/标记)
            if (type == PATH)
                actionCount++;
            else
                compareCount++;

            // 同步到 UI。焦点 a, b 传入当前的行和列坐标
            sync(map, r, c);
        }
    }

    // --- 适配统计方法，对齐顶级基类命名 ---

    protected void incrementCheck() {
        compareCount++;
        // sync(map, null, null); // 如果需要实时更新统计数字而不改图，可以单独同步
    }

    protected void incrementBreak() {
        actionCount++;
    }

    // --- 业务接口保持抽象 ---
    public abstract void generate();

    public abstract boolean isConnected();

    // --- 工具方法 ---
    protected int getCell(int r, int c) {
        return isOutOfIndex(r, c) ? WALL : map[r][c];
    }

    protected boolean isOutOfIndex(int r, int c) {
        return r < 0 || r >= rows || c < 0 || c >= cols;
    }

    public int[][] getMap() {
        return map;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }
}