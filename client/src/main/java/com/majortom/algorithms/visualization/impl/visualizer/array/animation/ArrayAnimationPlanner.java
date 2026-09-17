package com.majortom.algorithms.visualization.impl.visualizer.array.animation;

import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationPlanner;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.api.AnimationTimings;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
import java.util.Objects;

/** JavaFX-neutral array transition planner. Index mutations are translated into generic primitives. */
public final class ArrayAnimationPlanner implements AnimationPlanner<ArrayViewState> {
  private static final double MOVE_EPSILON = 0.5d;

  @Override
  public AnimationPlan plan(ArrayViewState previousState, LayoutPatch previousLayout,
      ArrayViewState nextState, LayoutPatch nextLayout) {
    AnimationPlan.Builder plan = AnimationPlan.builder();
    int oldSize = previousState.values().size();
    int newSize = nextState.values().size();
    ArrayViewState.Mutation mutation = nextState.mutation();

    switch (mutation.type()) {
      case INSERTED -> planInsertion(plan, previousLayout, nextLayout, mutation.index(), oldSize, newSize);
      case REMOVED -> planRemoval(plan, previousLayout, nextLayout, mutation.index(), oldSize, newSize);
      case UPDATED -> {
        planStableIndexes(plan, previousLayout, nextLayout, oldSize, newSize);
        int index = mutation.index();
        if (valid(index, newSize) && valid(index, oldSize)
            && !Objects.equals(previousState.values().get(index), nextState.values().get(index))) {
          valueChange(plan, index);
        }
      }
      case SWAPPED -> planSwap(plan, previousLayout, nextLayout, mutation.index(),
          mutation.otherIndex(), oldSize, newSize);
      case NONE -> planFallback(plan, previousState, previousLayout, nextState, nextLayout);
    }
    return plan.build();
  }

  private static void planInsertion(AnimationPlan.Builder plan, LayoutPatch previousLayout,
      LayoutPatch nextLayout, int insertedIndex, int oldSize, int newSize) {
    if (newSize != oldSize + 1 || insertedIndex < 0 || insertedIndex > oldSize) return;
    for (int source = 0; source < oldSize; source++) {
      int target = source >= insertedIndex ? source + 1 : source;
      moveIfNeeded(plan, previousLayout, source, nextLayout, target);
    }
    enter(plan, insertedIndex);
  }

  private static void planRemoval(AnimationPlan.Builder plan, LayoutPatch previousLayout,
      LayoutPatch nextLayout, int removedIndex, int oldSize, int newSize) {
    if (newSize + 1 != oldSize || !valid(removedIndex, oldSize)) return;
    exit(plan, removedIndex);
    for (int source = 0; source < oldSize; source++) {
      if (source == removedIndex) continue;
      int target = source > removedIndex ? source - 1 : source;
      moveIfNeeded(plan, previousLayout, source, nextLayout, target);
    }
  }

  private static void planSwap(AnimationPlan.Builder plan, LayoutPatch previousLayout,
      LayoutPatch nextLayout, int left, int right, int oldSize, int newSize) {
    if (oldSize != newSize || !valid(left, oldSize) || !valid(right, oldSize) || left == right) return;
    for (int source = 0; source < oldSize; source++) {
      int target = source == left ? right : source == right ? left : source;
      moveIfNeeded(plan, previousLayout, source, nextLayout, target);
    }
  }

  private static void planStableIndexes(AnimationPlan.Builder plan, LayoutPatch previousLayout,
      LayoutPatch nextLayout, int oldSize, int newSize) {
    int common = Math.min(oldSize, newSize);
    for (int index = 0; index < common; index++) {
      moveIfNeeded(plan, previousLayout, index, nextLayout, index);
    }
  }

  private static void planFallback(AnimationPlan.Builder plan, ArrayViewState previousState,
      LayoutPatch previousLayout, ArrayViewState nextState, LayoutPatch nextLayout) {
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

  private static void moveIfNeeded(AnimationPlan.Builder plan, LayoutPatch beforePatch,
      int sourceIndex, LayoutPatch afterPatch, int targetIndex) {
    ElementGeometry before = beforePatch.elements().get(ArrayAnimationIds.node(sourceIndex));
    ElementGeometry after = afterPatch.elements().get(ArrayAnimationIds.node(targetIndex));
    if (!moved(before, after)) return;
    plan.add(new AnimationStep.NodeMove(ArrayAnimationIds.node(targetIndex)),
        AnimationTimings.NODE_MOVE_DELAY_MS, AnimationTimings.NODE_MOVE_MS);
  }

  private static boolean moved(ElementGeometry before, ElementGeometry after) {
    if (before == null || after == null) return false;
    double beforeX = before.x() + before.width() / 2.0d;
    double beforeY = before.y() + before.height() / 2.0d;
    double afterX = after.x() + after.width() / 2.0d;
    double afterY = after.y() + after.height() / 2.0d;
    return Math.hypot(afterX - beforeX, afterY - beforeY) > MOVE_EPSILON;
  }

  private static void enter(AnimationPlan.Builder plan, int index) {
    plan.add(new AnimationStep.NodeEnter(ArrayAnimationIds.node(index)),
        AnimationTimings.NODE_ENTER_DELAY_MS, AnimationTimings.NODE_ENTER_MS);
  }

  private static void exit(AnimationPlan.Builder plan, int previousIndex) {
    plan.add(new AnimationStep.NodeExit(ArrayAnimationIds.exit(previousIndex)), 0.0d,
        AnimationTimings.NODE_EXIT_MS);
  }

  private static void valueChange(AnimationPlan.Builder plan, int index) {
    plan.add(new AnimationStep.ValueChange(ArrayAnimationIds.node(index)),
        AnimationTimings.VALUE_CHANGE_DELAY_MS, AnimationTimings.VALUE_CHANGE_MS);
  }

  private static boolean valid(int index, int size) {
    return index >= 0 && index < size;
  }
}
