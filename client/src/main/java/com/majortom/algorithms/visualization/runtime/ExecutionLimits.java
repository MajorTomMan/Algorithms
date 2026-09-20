package com.majortom.algorithms.visualization.runtime;

/** Client-side bounds: the live queue defaults to the per-run event budget. */
public final class ExecutionLimits {
    public static final int DEFAULT_MAXIMUM_EVENT_COUNT = 200_000;
    public static final int DEFAULT_LIVE_QUEUE_CAPACITY = DEFAULT_MAXIMUM_EVENT_COUNT;

    private ExecutionLimits() {}
}
