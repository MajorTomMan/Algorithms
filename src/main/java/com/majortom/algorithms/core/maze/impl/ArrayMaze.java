package com.majortom.algorithms.core.maze.impl;

import com.majortom.algorithms.core.maze.BaseMaze;
import java.util.Arrays;
import java.util.Random;

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


    @Override
    protected void updateInternalData(int r, int c, int type) {
        if (!isOverBorder(r, c)) {
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
        if (isOverBorder(r, c))
            return 1; // 越界视为墙
        return data[r][c];
    }

    @Override
    public void pickRandomPoints() {
        int rows = getRows();
        int cols = getCols();
        Random rand = new Random();

        // 1. 寻找起点：找一个状态为 0 (路) 的随机格
        int startR, startC;
        do {
            startR = rand.nextInt(rows);
            startC = rand.nextInt(cols);
        } while (getCell(startR, startC) != 0);

        // 2. 寻找终点：找另一个状态为 0 且不等于起点的随机格
        int endR, endC;
        do {
            endR = rand.nextInt(rows);
            endC = rand.nextInt(cols);
        } while (getCell(endR, endC) != 0 || (endR == startR && endC == startC));

        // 3. 设置状态 (3:起点, 5:终点)
        // 这里 false 表示不计入算法步数统计，仅仅是标记状态
        setCellState(startR, startC, 3, false);
        setCellState(endR, endC, 5, false);
    }

    @Override
    public boolean isOverBorder(int r, int c) {
        // TODO Auto-generated method stub
        // 如果行小于0，或超过最大行；或列小于0，或超过最大列，即为越界
        return r < 0 || r >= rows || c < 0 || c >= cols;
    }
}