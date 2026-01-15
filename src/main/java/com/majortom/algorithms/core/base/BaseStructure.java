package com.majortom.algorithms.core.base;

/**
 * 基础数据结构实体
 * 职责：维护原始数据、封装原子统计行为。
 * 仅作为数据容器与状态记录器。
 */
public abstract class BaseStructure<D> {

    // 算法执行过程中的原子统计量
    protected int compareCount = 0; // 比较次数 (如：a < b)
    protected int actionCount = 0; // 操作/步进次数 (如：swap, visit)

    /**
     * 获取封装的物理数据实体
     * 
     * @return D 泛型数据
     */
    public abstract D getData();

    /**
     * 原子增加：比较计数
     */
    public final void incrementCompare() {
        this.compareCount++;
    }

    /**
     * 原子增加：操作计数
     */
    public final void incrementAction() {
        this.actionCount++;
    }

    /**
     * 批量增加操作计数 (适用于路径寻优等步长不为 1 的场景)
     */
    public final void addActions(int delta) {
        this.actionCount += delta;
    }

    public final int getCompareCount() {
        return compareCount;
    }

    public final int getActionCount() {
        return actionCount;
    }

    /**
     * 核心重置逻辑
     * 清除所有统计信息，并允许子类清理特定的视觉状态标识
     */
    public void resetStatistics() {
        this.compareCount = 0;
        this.actionCount = 0;
    }

    /**
     * 物理结构重置 (由子类实现，如清空迷宫、释放图节点)
     */
    public abstract void clear();

    /**
     * 默认重置行为：同时清理统计量与物理结构
     */
    public void resetAll() {
        resetStatistics();
        clear();
    }
}