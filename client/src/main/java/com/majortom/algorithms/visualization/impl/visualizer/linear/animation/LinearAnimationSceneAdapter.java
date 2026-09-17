package com.majortom.algorithms.visualization.impl.visualizer.linear.animation;

import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.fx.AnimationSceneAdapter;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javafx.geometry.Point2D;

/** Shared FX scene adapter for Stack/Queue while keeping their item maps renderer-owned. */
public final class LinearAnimationSceneAdapter implements AnimationSceneAdapter {
    private final String kind;
    private final VisualizationSurface surface;
    private final Map<Integer, NodeView> items;
    private final Map<String, Point2D> capturedCenters = new LinkedHashMap<>();
    private final Map<String, NodeView> exitingItems = new LinkedHashMap<>();
    private LayoutPatch targetPatch;

    public LinearAnimationSceneAdapter(String kind, VisualizationSurface surface, Map<Integer, NodeView> items) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.surface = Objects.requireNonNull(surface, "surface");
        this.items = Objects.requireNonNull(items, "items");
    }

    public void prepare(AnimationPlan plan, LinearStructureViewState state, LayoutPatch patch) {
        capturedCenters.clear();
        targetPatch = Objects.requireNonNull(patch, "patch");
        captureCenters(state.mutation(), state.values().size());
        for (var timed : plan.steps()) {
            if (timed.step() instanceof AnimationStep.NodeExit exit) detachExit(exit.targetId());
        }
    }

    private void captureCenters(LinearStructureViewState.Mutation mutation, int newSize) {
        for (Map.Entry<Integer, NodeView> entry : items.entrySet()) {
            int source = entry.getKey();
            String logicalId = logicalIdForSource(source, mutation, newSize);
            capturedCenters.put(logicalId, entry.getValue().visualCenter());
        }
    }

    private String logicalIdForSource(int source, LinearStructureViewState.Mutation mutation, int newSize) {
        return switch (mutation.type()) {
            case PUSH -> "stack".equals(kind)
                    ? LinearAnimationIds.node(kind, source + 1)
                    : fallbackId(source, newSize);
            case POP -> "stack".equals(kind)
                    ? source == 0
                            ? LinearAnimationIds.exit(kind, source)
                            : LinearAnimationIds.node(kind, source - 1)
                    : fallbackId(source, newSize);
            case ENQUEUE -> "queue".equals(kind)
                    ? LinearAnimationIds.node(kind, source)
                    : fallbackId(source, newSize);
            case DEQUEUE -> "queue".equals(kind)
                    ? source == 0
                            ? LinearAnimationIds.exit(kind, source)
                            : LinearAnimationIds.node(kind, source - 1)
                    : fallbackId(source, newSize);
            case NONE -> fallbackId(source, newSize);
        };
    }

    private String fallbackId(int source, int newSize) {
        return source < newSize
                ? LinearAnimationIds.node(kind, source)
                : LinearAnimationIds.exit(kind, source);
    }

    private void detachExit(String logicalId) {
        int previousIndex = LinearAnimationIds.exitIndex(kind, logicalId);
        if (previousIndex < 0) return;
        NodeView item = items.remove(previousIndex);
        if (item != null) exitingItems.put(logicalId, item);
    }

    public boolean exitDetached(int previousIndex) {
        return exitingItems.containsKey(LinearAnimationIds.exit(kind, previousIndex));
    }

    @Override
    public Optional<NodeTarget> node(String logicalId) {
        int exitIndex = LinearAnimationIds.exitIndex(kind, logicalId);
        if (exitIndex >= 0) {
            NodeView exiting = exitingItems.get(logicalId);
            Point2D center = capturedCenters.get(logicalId);
            return exiting == null || center == null
                    ? Optional.empty()
                    : Optional.of(new NodeTarget(logicalId, exiting, center, List.of()));
        }
        int index = LinearAnimationIds.activeIndex(kind, logicalId);
        if (index < 0) return Optional.empty();
        NodeView item = items.get(index);
        ElementGeometry geometry = targetPatch == null
                ? null
                : targetPatch.elements().get(LinearAnimationIds.node(kind, index));
        if (item == null || geometry == null) return Optional.empty();
        return Optional.of(new NodeTarget(logicalId, item, center(geometry), List.of()));
    }

    @Override
    public Optional<EdgeTarget> edge(String logicalId) {
        return Optional.empty();
    }

    @Override
    public Optional<Point2D> capturedNodeCenter(String logicalId) {
        return Optional.ofNullable(capturedCenters.get(logicalId));
    }

    @Override
    public Optional<List<Point2D>> capturedEdgeRoute(String logicalId) {
        return Optional.empty();
    }

    @Override
    public Collection<NodeTarget> activeNodes() {
        List<NodeTarget> result = new ArrayList<>(items.size());
        for (Map.Entry<Integer, NodeView> entry : items.entrySet()) {
            ElementGeometry geometry = targetPatch == null
                    ? null
                    : targetPatch.elements().get(LinearAnimationIds.node(kind, entry.getKey()));
            if (geometry != null) {
                result.add(new NodeTarget(
                        LinearAnimationIds.node(kind, entry.getKey()), entry.getValue(), center(geometry), List.of()));
            }
        }
        return result;
    }

    @Override
    public Collection<EdgeTarget> activeEdges() {
        return List.of();
    }

    @Override
    public void discardExitedVisuals() {
        exitingItems.values().forEach(item -> surface.nodeLayer().getChildren().remove(item));
        exitingItems.clear();
    }

    @Override
    public void stabilize(AnimationPlan plan) {
        for (NodeTarget target : activeNodes()) {
            target.node().setTranslateX(0.0d);
            target.node().setTranslateY(0.0d);
            target.node().setOpacity(1.0d);
            target.node().setScaleX(1.0d);
            target.node().setScaleY(1.0d);
        }
        discardExitedVisuals();
        capturedCenters.clear();
    }

    private static Point2D center(ElementGeometry geometry) {
        return new Point2D(
                geometry.x() + geometry.width() / 2.0d,
                geometry.y() + geometry.height() / 2.0d);
    }
}
