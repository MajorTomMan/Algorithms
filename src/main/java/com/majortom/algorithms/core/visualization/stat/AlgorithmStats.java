package com.majortom.algorithms.core.visualization.stat;

public class AlgorithmStats {
    public long startTime;
    public long endTime;
    public int compareCount;
    public int actionCount; // 如交换、移动、标记等操作

    public long getDuration() {
        return (endTime > startTime) ? (endTime - startTime) : (System.currentTimeMillis() - startTime);
    }
}