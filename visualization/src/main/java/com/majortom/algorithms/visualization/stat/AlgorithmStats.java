package com.majortom.algorithms.visualization.stat;

/**
 * 旧版算法统计数据容器。
 *
 * <p>当前主链路已经逐步收敛到 core 层的 ExecutionStats/ExecutionStatsSnapshot。
 * 这个类保留给仍可能引用旧统计模型的可视化代码，后续如彻底迁移可再删除。</p>
 */
public class AlgorithmStats {
    /**
     * 执行开始时间。
     */
    public long startTime;

    /**
     * 执行结束时间。
     */
    public long endTime;

    /**
     * 比较次数。
     */
    public int compareCount;

    /**
     * 动作次数，例如交换、移动、标记等操作。
     */
    public int actionCount; // 如交换、移动、标记等操作

    /**
     * 计算当前持续时间。
     *
     * @return 如果已结束返回结束时间差，否则返回到当前时刻的持续时间
     */
    public long getDuration() {
        return (endTime > startTime) ? (endTime - startTime) : (System.currentTimeMillis() - startTime);
    }
}
