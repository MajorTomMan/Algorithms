package com.majortom.algorithms.core.maze;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.runtime.ExecutionContext;

/**
 * 迷宫实体抽象基类。
 *
 * <p>迷宫和其他结构略有不同：格子状态变化非常频繁，所以 {@link #setCellState(int, int, int, boolean)}
 * 会在结构内部直接调用 {@link ExecutionContext#sync}。迷宫生成器和寻路器只需要修改格子，
 * 不需要每一步手动调用算法基类的 sync。</p>
 *
 * @param <T> 迷宫内部数据类型，例如二维数组或图结构
 */
public abstract class BaseMaze<T> extends BaseStructure<T> {

    /**
     * 迷宫核心数据模型。
     */
    protected T data; // 核心数据模型（由子类在 initial 时实例化）

    /**
     * 迷宫行数。
     */
    protected int rows;

    /**
     * 迷宫列数。
     */
    protected int cols;

    /**
     * 迷宫是否已经完成生成。
     */
    protected boolean isGenerated;

    /**
     * 迷宫实体持有的执行上下文，用于格子变化时直接发出同步帧。
     */
    protected ExecutionContext<BaseMaze<T>> executionContext;

    /**
     * 创建迷宫实体。
     *
     * @param rows 行数
     * @param cols 列数
     */
    public BaseMaze(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    /**
     * 接收来自算法层的执行上下文。
     *
     * @param executionContext 执行上下文
     */
    public void setExecutionContext(ExecutionContext<BaseMaze<T>> executionContext) {
        this.executionContext = executionContext;
    }

    /**
     * 修改单个单元格状态并自动触发统计与同步。
     *
     * @param r 行坐标
     * @param c 列坐标
     * @param type 新单元格类型
     * @param isAction true 表示结构动作，false 表示访问/比较
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

        if (executionContext != null) {
            executionContext.sync(this, r, c, null, isAction);
        }
    }

    /**
     * 获取迷宫底层数据。
     *
     * @return 迷宫数据
     */
    @Override
    public T getData() {
        return data;
    }

    /**
     * 重置统计并静默重新初始化迷宫。
     */
    @Override
    public void resetStatistics() {
        // TODO Auto-generated method stub
        super.resetStatistics();
        initialSilent();
    }

    /**
     * 初始化迷宫，并允许产生必要的可视化状态。
     */
    public abstract void initial();

    /**
     * 静默初始化迷宫，不主动产生算法帧。
     */
    public abstract void initialSilent();

    /**
     * 更新底层数据中的单元格状态。
     *
     * @param r 行坐标
     * @param c 列坐标
     * @param type 新单元格类型
     */
    protected abstract void updateInternalData(int r, int c, int type);

    /**
     * 读取单元格类型。
     *
     * @param r 行坐标
     * @param c 列坐标
     * @return 单元格类型
     */
    public abstract int getCell(int r, int c);

    /**
     * 随机选择迷宫起点和终点。
     */
    public abstract void pickRandomPoints();

    /**
     * 判断坐标是否越界。
     *
     * @param r 行坐标
     * @param c 列坐标
     * @return 越界时返回 true
     */
    public boolean isOverBorder(int r, int c) {
        return r < 0 || r >= rows || c < 0 || c >= cols;
    }

    /**
     * 获取迷宫行数。
     *
     * @return 行数
     */
    public int getRows() {
        return rows;
    }

    /**
     * 获取迷宫列数。
     *
     * @return 列数
     */
    public int getCols() {
        return cols;
    }

    /**
     * 设置迷宫生成状态。
     *
     * @param isGenerated 是否已生成
     */
    public void setGenerated(boolean isGenerated) {
        this.isGenerated = isGenerated;
    }

    /**
     * 判断迷宫是否已生成。
     *
     * @return 已生成时返回 true
     */
    public boolean isGenerated() {
        return isGenerated;
    }

    /**
     * 将通用状态复制到迷宫副本。
     *
     * @param target 目标副本
     */
    protected void copyStateTo(BaseMaze<T> target) {
        target.isGenerated = this.isGenerated;
        target.actionCount = this.actionCount;
        target.compareCount = this.compareCount;
    }

    /**
     * 创建迷宫快照。
     *
     * @return 迷宫快照
     */
    @Override
    public abstract BaseMaze<T> copy();

    /**
     * 清除寻路过程中的临时可视化状态。
     */
    public abstract void clearVisualStates();

    /**
     * 在可通行路径上重新选择随机起点和终点。
     */
    public abstract void pickRandomPointsOnAvailablePaths();
}
