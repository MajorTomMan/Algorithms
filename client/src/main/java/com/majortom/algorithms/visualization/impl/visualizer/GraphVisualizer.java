package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.animation.api.AnimationControl;
import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.fx.AnimationSceneAdapter;
import com.majortom.algorithms.visualization.animation.runtime.StructureAnimationRuntime;
import com.majortom.algorithms.visualization.impl.visualizer.semantic.GraphStructureVisualization;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.geometry.CircleGeometry;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphElkLayout;
import com.majortom.algorithms.visualization.impl.visualizer.graph.animation.GraphAnimationIds;
import com.majortom.algorithms.visualization.impl.visualizer.graph.animation.GraphAnimationPlanner;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/** Graph renderer using measured JavaFX nodes, transient ELK Layered routes and GestureFX viewport. */
public final class GraphVisualizer extends BaseVisualizer<GraphViewState> {
    private static final double MIN_RADIUS = 28.0d;
    private static final double LABEL_PADDING = 24.0d;
    private static final double EDGE_LABEL_OFFSET = 20.0d;
    private static final double EDGE_LABEL_OFFSET_STEP = 12.0d;
    private static final double EDGE_LABEL_COLLISION_PADDING = 4.0d;

    private static final RenderSessionId SESSION_ID = RenderSessionId.of("GRAPH");
    private static final StructureVisualization<GraphViewState> STRUCTURE_VISUALIZATION = new GraphStructureVisualization();
    private final VisualizationSurface surface = new VisualizationSurface();
    private final Map<Long, NodeView> nodeViews = new LinkedHashMap<>();
    private final Map<Long, EdgeView> edgeViews = new LinkedHashMap<>();
    private final Map<Long, Label> nodeIdLabels = new LinkedHashMap<>();
    private final StructureAnimationRuntime<GraphViewState> animationRuntime =
            new StructureAnimationRuntime<>(new GraphAnimationPlanner());
    private final GraphAnimationSceneAdapter animationScene = new GraphAnimationSceneAdapter();
    private LayoutPatch lastPatch;
    private Long selectedNodeId;
    private Long selectedEdgeId;
    private Long pendingSelectedNodeId;
    private Long pendingSelectedEdgeId;
    private LongConsumer nodeSelectionListener = ignored -> { };
    private LongConsumer edgeSelectionListener = ignored -> { };
    private VisualDensity density = VisualDensity.DETAIL;

    public GraphVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        // Graph nodes carry ids/edge labels around the factual node geometry. Keep a larger
        // top breathing room so FIT_CONTENT never pins the highest node against the workspace
        // chrome while preserving the common right/bottom toolbar reserves.
        surface.setSafeInsets(new javafx.geometry.Insets(56.0d, 16.0d, 62.0d, 16.0d));
        surface.setFrameworkManagedCamera(true);
    }
    @Override
    public RenderSessionId sessionId() { return SESSION_ID; }



    @Override
    public CompletionStage<Void> commitLayout(
            GraphViewState state, LayoutPatch patch, RenderCommitContext context) {
        boolean animate = context.modelChange() && !context.initialFrame();
        AnimationPlan plan = animationRuntime.beginTransition(state, patch, animate);
        animationScene.prepare(plan, state, patch);

        reconcileNodes(state);
        reconcileEdges(state);
        applyPendingSelection(state);
        applyPresentation(state);

        for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
            ElementGeometry bounds = patch.elements().get(GraphElkLayout.nodeId(entry.getKey()));
            if (bounds == null) continue;
            NodeView view = entry.getValue();
            view.setGeometry(new CircleGeometry(Math.max(MIN_RADIUS, bounds.width() / 2.0d)));
            view.setCenter(bounds.x() + bounds.width() / 2.0d, bounds.y() + bounds.height() / 2.0d);
        }
        applyRoutes(patch);
        lastPatch = patch;
        resolveEdgeLabelCollisions();
        animationRuntime.play(plan, animationScene, context.presentationProgress()::publish);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> commitPresentation(
            GraphViewState state, RenderCommitContext context) {
        reconcileNodes(state);
        reconcileEdges(state);
        applyPendingSelection(state);
        applyPresentation(state);
        resolveEdgeLabelCollisions();
        return CompletableFuture.completedFuture(null);
    }

    private void reconcileNodes(GraphViewState state) {
        Set<Long> expected = state.nodes().stream()
                .map(GraphViewState.Node::id)
                .collect(java.util.stream.Collectors.toSet());
        List<Long> removed = nodeViews.keySet().stream().filter(id -> !expected.contains(id)).toList();
        for (Long nodeId : removed) {
            if (java.util.Objects.equals(selectedNodeId, nodeId)) selectedNodeId = null;
            NodeView view = nodeViews.remove(nodeId);
            if (view != null) surface.nodeLayer().getChildren().remove(view);
            removeNodeIdLabel(nodeId);
        }
        for (GraphViewState.Node node : state.nodes()) {
            if (nodeViews.containsKey(node.id())) continue;
            NodeView view = new NodeView(new CircleGeometry(MIN_RADIUS), node.value().text());
            long nodeId = node.id();
            view.setOnMouseClicked(event -> {
                selectNode(nodeId);
                event.consume();
            });
            nodeViews.put(node.id(), view);
            surface.nodeLayer().getChildren().add(view);
            installNodeIdLabel(node.id(), view);
        }
    }

    private void reconcileEdges(GraphViewState state) {
        Map<Long, GraphViewState.Edge> expected = new LinkedHashMap<>();
        for (GraphViewState.Edge edge : state.edges()) expected.put(edge.id(), edge);

        List<Long> removed = edgeViews.keySet().stream()
                .filter(id -> !expected.containsKey(id))
                .toList();
        for (Long edgeId : removed) {
            if (java.util.Objects.equals(selectedEdgeId, edgeId)) selectedEdgeId = null;
            EdgeView edge = edgeViews.remove(edgeId);
            if (edge != null) {
                edge.dispose();
                surface.edgeLayer().getChildren().remove(edge);
            }
        }

        for (GraphViewState.Edge edge : state.edges()) {
            NodeView source = nodeViews.get(edge.fromId());
            NodeView target = nodeViews.get(edge.toId());
            if (source == null || target == null) continue;
            EdgeView existing = edgeViews.get(edge.id());
            if (existing != null && existing.source() == source && existing.target() == target) continue;
            if (existing != null) {
                edgeViews.remove(edge.id());
                existing.dispose();
                surface.edgeLayer().getChildren().remove(existing);
            }
            EdgeView view = new EdgeView(source, target, state.directed());
            long edgeId = edge.id();
            view.setOnMouseClicked(event -> {
                selectEdge(edgeId);
                event.consume();
            });
            view.setCurved(source == target);
            edgeViews.put(edge.id(), view);
            surface.edgeLayer().getChildren().add(view);
        }
    }

    private void applyPresentation(GraphViewState state) {
        density = densityFor(state.nodes().size());
        for (GraphViewState.Node node : state.nodes()) {
            NodeView view = nodeViews.get(node.id());
            if (view == null) continue;
            view.setText(node.value().text());
            view.setVisited(state.visitedNodeIds().contains(node.id()));
            view.setHighlighted(isObservedNode(state.observation(), node.id()));
        }
        for (GraphViewState.Edge edge : state.edges()) {
            EdgeView view = edgeViews.get(edge.id());
            if (view == null) continue;
            view.setDirected(state.directed());
            view.setLabelText(weightText(edge.weight()));
            view.setLabelNormalOffset(edgeLabelOffset(edge.id()));
            view.setHighlighted(isObservedEdge(state, edge));
        }
        syncSelectionState();
    }

    private void applyRoutes(LayoutPatch patch) {
        Map<String, EdgeGeometry> routes = new LinkedHashMap<>();
        for (EdgeGeometry route : patch.edges()) routes.put(route.id(), route);
        for (Map.Entry<Long, EdgeView> entry : edgeViews.entrySet()) {
            EdgeGeometry route = routes.get(GraphElkLayout.edgeId(entry.getKey()));
            if (route == null || route.points().size() < 2) {
                entry.getValue().clearRoute();
            } else {
                entry.getValue().setRoute(route.points().stream().map(point -> new Point2D(point.x(), point.y())).toList());
            }
        }
        resolveEdgeLabelCollisions();
    }

    private void applyPendingSelection(GraphViewState state) {
        if (pendingSelectedNodeId != null) {
            if (state.nodes().stream().anyMatch(node -> node.id() == pendingSelectedNodeId)) {
                selectedNodeId = pendingSelectedNodeId;
                selectedEdgeId = null;
            }
            pendingSelectedNodeId = null;
            pendingSelectedEdgeId = null;
        } else if (pendingSelectedEdgeId != null) {
            if (state.edges().stream().anyMatch(edge -> edge.id() == pendingSelectedEdgeId)) {
                selectedEdgeId = pendingSelectedEdgeId;
                selectedNodeId = null;
            }
            pendingSelectedEdgeId = null;
        }
    }

    public void setNodeSelectionListener(LongConsumer listener) {
        if (listener == null) {
            nodeSelectionListener = ignored -> { };
        } else {
            nodeSelectionListener = listener;
        }
    }

    public void setEdgeSelectionListener(LongConsumer listener) {
        if (listener == null) {
            edgeSelectionListener = ignored -> { };
        } else {
            edgeSelectionListener = listener;
        }
    }

    public void clearSelection() {
        selectedNodeId = null;
        selectedEdgeId = null;
        pendingSelectedNodeId = null;
        pendingSelectedEdgeId = null;
    }

    public Long selectedNodeId() {
        return selectedNodeId;
    }

    public Long selectedEdgeId() {
        return selectedEdgeId;
    }

    public void selectNode(long nodeId) {
        if (!showNodeSelection(nodeId)) {
            return;
        }
        nodeSelectionListener.accept(nodeId);
    }

    public boolean showNodeSelection(long nodeId) {
        selectedNodeId = nodeId;
        selectedEdgeId = null;
        pendingSelectedNodeId = nodeViews.containsKey(nodeId) ? null : nodeId;
        pendingSelectedEdgeId = null;
        return true;
    }

    public void selectEdge(long edgeId) {
        if (!showEdgeSelection(edgeId)) {
            return;
        }
        edgeSelectionListener.accept(edgeId);
    }

    public boolean showEdgeSelection(long edgeId) {
        selectedEdgeId = edgeId;
        selectedNodeId = null;
        pendingSelectedEdgeId = edgeViews.containsKey(edgeId) ? null : edgeId;
        pendingSelectedNodeId = null;
        return true;
    }

    private void syncSelectionState() {
        nodeViews.forEach((id, view) -> view.setSelected(selectedNodeId != null && selectedNodeId.equals(id)));
        edgeViews.forEach((id, view) -> view.setSelected(selectedEdgeId != null && selectedEdgeId.equals(id)));
        syncNodeIdLabelVisibility();
    }

    private void installNodeIdLabel(long nodeId, NodeView view) {
        Label label = new Label("#" + nodeId);
        label.getStyleClass().add("graph-node-id-label");
        label.setMouseTransparent(true);
        label.layoutXProperty().bind(view.centerXProperty().add(view.translateXProperty())
                .subtract(label.widthProperty().divide(2.0d)));
        label.layoutYProperty().bind(view.centerYProperty().add(view.translateYProperty())
                .add(MIN_RADIUS + 6.0d));
        nodeIdLabels.put(nodeId, label);
        surface.decorationLayer().getChildren().add(label);
    }

    private void removeNodeIdLabel(long nodeId) {
        Label label = nodeIdLabels.remove(nodeId);
        if (label == null) return;
        label.layoutXProperty().unbind();
        label.layoutYProperty().unbind();
        surface.decorationLayer().getChildren().remove(label);
    }

    private void syncNodeIdLabelVisibility() {
        nodeIdLabels.forEach((id, label) -> {
            boolean visible = density == VisualDensity.DETAIL
                    || (selectedNodeId != null && selectedNodeId.equals(id));
            label.setManaged(visible);
            label.setVisible(visible);
        });
    }

    private VisualDensity densityFor(int nodeCount) {
        if (nodeCount <= 12) return VisualDensity.DETAIL;
        if (nodeCount <= 30) return VisualDensity.COMPACT;
        return VisualDensity.DENSE;
    }

    @Override
    protected AnimationControl animationControl() {
        return animationRuntime;
    }

    @Override
    public StructureVisualization<GraphViewState> structureVisualization() {
        return STRUCTURE_VISUALIZATION;
    }

    @Override
    public FxSurfaceAdapter fxSurfaceAdapter() {
        return surface;
    }
@Override
    public void setViewportObstructionInsets(javafx.geometry.Insets insets) {
        surface.setObstructionInsets(insets);
    }

    @Override
    public void onVisualizationReset() {
        super.onVisualizationReset();
        edgeViews.values().forEach(EdgeView::dispose);
        nodeViews.clear();
        edgeViews.clear();
        nodeIdLabels.clear();
        surface.nodeLayer().getChildren().clear();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
        selectedNodeId = null;
        selectedEdgeId = null;
        pendingSelectedNodeId = null;
        pendingSelectedEdgeId = null;
        lastPatch = null;
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        if (isDisposed()) return;
        super.dispose();
        edgeViews.values().forEach(EdgeView::dispose);
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
    }



    private final class GraphAnimationSceneAdapter implements AnimationSceneAdapter {
        private final Map<String, Point2D> capturedCenters = new LinkedHashMap<>();
        private final Map<String, List<Point2D>> capturedRoutes = new LinkedHashMap<>();
        private final Map<Long, NodeView> exitingNodes = new LinkedHashMap<>();
        private final Map<Long, Label> exitingNodeLabels = new LinkedHashMap<>();
        private final Map<String, EdgeView> exitingEdges = new LinkedHashMap<>();
        private LayoutPatch targetPatch;

        void prepare(AnimationPlan plan, GraphViewState state, LayoutPatch patch) {
            capturedCenters.clear();
            capturedRoutes.clear();
            for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
                capturedCenters.put(GraphAnimationIds.node(entry.getKey()), entry.getValue().visualCenter());
            }
            for (Map.Entry<Long, EdgeView> entry : edgeViews.entrySet()) {
                capturedRoutes.put(GraphAnimationIds.edge(entry.getKey()), entry.getValue().routeSnapshot());
            }
            targetPatch = patch;

            for (var timed : plan.steps()) {
                if (timed.step() instanceof AnimationStep.EdgeRemove remove) detachEdge(remove.targetId());
            }
            for (var timed : plan.steps()) {
                if (timed.step() instanceof AnimationStep.NodeExit exit) detachNode(exit.targetId());
            }
        }

        private void detachNode(String logicalId) {
            Long id = nodeViews.keySet().stream()
                    .filter(candidate -> GraphAnimationIds.node(candidate).equals(logicalId))
                    .findFirst().orElse(null);
            if (id == null) return;
            NodeView view = nodeViews.remove(id);
            if (view != null) exitingNodes.put(id, view);
            Label label = nodeIdLabels.remove(id);
            if (label != null) exitingNodeLabels.put(id, label);
            if (java.util.Objects.equals(selectedNodeId, id)) selectedNodeId = null;
        }

        private void detachEdge(String logicalId) {
            Long id = edgeViews.keySet().stream()
                    .filter(candidate -> GraphAnimationIds.edge(candidate).equals(logicalId)
                            || GraphAnimationIds.rewiredExitEdge(candidate).equals(logicalId))
                    .findFirst().orElse(null);
            if (id == null) return;
            EdgeView edge = edgeViews.remove(id);
            if (edge != null) exitingEdges.put(logicalId, edge);
            if (java.util.Objects.equals(selectedEdgeId, id)) selectedEdgeId = null;
        }

        @Override
        public Optional<NodeTarget> node(String logicalId) {
            for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
                if (GraphAnimationIds.node(entry.getKey()).equals(logicalId)) {
                    Label label = nodeIdLabels.get(entry.getKey());
                    return Optional.of(new NodeTarget(logicalId, entry.getValue(),
                            label == null ? List.of() : List.of(label)));
                }
            }
            for (Map.Entry<Long, NodeView> entry : exitingNodes.entrySet()) {
                if (GraphAnimationIds.node(entry.getKey()).equals(logicalId)) {
                    Label label = exitingNodeLabels.get(entry.getKey());
                    return Optional.of(new NodeTarget(logicalId, entry.getValue(),
                            label == null ? List.of() : List.of(label)));
                }
            }
            return Optional.empty();
        }

        @Override
        public Optional<EdgeTarget> edge(String logicalId) {
            EdgeView active = edgeViews.entrySet().stream()
                    .filter(entry -> GraphAnimationIds.edge(entry.getKey()).equals(logicalId))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            EdgeView edge = active == null ? exitingEdges.get(logicalId) : active;
            return edge == null ? Optional.empty() : Optional.of(new EdgeTarget(logicalId, edge));
        }

        @Override
        public Optional<Point2D> capturedNodeCenter(String logicalId) {
            return Optional.ofNullable(capturedCenters.get(logicalId));
        }

        @Override
        public Optional<List<Point2D>> capturedEdgeRoute(String logicalId) {
            return Optional.ofNullable(capturedRoutes.get(logicalId));
        }

        @Override
        public Collection<NodeTarget> activeNodes() {
            List<NodeTarget> result = new ArrayList<>(nodeViews.size());
            for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
                Label label = nodeIdLabels.get(entry.getKey());
                result.add(new NodeTarget(GraphAnimationIds.node(entry.getKey()), entry.getValue(),
                        label == null ? List.of() : List.of(label)));
            }
            return result;
        }

        @Override
        public Collection<EdgeTarget> activeEdges() {
            return edgeViews.entrySet().stream()
                    .map(entry -> new EdgeTarget(GraphAnimationIds.edge(entry.getKey()), entry.getValue()))
                    .toList();
        }

        @Override
        public void discardExitedVisuals() {
            exitingEdges.values().forEach(edge -> {
                surface.edgeLayer().getChildren().remove(edge);
                edge.dispose();
            });
            exitingEdges.clear();
            exitingNodeLabels.values().forEach(label -> {
                label.layoutXProperty().unbind();
                label.layoutYProperty().unbind();
                surface.decorationLayer().getChildren().remove(label);
            });
            exitingNodeLabels.clear();
            exitingNodes.values().forEach(node -> surface.nodeLayer().getChildren().remove(node));
            exitingNodes.clear();
        }

        @Override
        public void stabilize(AnimationPlan plan) {
            for (NodeTarget target : activeNodes()) {
                Node node = target.node();
                node.setTranslateX(0.0d);
                node.setTranslateY(0.0d);
                node.setOpacity(1.0d);
                node.setScaleX(1.0d);
                node.setScaleY(1.0d);
                target.companions().forEach(companion -> companion.setOpacity(1.0d));
            }
            for (EdgeTarget target : activeEdges()) {
                target.edge().setRevealProgress(1.0d);
                target.edge().setOpacity(1.0d);
            }
            discardExitedVisuals();
            if (targetPatch != null) applyRoutes(targetPatch);
            resolveEdgeLabelCollisions();
            capturedCenters.clear();
            capturedRoutes.clear();
        }
    }

    private static boolean isObservedNode(GraphViewState.Observation observation, long nodeId) {
        return switch (observation.type()) {
            case VISITED -> false;
            case EXAMINED -> (observation.firstNodeId() != null && observation.firstNodeId() == nodeId)
                    || (observation.secondNodeId() != null && observation.secondNodeId() == nodeId);
            case NONE -> false;
        };
    }

    private double edgeLabelOffset(long edgeId) {
        if ((edgeId & 1L) == 0L) {
            return EDGE_LABEL_OFFSET;
        }
        return -EDGE_LABEL_OFFSET;
    }

    /**
     * Keeps weighted-edge labels readable without changing graph topology or route ownership.
     * Labels first keep the deterministic side chosen by edge id, then move farther from the
     * edge only when that position intersects a node, node-id label, or an already placed weight.
     */
    private void resolveEdgeLabelCollisions() {
        List<Bounds> occupied = new ArrayList<>();
        for (NodeView node : nodeViews.values()) {
            occupied.add(expanded(node.getBoundsInParent(), EDGE_LABEL_COLLISION_PADDING));
        }
        for (Label nodeIdLabel : nodeIdLabels.values()) {
            if (nodeIdLabel.isVisible()) {
                occupied.add(expanded(nodeIdLabel.getBoundsInParent(), EDGE_LABEL_COLLISION_PADDING));
            }
        }

        List<Map.Entry<Long, EdgeView>> ordered = new ArrayList<>(edgeViews.entrySet());
        ordered.sort(Map.Entry.comparingByKey());
        for (Map.Entry<Long, EdgeView> entry : ordered) {
            EdgeView edge = entry.getValue();
            if (edge.labelText() == null) {
                continue;
            }
            double[] candidates = edgeLabelOffsetCandidates(entry.getKey());
            double bestOffset = candidates[0];
            double bestScore = Double.POSITIVE_INFINITY;
            for (double candidate : candidates) {
                edge.setLabelNormalOffset(candidate);
                Bounds candidateBounds = expanded(edge.labelNode().getBoundsInParent(), EDGE_LABEL_COLLISION_PADDING);
                double score = overlapScore(candidateBounds, occupied);
                if (score < bestScore) {
                    bestScore = score;
                    bestOffset = candidate;
                }
                if (score == 0.0d) {
                    break;
                }
            }
            edge.setLabelNormalOffset(bestOffset);
            occupied.add(expanded(edge.labelNode().getBoundsInParent(), EDGE_LABEL_COLLISION_PADDING));
        }
    }

    private double[] edgeLabelOffsetCandidates(long edgeId) {
        double first = edgeLabelOffset(edgeId);
        double opposite = -first;
        double secondMagnitude = EDGE_LABEL_OFFSET + EDGE_LABEL_OFFSET_STEP;
        double thirdMagnitude = secondMagnitude + EDGE_LABEL_OFFSET_STEP;
        double sign = 1.0d;
        if (first < 0.0d) {
            sign = -1.0d;
        }
        return new double[] {
                first,
                opposite,
                sign * secondMagnitude,
                -sign * secondMagnitude,
                sign * thirdMagnitude,
                -sign * thirdMagnitude
        };
    }

    private Bounds expanded(Bounds bounds, double padding) {
        return new BoundingBox(
                bounds.getMinX() - padding,
                bounds.getMinY() - padding,
                bounds.getWidth() + padding * 2.0d,
                bounds.getHeight() + padding * 2.0d);
    }

    private double overlapScore(Bounds candidate, List<Bounds> occupied) {
        double score = 0.0d;
        for (Bounds other : occupied) {
            double width = Math.min(candidate.getMaxX(), other.getMaxX())
                    - Math.max(candidate.getMinX(), other.getMinX());
            double height = Math.min(candidate.getMaxY(), other.getMaxY())
                    - Math.max(candidate.getMinY(), other.getMinY());
            if (width > 0.0d && height > 0.0d) {
                score += width * height;
            }
        }
        return score;
    }

    private static String weightText(Double weight) {
        if (weight == null) {
            return null;
        }
        if (Math.rint(weight) == weight) {
            return Long.toString(weight.longValue());
        }
        return String.format(java.util.Locale.ROOT, "%.2f", weight);
    }

    private static boolean isObservedEdge(GraphViewState state, GraphViewState.Edge edge) {
        GraphViewState.Observation observation = state.observation();
        if (observation.type() != GraphViewState.Type.EXAMINED
                || observation.firstNodeId() == null || observation.secondNodeId() == null) {
            return false;
        }
        boolean direct = edge.fromId() == observation.firstNodeId() && edge.toId() == observation.secondNodeId();
        if (state.directed()) {
            return direct;
        }
        return direct || (edge.fromId() == observation.secondNodeId() && edge.toId() == observation.firstNodeId());
    }



}
