package com.majortom.algorithms.core.maze.impl;

import com.majortom.algorithms.core.maze.BaseMaze;
import java.util.Arrays;
import java.util.Random;

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
        resetStatistics();
        this.sync(data, null, null);
    }

    /**
     * 关键适配：当 Controller 调用 algorithm.run(data) 时，
     * 我们需要通过这个 run 方法把控制权交给具体的生成策略。
     */
    @Override
    public void run(int[][] data) {
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
            return 1;
        return data[r][c];
    }

    @Override
    public boolean isOverBorder(int r, int c) {
        return r < 0 || r >= rows || c < 0 || c >= cols;
    }

    @Override
    public void pickRandomPoints() {
        Random rand = new Random();
        int startR, startC, endR, endC;
        do {
            startR = rand.nextInt(rows);
            startC = rand.nextInt(cols);
        } while (getCell(startR, startC) != 0);

        do {
            endR = rand.nextInt(rows);
            endC = rand.nextInt(cols);
        } while (getCell(endR, endC) != 0 || (endR == startR && endC == startC));

        setCellState(startR, startC, 3, false);
        setCellState(endR, endC, 5, false);
    }
}