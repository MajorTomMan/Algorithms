package com.majortom.algorithms.core.maze;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.base.listener.StepListener;
import com.majortom.algorithms.core.base.listener.SyncListener;

/**
 * 迷宫实体抽象基类
 * 职责：
 * 1. 提供统一的原子化操作接口，并自动更新统计量。
 * 2. 具备“发信号”能力，将内部变化透传给算法基类，最终触达 UI。
 */
public abstract class BaseMaze<T> extends BaseStructure<T> {

    protected T data; // 核心数据模型（由子类在 initial 时实例化）
    protected int rows;
    protected int cols;
    protected boolean isGenerated;
    // 为了让 Maze 内部能主动触发渲染，我们需要持有监听器引用
    protected SyncListener<? super BaseMaze<T>> syncListener;
    protected StepListener stepListener;

    public BaseMaze(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    /**
     * 接收来自 Algorithm 的环境授权
     */
    public void setEnvironment(SyncListener<? super BaseMaze<T>> sync, StepListener step) {
        this.syncListener = sync;
        this.stepListener = step;
    }

    /**
     * 修改单个单元格状态并自动触发统计与同步
     */
    public void setCellState(int r, int c, int type, boolean isAction) {
        // 1. 修改内存数据
        updateInternalData(r, c, type);

        // 2. 自动统计
        if (isAction) {
            incrementAction();
        } else {
            incrementCompare();
        }

        if (syncListener != null) {
            syncListener.onSync(this, r, c, getCompareCount(), getActionCount());
        }
        if (stepListener != null && isAction) {
            stepListener.onStep();
        }
    }

    @Override
    public T getData() {
        return data;
    }

    @Override
    public void reset() {
        this.actionCount = 0;
        this.compareCount = 0;
        initialSilent(); // 静默重置数据
    }

    // --- 抽象方法保持不变 ---
    public abstract void initial();

    public abstract void initialSilent();

    protected abstract void updateInternalData(int r, int c, int type);

    public abstract int getCell(int r, int c);

    public abstract void pickRandomPoints();

    // --- Getter & Helper ---
    public boolean isOverBorder(int r, int c) {
        return r < 0 || r >= rows || c < 0 || c >= cols;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public void setGenerated(boolean isGenerated) {
        this.isGenerated = isGenerated;
    }

    public abstract void clearVisualStates();

    public abstract void pickRandomPointsOnAvailablePaths();
}