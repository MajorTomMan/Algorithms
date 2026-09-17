package com.majortom.algorithms.visualization.animation.api;

import java.util.Objects;

/** One primitive placed on a plan-local millisecond timeline. */
public record TimedAnimationStep(AnimationStep step, double startMillis, double durationMillis) {
    public TimedAnimationStep {
        Objects.requireNonNull(step, "step");
        if (!Double.isFinite(startMillis) || startMillis < 0.0d) {
            throw new IllegalArgumentException("startMillis must be finite and >= 0");
        }
        if (!Double.isFinite(durationMillis) || durationMillis < 0.0d) {
            throw new IllegalArgumentException("durationMillis must be finite and >= 0");
        }
    }

    public double endMillis() { return startMillis + durationMillis; }
}
