package com.majortom.algorithms.core.base;

import java.util.HashMap;
import java.util.Map;

/**
 * 基础数据结构实体
 * 职责：维护原始数据、封装原子统计行为。
 * 仅作为数据容器与状态记录器。
 */
public abstract class BaseStructure<D> {

    // 算法执行过程中的原子统计量
    protected int compareCount = 0;
    protected int actionCount = 0;

    // --- 预留接口：性能与扩展数据 ---
    protected long timeElapsedMs = 0; // 耗时统计 (单位：毫秒)
    protected Map<String, Object> extraMetrics = new HashMap<>(); // 扩展指标预留池

    /**
     * 获取封装的物理数据实体
     */
    public abstract D getData();

    /**
     * 深拷贝方法
     */
    public abstract BaseStructure<D> copy();

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

    public final void addActions(int delta) {
        this.actionCount += delta;
    }

    public final void setTimeElapsed(long ms) {
        this.timeElapsedMs = ms;
    }

    public final long getTimeElapsed() {
        return timeElapsedMs;
    }

    public final void putMetric(String key, Object value) {
        this.extraMetrics.put(key, value);
    }

    public final Object getMetric(String key) {
        return this.extraMetrics.get(key);
    }

    public final int getCompareCount() {
        return compareCount;
    }

    public final int getActionCount() {
        return actionCount;
    }

    /**
     * 核心重置逻辑
     */
    public void resetStatistics() {
        this.compareCount = 0;
        this.actionCount = 0;
        this.timeElapsedMs = 0;
        this.extraMetrics.clear();
    }

    /**
     * 物理结构重置
     */
    public abstract void clear();

    /**
     * 默认重置行为
     */
    public void resetAll() {
        resetStatistics();
        clear();
    }
}