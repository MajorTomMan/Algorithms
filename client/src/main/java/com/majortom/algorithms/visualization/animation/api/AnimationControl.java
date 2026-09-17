package com.majortom.algorithms.visualization.animation.api;

/** Unified playback/lifecycle control for a structure animation runtime. */
public interface AnimationControl {
    AnimationControl NONE = new AnimationControl() {};

    default void setSpeed(double speed) {}
    default void setScrubbing(boolean scrubbing) {}
    default void pause() {}
    default void resume() {}
    default void step() {}
    default void finishImmediately() {}
    default void reset() {}
    default void dispose() {}
    default boolean isAnimating() { return false; }
}
