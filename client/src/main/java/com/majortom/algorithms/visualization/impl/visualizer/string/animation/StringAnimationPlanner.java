package com.majortom.algorithms.visualization.impl.visualizer.string.animation;

import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationPlanner;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.api.AnimationTimings;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;

/** JavaFX-neutral String transition planner. */
public final class StringAnimationPlanner implements AnimationPlanner<StringViewState> {
  private static final double MOVE_EPSILON = 0.5d;

  @Override
  public AnimationPlan plan(StringViewState previousState, LayoutPatch previousLayout,
      StringViewState nextState, LayoutPatch nextLayout) {
    AnimationPlan.Builder plan = AnimationPlan.builder();
    int oldSize = previousState.value().length();
    int newSize = nextState.value().length();
    StringViewState.Mutation mutation = nextState.mutation();

    switch (mutation.type()) {
      case INSERTED -> planInsertion(plan, previousLayout, nextLayout, mutation.index(),
          mutation.length(), oldSize, newSize);
      case REMOVED -> planRemoval(plan, previousLayout, nextLayout, mutation.index(),
          mutation.length(), oldSize, newSize);
      case UPDATED -> {
        planIdentityMoves(plan, previousLayout, nextLayout, 0, Math.min(oldSize, newSize));
        int index = mutation.index();
        if (valid(index, oldSize) && valid(index, newSize)
            && previousState.value().charAt(index) != nextState.value().charAt(index)) {
          valueChange(plan, index);
        }
      }
      case REPLACED -> planReplacement(plan, previousState, previousLayout, nextState, nextLayout,
          mutation.index(), mutation.length());
      case NONE -> planFallback(plan, previousState, previousLayout, nextState, nextLayout);
    }
    return plan.build();
  }

  private static void planInsertion(AnimationPlan.Builder plan, LayoutPatch before, LayoutPatch after,
      int index, int length, int oldSize, int newSize) {
    if (length <= 0 || newSize != oldSize + length || index < 0 || index > oldSize) return;
    for (int source = 0; source < oldSize; source++) {
      int target = source >= index ? source + length : source;
      moveIfNeeded(plan, before, source, after, target);
    }
    for (int target = index; target < index + length; target++) enter(plan, target);
  }

  private static void planRemoval(AnimationPlan.Builder plan, LayoutPatch before, LayoutPatch after,
      int index, int length, int oldSize, int newSize) {
    if (length <= 0 || newSize + length != oldSize || index < 0 || index + length > oldSize) return;
    for (int source = index; source < index + length; source++) exit(plan, source);
    for (int source = 0; source < oldSize; source++) {
      if (source >= index && source < index + length) continue;
      int target = source >= index + length ? source - length : source;
      moveIfNeeded(plan, before, source, after, target);
    }
  }

  private static void planReplacement(AnimationPlan.Builder plan, StringViewState previousState,
      LayoutPatch before, StringViewState nextState, LayoutPatch after, int index, int newLength) {
    int oldSize = previousState.value().length();
    int newSize = nextState.value().length();
    int delta = newSize - oldSize;
    int oldLength = newLength - delta;
    if (index < 0 || newLength < 0 || oldLength < 0 || index + oldLength > oldSize
        || index + newLength > newSize) return;

    int common = Math.min(oldLength, newLength);
    for (int offset = 0; offset < common; offset++) {
      int target = index + offset;
      moveIfNeeded(plan, before, target, after, target);
      if (previousState.value().charAt(target) != nextState.value().charAt(target)) valueChange(plan, target);
    }
    for (int source = index + common; source < index + oldLength; source++) exit(plan, source);
    for (int target = index + common; target < index + newLength; target++) enter(plan, target);
    for (int source = index + oldLength; source < oldSize; source++) {
      moveIfNeeded(plan, before, source, after, source + delta);
    }
    for (int source = 0; source < index; source++) moveIfNeeded(plan, before, source, after, source);
  }

  private static void planFallback(AnimationPlan.Builder plan, StringViewState previousState,
      LayoutPatch before, StringViewState nextState, LayoutPatch after) {
    int common = Math.min(previousState.value().length(), nextState.value().length());
    for (int index = 0; index < common; index++) {
      moveIfNeeded(plan, before, index, after, index);
      if (previousState.value().charAt(index) != nextState.value().charAt(index)) valueChange(plan, index);
    }
    for (int index = common; index < previousState.value().length(); index++) exit(plan, index);
    for (int index = common; index < nextState.value().length(); index++) enter(plan, index);
  }

  private static void planIdentityMoves(AnimationPlan.Builder plan, LayoutPatch before,
      LayoutPatch after, int fromInclusive, int toExclusive) {
    for (int index = fromInclusive; index < toExclusive; index++) {
      moveIfNeeded(plan, before, index, after, index);
    }
  }

  private static void moveIfNeeded(AnimationPlan.Builder plan, LayoutPatch beforePatch,
      int sourceIndex, LayoutPatch afterPatch, int targetIndex) {
    ElementGeometry before = beforePatch.elements().get(StringAnimationIds.node(sourceIndex));
    ElementGeometry after = afterPatch.elements().get(StringAnimationIds.node(targetIndex));
    if (!moved(before, after)) return;
    plan.add(new AnimationStep.NodeMove(StringAnimationIds.node(targetIndex)),
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
    plan.add(new AnimationStep.NodeEnter(StringAnimationIds.node(index)),
        AnimationTimings.NODE_ENTER_DELAY_MS, AnimationTimings.NODE_ENTER_MS);
  }

  private static void exit(AnimationPlan.Builder plan, int previousIndex) {
    plan.add(new AnimationStep.NodeExit(StringAnimationIds.exit(previousIndex)), 0.0d,
        AnimationTimings.NODE_EXIT_MS);
  }

  private static void valueChange(AnimationPlan.Builder plan, int index) {
    plan.add(new AnimationStep.ValueChange(StringAnimationIds.node(index)),
        AnimationTimings.VALUE_CHANGE_DELAY_MS, AnimationTimings.VALUE_CHANGE_MS);
  }

  private static boolean valid(int index, int size) {
    return index >= 0 && index < size;
  }
}
