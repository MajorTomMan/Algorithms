package com.majortom.algorithms.core.runtime;

import java.util.Map;

/**
 * 算法运行统计快照。
 *
 * <p>它是 {@link ExecutionStats} 在某一时刻的只读记录。每个 {@link ExecutionFrame}
 * 都持有一份快照，最终的 {@link ExecutionRecord#summary()} 也使用这个类型。</p>
 *
 * @param startTimeMillis 执行开始时间
 * @param endTimeMillis 执行结束时间；未结束时为 0
 * @param durationMillis 当前持续时间
 * @param frameCount 已产生帧数
 * @param compareCount 比较次数
 * @param actionCount 动作次数
 * @param extras 扩展统计项
 */
public record ExecutionStatsSnapshot(
        long startTimeMillis,
        long endTimeMillis,
        long durationMillis,
        long frameCount,
        int compareCount,
        int actionCount,
        Map<String, Object> extras) {

    /**
     * 创建空统计快照。
     *
     * @return 所有计数均为 0 的快照
     */
    public static ExecutionStatsSnapshot empty() {
        return new ExecutionStatsSnapshot(0L, 0L, 0L, 0L, 0, 0, Map.of());
    }
}
