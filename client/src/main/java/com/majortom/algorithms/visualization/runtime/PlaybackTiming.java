package com.majortom.algorithms.visualization.runtime;

/** Shared playback/animation speed bounds, independent from individual animation durations. */
public final class PlaybackTiming {
    public static final long DEFAULT_FRAME_DELAY_MILLIS = 100L;
    public static final double MIN_SPEED = 0.05d;
    public static final double MAX_SPEED = 32.0d;

    private PlaybackTiming() {}

    public static double clampSpeed(double speed) {
        return Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
    }
}
