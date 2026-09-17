package com.majortom.algorithms.visualization.animation.api;

/** Shared first-generation timing profile. Durations live in the animation layer, never Visualizers. */
public final class AnimationTimings {
    public static final double EDGE_REMOVE_MS = 140.0d;
    public static final double EDGE_CREATE_DELAY_MS = 80.0d;
    public static final double EDGE_CREATE_MS = 190.0d;
    public static final double EDGE_MORPH_DELAY_MS = 45.0d;
    public static final double EDGE_MORPH_MS = 250.0d;
    public static final double NODE_MOVE_DELAY_MS = 65.0d;
    public static final double NODE_MOVE_MS = 250.0d;
    public static final double NODE_ENTER_DELAY_MS = 85.0d;
    public static final double NODE_ENTER_MS = 210.0d;
    public static final double NODE_EXIT_MS = 175.0d;
    public static final double VALUE_CHANGE_DELAY_MS = 80.0d;
    public static final double VALUE_CHANGE_MS = 170.0d;
    public static final double INTERRUPTED_SETTLE_MS = 180.0d;

    private AnimationTimings() {}
}
