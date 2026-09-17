package com.majortom.algorithms.visualization.impl.visualizer.linked.animation;

import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationPlanner;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.api.AnimationTimings;
import com.majortom.algorithms.visualization.impl.visualizer.linked.LinkedListLayout;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** JavaFX-neutral linked-list transition planner. */
public final class LinkedListAnimationPlanner implements AnimationPlanner<LinkedListViewState> {
  private static final double MOVE_EPSILON = 0.5d;

  @Override
  public AnimationPlan plan(LinkedListViewState previousState, LayoutPatch previousLayout,
      LinkedListViewState nextState, LayoutPatch nextLayout) {
    AnimationPlan.Builder plan = AnimationPlan.builder();
    Set<Long> previousIds = previousState.nodes().keySet();
    Set<Long> nextIds = nextState.nodes().keySet();

    Set<String> previousEdges = edgeIds(previousState);
    Set<String> nextEdges = edgeIds(nextState);
    previousEdges.stream().filter(id -> !nextEdges.contains(id)).forEach(id -> plan.add(
        new AnimationStep.EdgeRemove(id), 0.0d, AnimationTimings.EDGE_REMOVE_MS));

    previousIds.stream().filter(id -> !nextIds.contains(id)).forEach(id -> plan.add(
        new AnimationStep.NodeExit(LinkedListAnimationIds.node(id)), 0.0d,
        AnimationTimings.NODE_EXIT_MS));

    nextEdges.stream().filter(id -> !previousEdges.contains(id)).forEach(id -> plan.add(
        new AnimationStep.EdgeCreate(id), AnimationTimings.EDGE_CREATE_DELAY_MS,
        AnimationTimings.EDGE_CREATE_MS));

    nextIds.stream().filter(previousIds::contains).forEach(id -> {
      ElementGeometry before = previousLayout.elements().get(LinkedListLayout.nodeId(id));
      ElementGeometry after = nextLayout.elements().get(LinkedListLayout.nodeId(id));
      if (moved(before, after)) {
        plan.add(new AnimationStep.NodeMove(LinkedListAnimationIds.node(id)),
            AnimationTimings.NODE_MOVE_DELAY_MS, AnimationTimings.NODE_MOVE_MS);
      }
      LinkedListViewState.Node oldNode = previousState.nodes().get(id);
      LinkedListViewState.Node newNode = nextState.nodes().get(id);
      if (oldNode != null && newNode != null && !oldNode.value().equals(newNode.value())) {
        plan.add(new AnimationStep.ValueChange(LinkedListAnimationIds.node(id)),
            AnimationTimings.VALUE_CHANGE_DELAY_MS, AnimationTimings.VALUE_CHANGE_MS);
      }
    });

    nextIds.stream().filter(id -> !previousIds.contains(id)).forEach(id -> plan.add(
        new AnimationStep.NodeEnter(LinkedListAnimationIds.node(id)),
        AnimationTimings.NODE_ENTER_DELAY_MS, AnimationTimings.NODE_ENTER_MS));
    return plan.build();
  }

  private static boolean moved(ElementGeometry before, ElementGeometry after) {
    if (before == null || after == null) return false;
    double beforeX = before.x() + before.width() / 2.0d;
    double beforeY = before.y() + before.height() / 2.0d;
    double afterX = after.x() + after.width() / 2.0d;
    double afterY = after.y() + after.height() / 2.0d;
    return Math.hypot(afterX - beforeX, afterY - beforeY) > MOVE_EPSILON;
  }

  private static Set<String> edgeIds(LinkedListViewState state) {
    Set<String> ids = new LinkedHashSet<>();
    Map<Long, LinkedListViewState.Node> nodes = state.nodes();
    for (LinkedListViewState.Node node : nodes.values()) {
      if (node.nextId() != null && nodes.containsKey(node.nextId())) {
        ids.add(LinkedListAnimationIds.nextEdge(node.id(), node.nextId()));
      }
      if (node.previousId() != null && nodes.containsKey(node.previousId())) {
        ids.add(LinkedListAnimationIds.previousEdge(node.id(), node.previousId()));
      }
    }
    return ids;
  }
}
