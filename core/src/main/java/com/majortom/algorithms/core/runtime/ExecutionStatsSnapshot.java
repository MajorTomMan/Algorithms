package com.majortom.algorithms.core.runtime;

import java.util.Map;

public record ExecutionStatsSnapshot(
        long startTimeMillis,
        long endTimeMillis,
        long durationMillis,
        long frameCount,
        int compareCount,
        int actionCount,
        Map<String, Object> extras) {

    public static ExecutionStatsSnapshot empty() {
        return new ExecutionStatsSnapshot(0L, 0L, 0L, 0L, 0, 0, Map.of());
    }
}
