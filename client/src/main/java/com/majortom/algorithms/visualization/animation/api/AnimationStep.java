package com.majortom.algorithms.visualization.animation.api;

import java.util.Objects;

/** JavaFX-neutral visual transition primitive. Structure-specific planners compose these only. */
public sealed interface AnimationStep
        permits AnimationStep.NodeMove, AnimationStep.NodeEnter, AnimationStep.NodeExit,
                AnimationStep.EdgeCreate, AnimationStep.EdgeRemove, AnimationStep.EdgeMorph,
                AnimationStep.ValueChange {
    String targetId();

    record NodeMove(String targetId) implements AnimationStep {
        public NodeMove { Objects.requireNonNull(targetId, "targetId"); }
    }

    record NodeEnter(String targetId) implements AnimationStep {
        public NodeEnter { Objects.requireNonNull(targetId, "targetId"); }
    }

    record NodeExit(String targetId) implements AnimationStep {
        public NodeExit { Objects.requireNonNull(targetId, "targetId"); }
    }

    record EdgeCreate(String targetId) implements AnimationStep {
        public EdgeCreate { Objects.requireNonNull(targetId, "targetId"); }
    }

    record EdgeRemove(String targetId) implements AnimationStep {
        public EdgeRemove { Objects.requireNonNull(targetId, "targetId"); }
    }

    /** Reserved for a route that keeps the same logical edge identity while its path changes. */
    record EdgeMorph(String targetId) implements AnimationStep {
        public EdgeMorph { Objects.requireNonNull(targetId, "targetId"); }
    }

    record ValueChange(String targetId) implements AnimationStep {
        public ValueChange { Objects.requireNonNull(targetId, "targetId"); }
    }
}
