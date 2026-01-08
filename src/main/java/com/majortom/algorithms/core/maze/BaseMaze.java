package com.majortom.algorithms.core.maze;

import java.util.Random;

/**
 * 迷宫算法基类
 * 提供基础的数据结构管理、坐标边界检查以及步进式可视化控制机制。
 */
public abstract class BaseMaze {
    // 单元格类型定义
    public static final int PATH = 0;
    public static final int WALL = 1;
    public static final int START = 2;
    public static final int END = 3;

    private final int rows;
    private final int cols;
    private final int[][] map;
    protected final Random random = new Random();

    // 记录起终点坐标
    protected int startRow, startCol;
    protected int endRow, endCol;

    /**
     * 步进回调接口，用于通知 UI 更新
     */
    public interface StepListener {
        void onStep();
    }

    private StepListener listener;
    private final Object lock = new Object();
    private boolean paused = true;

    public BaseMaze(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.map = new int[rows][cols];
        initial();
    }

    /**
     * 抽象生成方法：子类需实现具体的迷宫生成逻辑（如 DFS, BFS, Kruskal 等）
     */
    public abstract void generate();

    /**
     * 抽象联通性检查：判断当前迷宫起终点是否联通
     */
    public abstract boolean isConnected();

    /**
     * 初始化地图，默认填充为墙
     */
    protected void initial() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                map[r][c] = WALL;
            }
        }
    }

    /**
     * 更新单元格状态，并触发步进阻塞以便观察生成过程
     * * @param r 行坐标
     * 
     * @param c    列坐标
     * @param type 单元格类型
     */
    protected void setCell(int r, int c, int type) {
        if (!isOutOfIndex(r, c)) {
            map[r][c] = type;
            if (listener != null) {
                listener.onStep(); // 触发 UI 重绘回调
                waitForSignal(); // 进入等待状态，由外部调用 nextStep 唤醒
            }
        }
    }

    /**
     * 外部控制入口：唤醒处于等待状态的算法线程，继续执行下一步
     */
    public void nextStep() {
        synchronized (lock) {
            paused = false;
            lock.notifyAll();
        }
    }

    /**
     * 内部阻塞方法：使当前线程进入等待状态
     */
    private void waitForSignal() {
        synchronized (lock) {
            paused = true;
            while (paused) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // --- 辅助工具方法 ---

    protected int getCell(int r, int c) {
        return isOutOfIndex(r, c) ? WALL : map[r][c];
    }

    protected boolean isOutOfIndex(int r, int c) {
        return r < 0 || r >= rows || c < 0 || c >= cols;
    }

    // --- Getters & Setters ---

    public void setStepListener(StepListener listener) {
        this.listener = listener;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int[][] getMap() {
        return map;
    }
}