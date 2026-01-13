package com.majortom.algorithms.core.maze.impl;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.constants.MazeConstant;
import java.util.Arrays;
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
        // 🚩 修正同步：通知监听器，数据已重置（通常用于刷新 Canvas 背景）
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
        // 边界保护：越界视为墙，避免寻路算法抛出 ArrayIndexOutOfBoundsException
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

        // 🚩 使用常量代替硬编码数字：3->START, 5->END
        setCellState(startR, startC, MazeConstant.START, false);
        setCellState(endR, endC, MazeConstant.END, false);
    }

    @Override
    public void initialSilent() {
        this.isGenerated = false;
        for (int i = 0; i < rows; i++) {
            Arrays.fill(this.data[i], MazeConstant.WALL);
        }
        // 重置 BaseStructure 中的统计量
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
                // 🚩 只要不是墙，且不是起点终点，都统一还原为普通的路
                // 这样能清除生成算法留下的“已访问”、“正在访问”等临时颜色
                if (state != MazeConstant.WALL && state != MazeConstant.START && state != MazeConstant.END) {
                    data[i][j] = MazeConstant.ROAD;
                }
            }
        }
        // 💡 记得重置统计量，让寻路算法重新计算“已访问节点”
        this.actionCount = 0;
    }

    @Override
    public void pickRandomPointsOnAvailablePaths() {
        // 1. 搜集当前地图上所有已经是 ROAD 的坐标
        java.util.List<int[]> availablePaths = new java.util.ArrayList<>();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (data[i][j] == MazeConstant.ROAD) {
                    availablePaths.add(new int[] { i, j });
                }
            }
        }

        // 2. 如果路太少（比如刚开始生成就点了寻路），则直接调用原有的逻辑或退出
        if (availablePaths.size() < 2) {
            System.err.println("System: Not enough paths to pick points.");
            return;
        }

        // 3. 随机抽取两个不重复的索引
        Random rand = new Random();
        int startIndex = rand.nextInt(availablePaths.size());
        int endIndex;
        do {
            endIndex = rand.nextInt(availablePaths.size());
        } while (startIndex == endIndex);

        int[] startPos = availablePaths.get(startIndex);
        int[] endPos = availablePaths.get(endIndex);

        // 4. 设置状态，这里 false 表示不需要步进动画，瞬间设置完成
        setCellState(startPos[0], startPos[1], MazeConstant.START, false);
        setCellState(endPos[0], endPos[1], MazeConstant.END, false);
    }
}