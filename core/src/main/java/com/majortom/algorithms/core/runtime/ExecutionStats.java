package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.base.BaseStructure;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExecutionStats {

    private long startTimeMillis;
    private long endTimeMillis;
    private long frameCount;
    private int compareCount;
    private int actionCount;
    private final Map<String, Object> extras = new LinkedHashMap<>();

    public void markStarted() {
        startTimeMillis = System.currentTimeMillis();
        endTimeMillis = 0L;
    }

    public void markEnded() {
        endTimeMillis = System.currentTimeMillis();
    }

    public void syncFrom(BaseStructure<?> structure) {
        if (structure == null) {
            return;
        }
        compareCount = structure.getCompareCount();
        actionCount = structure.getActionCount();
        frameCount++;
    }

    public void putExtra(String key, Object value) {
        extras.put(key, value);
    }

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
