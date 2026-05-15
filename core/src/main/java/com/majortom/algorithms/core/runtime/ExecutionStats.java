package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.base.BaseStructure;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 算法运行统计累加器。
 *
 * <p>它是可变对象，由 {@link ExecutionContext} 在每次同步点更新。
 * 对外展示或写入执行帧时，通过 {@link #snapshot()} 生成不可变语义的
 * {@link ExecutionStatsSnapshot}。</p>
 */
public class ExecutionStats {

    /**
     * 执行开始时间。
     */
    private long startTimeMillis;

    /**
     * 执行结束时间，未结束时为 0。
     */
    private long endTimeMillis;

    /**
     * 已产生的执行帧数量。
     */
    private long frameCount;

    /**
     * 从结构同步来的比较次数。
     */
    private int compareCount;

    /**
     * 从结构同步来的动作次数，例如交换、插入、访问等。
     */
    private int actionCount;

    /**
     * 额外统计项，给未来算法或模块扩展使用。
     */
    private final Map<String, Object> extras = new LinkedHashMap<>();

    /**
     * 标记执行开始。
     */
    public void markStarted() {
        startTimeMillis = System.currentTimeMillis();
        endTimeMillis = 0L;
    }

    /**
     * 标记执行结束。
     */
    public void markEnded() {
        endTimeMillis = System.currentTimeMillis();
    }

    /**
     * 从当前结构同步统计数据。
     *
     * @param structure 当前算法结构
     */
    public void syncFrom(BaseStructure<?> structure) {
        if (structure == null) {
            return;
        }
        compareCount = structure.getCompareCount();
        actionCount = structure.getActionCount();
        frameCount++;
    }

    /**
     * 添加扩展统计项。
     *
     * @param key 统计项名称
     * @param value 统计项值
     */
    public void putExtra(String key, Object value) {
        extras.put(key, value);
    }

    /**
     * 创建当前统计快照。
     *
     * @return 当前统计快照
     */
    public ExecutionStatsSnapshot snapshot() {
        long effectiveEnd = (endTimeMillis > 0L) ? endTimeMillis : System.currentTimeMillis();
        long duration = (startTimeMillis > 0L) ? Math.max(0L, effectiveEnd - startTimeMillis) : 0L;
        return new ExecutionStatsSnapshot(
                startTimeMillis,
                endTimeMillis,
                duration,
                frameCount,
                compareCount,
                actionCount,
                Map.copyOf(extras));
    }
}
