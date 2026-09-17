package com.majortom.algorithms.visualization.impl.visualizer.linear.animation;

import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationPlanner;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.api.AnimationTimings;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import java.util.Objects;

/** JavaFX-neutral Stack/Queue transition planner built from generic node primitives. */
public final class LinearStructureAnimationPlanner implements AnimationPlanner<LinearStructureViewState> {
    private static final double MOVE_EPSILON = 0.5d;

    private final String kind;

    public LinearStructureAnimationPlanner(String kind) {
        if (!StructureIds.STACK.equals(kind) && !StructureIds.QUEUE.equals(kind)) {
            throw new IllegalArgumentException("Unsupported linear animation kind: " + kind);
        }
        this.kind = kind;
    }

    @Override
    public AnimationPlan plan(
            LinearStructureViewState previousState,
            LayoutPatch previousLayout,
            LinearStructureViewState nextState,
            LayoutPatch nextLayout) {
        AnimationPlan.Builder plan = AnimationPlan.builder();
        int oldSize = previousState.values().size();
        int newSize = nextState.values().size();
        LinearStructureViewState.Type type = nextState.mutation().type();

        if (StructureIds.STACK.equals(kind) && type == LinearStructureViewState.Type.PUSH) {
            planFrontInsertion(plan, previousLayout, nextLayout, oldSize, newSize);
        } else if (StructureIds.STACK.equals(kind) && type == LinearStructureViewState.Type.POP) {
            planFrontRemoval(plan, previousLayout, nextLayout, oldSize, newSize);
        } else if (StructureIds.QUEUE.equals(kind) && type == LinearStructureViewState.Type.ENQUEUE) {
            planTailInsertion(plan, previousLayout, nextLayout, oldSize, newSize);
        } else if (StructureIds.QUEUE.equals(kind) && type == LinearStructureViewState.Type.DEQUEUE) {
            planFrontRemoval(plan, previousLayout, nextLayout, oldSize, newSize);
        } else {
            planFallback(plan, previousState, previousLayout, nextState, nextLayout);
        }
        return plan.build();
    }

    private void planFrontInsertion(AnimationPlan.Builder plan, LayoutPatch before, LayoutPatch after,
            int oldSize, int newSize) {
        if (newSize != oldSize + 1) return;
        for (int source = 0; source < oldSize; source++) {
            moveIfNeeded(plan, before, source, after, source + 1);
        }
        enter(plan, 0);
    }

    private void planTailInsertion(AnimationPlan.Builder plan, LayoutPatch before, LayoutPatch after,
            int oldSize, int newSize) {
        if (newSize != oldSize + 1) return;
        for (int source = 0; source < oldSize; source++) {
            moveIfNeeded(plan, before, source, after, source);
        }
        enter(plan, newSize - 1);
    }

    private void planFrontRemoval(AnimationPlan.Builder plan, LayoutPatch before, LayoutPatch after,
            int oldSize, int newSize) {
        if (newSize + 1 != oldSize || oldSize == 0) return;
        exit(plan, 0);
        for (int source = 1; source < oldSize; source++) {
            moveIfNeeded(plan, before, source, after, source - 1);
        }
    }

    private void planFallback(AnimationPlan.Builder plan,
            LinearStructureViewState previousState, LayoutPatch previousLayout,
            LinearStructureViewState nextState, LayoutPatch nextLayout) {
        int common = Math.min(previousState.values().size(), nextState.values().size());
        for (int index = 0; index < common; index++) {
            moveIfNeeded(plan, previousLayout, index, nextLayout, index);
            if (!Objects.equals(previousState.values().get(index), nextState.values().get(index))) {
                valueChange(plan, index);
            }
        }
        for (int index = common; index < previousState.values().size(); index++) exit(plan, index);
        for (int index = common; index < nextState.values().size(); index++) enter(plan, index);
    }

    private void moveIfNeeded(AnimationPlan.Builder plan, LayoutPatch beforePatch, int sourceIndex,
            LayoutPatch afterPatch, int targetIndex) {
        ElementGeometry before = beforePatch.elements().get(LinearAnimationIds.node(kind, sourceIndex));
        ElementGeometry after = afterPatch.elements().get(LinearAnimationIds.node(kind, targetIndex));
        if (before == null || after == null) return;
        double beforeX = before.x() + before.width() / 2.0d;
        double beforeY = before.y() + before.height() / 2.0d;
        double afterX = after.x() + after.width() / 2.0d;
        double afterY = after.y() + after.height() / 2.0d;
        if (Math.hypot(afterX - beforeX, afterY - beforeY) <= MOVE_EPSILON) return;
        plan.add(new AnimationStep.NodeMove(LinearAnimationIds.node(kind, targetIndex)),
                AnimationTimings.NODE_MOVE_DELAY_MS, AnimationTimings.NODE_MOVE_MS);
    }

    private void enter(AnimationPlan.Builder plan, int index) {
        plan.add(new AnimationStep.NodeEnter(LinearAnimationIds.node(kind, index)),
                AnimationTimings.NODE_ENTER_DELAY_MS, AnimationTimings.NODE_ENTER_MS);
    }

    private void exit(AnimationPlan.Builder plan, int previousIndex) {
        plan.add(new AnimationStep.NodeExit(LinearAnimationIds.exit(kind, previousIndex)),
                0.0d, AnimationTimings.NODE_EXIT_MS);
    }

    private void valueChange(AnimationPlan.Builder plan, int index) {
        plan.add(new AnimationStep.ValueChange(LinearAnimationIds.node(kind, index)),
                AnimationTimings.VALUE_CHANGE_DELAY_MS, AnimationTimings.VALUE_CHANGE_MS);
    }
}
