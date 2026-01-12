package com.majortom.algorithms.core.maze.impl;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.constants.MazeConstant;

import java.util.Arrays;
import java.util.Random;

public class ArrayMaze extends BaseMaze<int[][]> {

    public ArrayMaze(int rows, int cols) {
        super(rows, cols);
        this.data = new int[rows][cols];
    }

    @Override
    public void initial() {
        initialSilent();
        // 关键点：初始化后发送一次全图信号，让 UI 刷出满墙背景
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

    @Override
    public int getCell(int r, int c) {
        return isOverBorder(r, c) ? MazeConstant.WALL : data[r][c];
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

    @Override
    public void initialSilent() {
        for (int i = 0; i < rows; i++) {
            Arrays.fill(this.data[i], MazeConstant.WALL); // 默认填充为墙
        }
        resetStatistics();
    }
}