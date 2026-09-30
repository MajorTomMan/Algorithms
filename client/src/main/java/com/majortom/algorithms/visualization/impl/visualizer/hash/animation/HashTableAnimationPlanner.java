package com.majortom.algorithms.visualization.impl.visualizer.hash.animation;

import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationPlanner;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.api.AnimationTimings;
import com.majortom.algorithms.visualization.impl.visualizer.hash.HashVisualIds;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.runtime.hash.HashTableViewState;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** JavaFX-neutral transition planner for stable hash bucket/entry identities. */
public final class HashTableAnimationPlanner implements AnimationPlanner<HashTableViewState> {
  private static final double MOVE_EPSILON = 0.5d;
  private static final double ROUTE_EPSILON = 0.5d;

  @Override
  public AnimationPlan plan(
      HashTableViewState previousState,
      LayoutPatch previousLayout,
      HashTableViewState nextState,
      LayoutPatch nextLayout) {
    AnimationPlan.Builder plan = AnimationPlan.builder();
    Map<String, String> previousNodes = nodes(previousState);
    Map<String, String> nextNodes = nodes(nextState);
    Set<String> previousEdges = edges(previousState);
    Set<String> nextEdges = edges(nextState);

    previousEdges.stream()
        .filter(id -> !nextEdges.contains(id))
        .forEach(id -> plan.add(
            new AnimationStep.EdgeRemove(id), 0.0d, AnimationTimings.EDGE_REMOVE_MS));

    previousNodes.keySet().stream()
        .filter(id -> !nextNodes.containsKey(id))
        .forEach(id -> plan.add(
            new AnimationStep.NodeExit(id), 0.0d, AnimationTimings.NODE_EXIT_MS));

    nextEdges.stream()
        .filter(id -> !previousEdges.contains(id))
        .forEach(id -> plan.add(
            new AnimationStep.EdgeCreate(id),
            AnimationTimings.EDGE_CREATE_DELAY_MS,
            AnimationTimings.EDGE_CREATE_MS));

    Map<String, EdgeGeometry> previousRoutes = routes(previousLayout);
    Map<String, EdgeGeometry> nextRoutes = routes(nextLayout);
    nextEdges.stream()
        .filter(previousEdges::contains)
        .filter(id -> routeChanged(previousRoutes.get(id), nextRoutes.get(id)))
        .forEach(id -> plan.add(
            new AnimationStep.EdgeMorph(id),
            AnimationTimings.EDGE_MORPH_DELAY_MS,
            AnimationTimings.EDGE_MORPH_MS));

    nextNodes.keySet().stream()
        .filter(previousNodes::containsKey)
        .forEach(id -> {
          if (moved(previousLayout.elements().get(id), nextLayout.elements().get(id))) {
            plan.add(
                new AnimationStep.NodeMove(id),
                AnimationTimings.NODE_MOVE_DELAY_MS,
                AnimationTimings.NODE_MOVE_MS);
          }
          if (!previousNodes.get(id).equals(nextNodes.get(id))) {
            plan.add(
                new AnimationStep.ValueChange(id),
                AnimationTimings.VALUE_CHANGE_DELAY_MS,
                AnimationTimings.VALUE_CHANGE_MS);
          }
        });

    nextNodes.keySet().stream()
        .filter(id -> !previousNodes.containsKey(id))
        .forEach(id -> plan.add(
            new AnimationStep.NodeEnter(id),
            AnimationTimings.NODE_ENTER_DELAY_MS,
            AnimationTimings.NODE_ENTER_MS));

    return plan.build();
  }

  private static Map<String, String> nodes(HashTableViewState state) {
    Map<String, String> result = new LinkedHashMap<>();
    for (HashTableViewState.Bucket bucket : state.buckets()) {
      result.put(HashVisualIds.bucket(bucket.index()), "#" + bucket.index());
      for (HashTableViewState.Entry entry : bucket.entries()) {
        result.put(
            HashVisualIds.entry(entry.id()),
            entry.key().text() + " → " + entry.value().text());
      }
    }
    return result;
  }

  private static Set<String> edges(HashTableViewState state) {
    java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
    for (HashTableViewState.Bucket bucket : state.buckets()) {
      String previous = HashVisualIds.bucket(bucket.index());
      for (HashTableViewState.Entry entry : bucket.entries()) {
        String target = HashVisualIds.entry(entry.id());
        result.add(HashVisualIds.link(previous, target));
        previous = target;
      }
    }
    return Set.copyOf(result);
  }

  private static Map<String, EdgeGeometry> routes(LayoutPatch patch) {
    Map<String, EdgeGeometry> result = new LinkedHashMap<>();
    for (EdgeGeometry edge : patch.edges()) {
      result.put(edge.id(), edge);
    }
    return result;
  }

  private static boolean moved(ElementGeometry before, ElementGeometry after) {
    if (before == null || after == null) return false;
    double beforeX = before.x() + before.width() / 2.0d;
    double beforeY = before.y() + before.height() / 2.0d;
    double afterX = after.x() + after.width() / 2.0d;
    double afterY = after.y() + after.height() / 2.0d;
    return Math.hypot(afterX - beforeX, afterY - beforeY) > MOVE_EPSILON;
  }

  private static boolean routeChanged(EdgeGeometry before, EdgeGeometry after) {
    if (before == null || after == null) return false;
    if (before.points().size() != after.points().size()) return true;
    for (int index = 0; index < before.points().size(); index++) {
      EdgeGeometry.Point left = before.points().get(index);
      EdgeGeometry.Point right = after.points().get(index);
      if (Math.hypot(left.x() - right.x(), left.y() - right.y()) > ROUTE_EPSILON) {
        return true;
      }
    }
    return false;
  }
}
