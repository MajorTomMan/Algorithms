package com.majortom.algorithms.core.base;

/**
 * 基础数据结构实体
 * 职责：维护数据本身的状态、统计信息，不涉及算法逻辑
 */
public abstract class BaseStructure<D> {

    // 基础统计量
    protected int compareCount = 0;
    protected int actionCount = 0;

    public void incrementCompare() {
        this.compareCount++;
    }

    public void incrementAction() {
        this.actionCount++;
    }

    public int getCompareCount() {
        return compareCount;
    }

    public int getActionCount() {
        return actionCount;
    }

    /**
     * 子类需实现：获取具体的物理数据
     */
    public abstract D getData();

    /**
     * 重置所有状态与统计
     */
    public void reset() {
        this.compareCount = 0;
        this.actionCount = 0;
    }
}