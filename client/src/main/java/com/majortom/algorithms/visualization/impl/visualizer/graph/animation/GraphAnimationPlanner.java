package com.majortom.algorithms.visualization.impl.visualizer.graph.animation;

import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationPlanner;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.api.AnimationTimings;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** JavaFX-neutral graph transition planner. */
public final class GraphAnimationPlanner implements AnimationPlanner<GraphViewState> {
    private static final double MOVE_EPSILON = 0.5d;
    private static final double ROUTE_EPSILON = 0.5d;

    @Override
    public AnimationPlan plan(GraphViewState previousState, LayoutPatch previousLayout,
            GraphViewState nextState, LayoutPatch nextLayout) {
        AnimationPlan.Builder plan = AnimationPlan.builder();
        Map<Long, GraphViewState.Node> previousNodes = previousState.nodesById();
        Map<Long, GraphViewState.Node> nextNodes = nextState.nodesById();
        Map<Long, GraphViewState.Edge> previousEdges = edgesById(previousState);
        Map<Long, GraphViewState.Edge> nextEdges = edgesById(nextState);

        Set<Long> rewiredEdges = nextEdges.keySet().stream()
                .filter(previousEdges::containsKey)
                .filter(id -> endpointsChanged(previousEdges.get(id), nextEdges.get(id)))
                .collect(Collectors.toSet());

        previousEdges.keySet().stream()
                .filter(id -> !nextEdges.containsKey(id) || rewiredEdges.contains(id))
                .forEach(id -> plan.add(new AnimationStep.EdgeRemove(
                                rewiredEdges.contains(id)
                                        ? GraphAnimationIds.rewiredExitEdge(id)
                                        : GraphAnimationIds.edge(id)),
                        0.0d, AnimationTimings.EDGE_REMOVE_MS));
        previousNodes.keySet().stream().filter(id -> !nextNodes.containsKey(id)).forEach(id -> plan.add(
                new AnimationStep.NodeExit(GraphAnimationIds.node(id)), 0.0d,
                AnimationTimings.NODE_EXIT_MS));

        nextEdges.keySet().stream()
                .filter(id -> !previousEdges.containsKey(id) || rewiredEdges.contains(id))
                .forEach(id -> plan.add(new AnimationStep.EdgeCreate(GraphAnimationIds.edge(id)),
                        AnimationTimings.EDGE_CREATE_DELAY_MS, AnimationTimings.EDGE_CREATE_MS));

        Map<String, EdgeGeometry> previousRoutes = routes(previousLayout);
        Map<String, EdgeGeometry> nextRoutes = routes(nextLayout);
        nextEdges.keySet().stream().filter(previousEdges::containsKey)
                .filter(id -> !rewiredEdges.contains(id)).forEach(id -> {
                    String logicalId = GraphAnimationIds.edge(id);
                    if (routeChanged(previousRoutes.get(logicalId), nextRoutes.get(logicalId))) {
                        plan.add(new AnimationStep.EdgeMorph(logicalId),
                                AnimationTimings.EDGE_MORPH_DELAY_MS, AnimationTimings.EDGE_MORPH_MS);
                    }
                });

        nextNodes.keySet().stream().filter(previousNodes::containsKey).forEach(id -> {
            ElementGeometry before = previousLayout.elements().get(GraphAnimationIds.node(id));
            ElementGeometry after = nextLayout.elements().get(GraphAnimationIds.node(id));
            if (moved(before, after)) {
                plan.add(new AnimationStep.NodeMove(GraphAnimationIds.node(id)),
                        AnimationTimings.NODE_MOVE_DELAY_MS, AnimationTimings.NODE_MOVE_MS);
            }
            if (!previousNodes.get(id).value().equals(nextNodes.get(id).value())) {
                plan.add(new AnimationStep.ValueChange(GraphAnimationIds.node(id)),
                        AnimationTimings.VALUE_CHANGE_DELAY_MS, AnimationTimings.VALUE_CHANGE_MS);
            }
        });

        nextNodes.keySet().stream().filter(id -> !previousNodes.containsKey(id)).forEach(id -> plan.add(
                new AnimationStep.NodeEnter(GraphAnimationIds.node(id)),
                AnimationTimings.NODE_ENTER_DELAY_MS, AnimationTimings.NODE_ENTER_MS));
        return plan.build();
    }

    private static Map<Long, GraphViewState.Edge> edgesById(GraphViewState state) {
        Map<Long, GraphViewState.Edge> result = new LinkedHashMap<>();
        for (GraphViewState.Edge edge : state.edges()) result.put(edge.id(), edge);
        return result;
    }

    private static boolean endpointsChanged(GraphViewState.Edge before, GraphViewState.Edge after) {
        return before.fromId() != after.fromId() || before.toId() != after.toId();
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
