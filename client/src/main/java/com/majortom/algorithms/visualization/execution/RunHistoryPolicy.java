package com.majortom.algorithms.visualization.execution;

/** Bounds the number of retained local runs and their total authoritative event count. */
public record RunHistoryPolicy(int maximumRuns, long maximumEvents) {

    public RunHistoryPolicy {
        if (maximumRuns <= 0) {
            throw new IllegalArgumentException("maximumRuns must be positive");
        }
        if (maximumEvents <= 0L) {
            throw new IllegalArgumentException("maximumEvents must be positive");
        }
    }

    public static RunHistoryPolicy desktopDefault() {
        return new RunHistoryPolicy(20, 500_000L);
    }
}
