package com.majortom.algorithms.visualization.impl.visualizer.tree.animation;

import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationPlanner;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.api.AnimationTimings;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** JavaFX-neutral tree transition planner. */
public final class TreeAnimationPlanner implements AnimationPlanner<TreeViewState> {
    private static final double MOVE_EPSILON = 0.5d;
    private static final double ROUTE_EPSILON = 0.5d;

    @Override
    public AnimationPlan plan(TreeViewState previousState, LayoutPatch previousLayout,
            TreeViewState nextState, LayoutPatch nextLayout) {
        AnimationPlan.Builder plan = AnimationPlan.builder();
        Set<Long> previousNodes = previousState.nodes().keySet();
        Set<Long> nextNodes = nextState.nodes().keySet();
        Set<String> previousEdges = TreeAnimationIds.edgeIds(previousState);
        Set<String> nextEdges = TreeAnimationIds.edgeIds(nextState);

        previousEdges.stream().filter(id -> !nextEdges.contains(id)).forEach(id -> plan.add(
                new AnimationStep.EdgeRemove(id), 0.0d, AnimationTimings.EDGE_REMOVE_MS));
        previousNodes.stream().filter(id -> !nextNodes.contains(id)).forEach(id -> plan.add(
                new AnimationStep.NodeExit(TreeAnimationIds.node(id)), 0.0d,
                AnimationTimings.NODE_EXIT_MS));

        nextEdges.stream().filter(id -> !previousEdges.contains(id)).forEach(id -> plan.add(
                new AnimationStep.EdgeCreate(id), AnimationTimings.EDGE_CREATE_DELAY_MS,
                AnimationTimings.EDGE_CREATE_MS));

        Map<String, EdgeGeometry> previousRoutes = routes(previousLayout);
        Map<String, EdgeGeometry> nextRoutes = routes(nextLayout);
        nextEdges.stream().filter(previousEdges::contains).forEach(id -> {
            if (routeChanged(previousRoutes.get(id), nextRoutes.get(id))) {
                plan.add(new AnimationStep.EdgeMorph(id), AnimationTimings.EDGE_MORPH_DELAY_MS,
                        AnimationTimings.EDGE_MORPH_MS);
            }
        });

        nextNodes.stream().filter(previousNodes::contains).forEach(id -> {
            ElementGeometry before = previousLayout.elements().get(TreeAnimationIds.node(id));
            ElementGeometry after = nextLayout.elements().get(TreeAnimationIds.node(id));
            if (moved(before, after)) {
                plan.add(new AnimationStep.NodeMove(TreeAnimationIds.node(id)),
                        AnimationTimings.NODE_MOVE_DELAY_MS, AnimationTimings.NODE_MOVE_MS);
            }
            TreeViewState.Node oldNode = previousState.nodes().get(id);
            TreeViewState.Node newNode = nextState.nodes().get(id);
            if (oldNode != null && newNode != null && !oldNode.value().equals(newNode.value())) {
                plan.add(new AnimationStep.ValueChange(TreeAnimationIds.node(id)),
                        AnimationTimings.VALUE_CHANGE_DELAY_MS, AnimationTimings.VALUE_CHANGE_MS);
            }
        });

        nextNodes.stream().filter(id -> !previousNodes.contains(id)).forEach(id -> plan.add(
                new AnimationStep.NodeEnter(TreeAnimationIds.node(id)),
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

    private static Map<String, EdgeGeometry> routes(LayoutPatch patch) {
        Map<String, EdgeGeometry> result = new LinkedHashMap<>();
        for (EdgeGeometry edge : patch.edges()) result.put(edge.id(), edge);
        return result;
    }

    private static boolean routeChanged(EdgeGeometry before, EdgeGeometry after) {
        if (before == null || after == null) return false;
        if (before.points().size() != after.points().size()) return true;
        for (int index = 0; index < before.points().size(); index++) {
            EdgeGeometry.Point left = before.points().get(index);
            EdgeGeometry.Point right = after.points().get(index);
            if (Math.hypot(left.x() - right.x(), left.y() - right.y()) > ROUTE_EPSILON) return true;
        }
        return false;
    }
}
