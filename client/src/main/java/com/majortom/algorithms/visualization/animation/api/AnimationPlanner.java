package com.majortom.algorithms.visualization.animation.api;

import com.majortom.algorithms.visualization.render.api.LayoutPatch;

/** JavaFX-neutral planner from factual state/layout differences to visual transition primitives. */
@FunctionalInterface
public interface AnimationPlanner<S> {
    AnimationPlan plan(S previousState, LayoutPatch previousLayout, S nextState, LayoutPatch nextLayout);
}
