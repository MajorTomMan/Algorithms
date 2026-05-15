package com.majortom.algorithms.core.base;

import java.util.HashMap;
import java.util.Map;

/**
 * 可被算法执行层记录和可视化层渲染的数据结构基类。
 *
 * <p>排序数组、树、图、迷宫等核心结构都继承它。算法在结构上执行原子操作时，
 * 通过 {@link #incrementCompare()} 和 {@link #incrementAction()} 更新统计；
 * {@code ExecutionContext} 在同步点读取这些统计，并把 {@link #copy()} 返回的快照写入时间轴。</p>
 *
 * @param <D> 底层真实数据类型，例如数组、树节点、GraphStream 图或迷宫矩阵
 */
public abstract class BaseStructure<D> {

    /**
     * 算法执行过程中的比较/访问次数。
     */
    protected int compareCount = 0;

    /**
     * 算法执行过程中的结构修改/动作次数。
     */
    protected int actionCount = 0;

    /**
     * 预留耗时统计字段，单位毫秒。
     */
    protected long timeElapsedMs = 0; // 耗时统计 (单位：毫秒)

    /**
     * 扩展指标池，用于暂存模块私有统计。
     */
    protected Map<String, Object> extraMetrics = new HashMap<>(); // 扩展指标预留池

    /**
     * 获取封装的底层数据实体。
     *
     * @return 底层数据
     */
    public abstract D getData();

    /**
     * 创建结构快照。
     *
     * <p>执行层会把返回值保存进 {@code ExecutionFrame}。实现类必须避免返回当前可变对象本身，
     * 否则后续算法步骤会污染历史帧。</p>
     *
     * @return 当前结构的独立快照
     */
    public abstract BaseStructure<D> copy();

    /**
     * 原子增加一次比较/访问计数。
     */
    public final void incrementCompare() {
        this.compareCount++;
    }

    /**
     * 原子增加一次动作/修改计数。
     */
    public final void incrementAction() {
        this.actionCount++;
    }

    /**
     * 批量增加动作计数。
     *
     * @param delta 增量
     */
    public final void addActions(int delta) {
        this.actionCount += delta;
    }

    /**
     * 设置结构侧记录的耗时。
     *
     * @param ms 耗时毫秒数
     */
    public final void setTimeElapsed(long ms) {
        this.timeElapsedMs = ms;
    }

    /**
     * 获取结构侧记录的耗时。
     *
     * @return 耗时毫秒数
     */
    public final long getTimeElapsed() {
        return timeElapsedMs;
    }

    /**
     * 写入扩展指标。
     *
     * @param key 指标名
     * @param value 指标值
     */
    public final void putMetric(String key, Object value) {
        this.extraMetrics.put(key, value);
    }

    /**
     * 读取扩展指标。
     *
     * @param key 指标名
     * @return 指标值
     */
    public final Object getMetric(String key) {
        return this.extraMetrics.get(key);
    }

    /**
     * 获取比较/访问次数。
     *
     * @return 比较次数
     */
    public final int getCompareCount() {
        return compareCount;
    }

    /**
     * 获取动作/修改次数。
     *
     * @return 动作次数
     */
    public final int getActionCount() {
        return actionCount;
    }

    /**
     * 重置统计信息，但不清空底层结构。
     */
    public void resetStatistics() {
        this.compareCount = 0;
        this.actionCount = 0;
        this.timeElapsedMs = 0;
        this.extraMetrics.clear();
    }

    /**
     * 清空底层物理结构。
     */
    public abstract void clear();

    /**
     * 同时重置统计和底层结构。
     */
    public void resetAll() {
        resetStatistics();
        clear();
    }
}
