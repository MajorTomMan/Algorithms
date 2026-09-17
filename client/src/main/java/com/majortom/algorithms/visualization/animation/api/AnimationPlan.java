package com.majortom.algorithms.visualization.animation.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable JavaFX-neutral transition plan. */
public record AnimationPlan(List<TimedAnimationStep> steps) {
    private static final AnimationPlan EMPTY = new AnimationPlan(List.of());

    public AnimationPlan {
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
    }

    public static AnimationPlan empty() { return EMPTY; }

    public boolean isEmpty() { return steps.isEmpty(); }

    public double totalDurationMillis() {
        return steps.stream().mapToDouble(TimedAnimationStep::endMillis).max().orElse(0.0d);
    }

    public boolean contains(Class<? extends AnimationStep> type, String targetId) {
        return steps.stream().anyMatch(timed -> type.isInstance(timed.step())
                && timed.step().targetId().equals(targetId));
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final List<TimedAnimationStep> steps = new ArrayList<>();

        public Builder add(AnimationStep step, double startMillis, double durationMillis) {
            steps.add(new TimedAnimationStep(step, startMillis, durationMillis));
            return this;
        }

        public AnimationPlan build() {
            return steps.isEmpty() ? EMPTY : new AnimationPlan(steps);
        }
    }
}
