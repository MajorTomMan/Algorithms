package com.majortom.algorithms.core.maze.impl;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.constants.MazeConstant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 迷宫数据实体的具体实现
 * 职责：维护 int[][] 核心矩阵，提供边界检查与坐标随机化逻辑。
 */
public class ArrayMaze extends BaseMaze<int[][]> {

    public ArrayMaze(int rows, int cols) {
        super(rows, cols);
        // 初始化时直接实例化数据模型
        this.data = new int[rows][cols];
        initialSilent();
    }

    @Override
    public void initial() {
        initialSilent();
        if (syncListener != null) {
            syncListener.onSync(this, -1, -1, 0, 0);
        }
    }

    @Override
    protected void updateInternalData(int r, int c, int type) {
        if (!isOverBorder(r, c)) {
            this.data[r][c] = type;
        }
    }

    @Override
    public int getCell(int r, int c) {
        return isOverBorder(r, c) ? MazeConstant.WALL : data[r][c];
    }

    public void setGenerated(boolean generated) {
        this.isGenerated = generated;
    }

    @Override
    public void pickRandomPoints() {
        if (!isGenerated && !hasEnoughSpace())
            return;

        Random rand = new Random();
        int startR, startC, endR, endC;

        // 寻找起点
        do {
            startR = rand.nextInt(rows);
            startC = rand.nextInt(cols);
        } while (getCell(startR, startC) != MazeConstant.ROAD);

        // 寻找终点
        do {
            endR = rand.nextInt(rows);
            endC = rand.nextInt(cols);
        } while (getCell(endR, endC) != MazeConstant.ROAD || (endR == startR && endC == startC));

        setCellState(startR, startC, MazeConstant.START, false);
        setCellState(endR, endC, MazeConstant.END, false);
    }

    @Override
    public void initialSilent() {
        this.isGenerated = false;
        for (int i = 0; i < rows; i++) {
            Arrays.fill(this.data[i], MazeConstant.WALL);
        }

        this.compareCount = 0;
        this.actionCount = 0;
    }

    private boolean hasEnoughSpace() {
        int count = 0;
        for (int[] row : data) {
            for (int cell : row) {
                if (cell == MazeConstant.ROAD)
                    count++;
                if (count >= 2)
                    return true;
            }
        }
        return false;
    }

    @Override
    public void clearVisualStates() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int state = data[i][j];
                if (state != MazeConstant.WALL && state != MazeConstant.START && state != MazeConstant.END) {
                    data[i][j] = MazeConstant.ROAD;
                }
            }
        }
        this.actionCount = 0;
    }

    @Override
    public void pickRandomPointsOnAvailablePaths() {
        List<int[]> roads = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (data[r][c] == MazeConstant.ROAD)
                    roads.add(new int[] { r, c });
            }
        }
        if (roads.size() < 2)
            return;

        Collections.shuffle(roads); // 只有在后台线程这么干才不卡
        int[] s = roads.get(0);
        int[] e = roads.get(1);

        setCellState(s[0], s[1], MazeConstant.START, false);
        setCellState(e[0], e[1], MazeConstant.END, false);
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        initial();
    }
}