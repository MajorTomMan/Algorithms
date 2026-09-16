package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.CircleGeometry;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphElkLayout;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralChange;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.fx.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.fx.RenderCommitContext;
import com.majortom.algorithms.visualization.render.runtime.DefaultRenderFramework;
import com.majortom.algorithms.visualization.render.runtime.RenderRuntime;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.render.viewport.CameraState;
import com.majortom.algorithms.visualization.render.viewport.ViewportSnapshot;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;

import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.beans.InvalidationListener;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/**
 * Graph renderer using measured JavaFX nodes, transient ELK Layered routes and GestureFX viewport.
 */
public final class GraphVisualizer extends BaseVisualizer<GraphViewState>
        implements FxSurfaceAdapter<GraphViewState> {
    private static final double MIN_RADIUS = 28.0d;
    private static final double LABEL_PADDING = 24.0d;
    private static final Duration MOVE_DURATION = Duration.millis(300.0d);
    private static final Duration APPEAR_DURATION = Duration.millis(180.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(140.0d);
    private static final double NEW_NODE_ENTRY_DISTANCE = MIN_RADIUS * 2.0d + 8.0d;
    private static final double EDGE_LABEL_OFFSET = 20.0d;
    private static final double EDGE_LABEL_OFFSET_STEP = 12.0d;
    private static final double EDGE_LABEL_COLLISION_PADDING = 4.0d;

    private static final RenderSessionId SESSION_ID = RenderSessionId.of("GRAPH");
    private final VisualizationSurface surface = new VisualizationSurface();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final DefaultRenderFramework renderFramework = RenderRuntime.shared();
    private final Map<Long, NodeView> nodeViews = new LinkedHashMap<>();
    private final Map<Long, EdgeView> edgeViews = new LinkedHashMap<>();
    private final Map<Long, Label> nodeIdLabels = new LinkedHashMap<>();
    private boolean measuringElements;
    private final InvalidationListener elementSizeListener =
            observable -> {
                if (!measuringElements) {
                    requestGeometryRefresh();
                }
            };

    private GraphViewState renderedState = emptyState();
    private volatile GraphViewState lastSubmittedState;
    private Animation activeAnimation;
    private PendingLayout pendingLayout = PendingLayout.empty();
    private boolean firstRender = true;
    private boolean hasAppliedLayout;
    private Long selectedNodeId;
    private Long selectedEdgeId;
    private Long pendingSelectedNodeId;
    private Long pendingSelectedEdgeId;
    private LongConsumer nodeSelectionListener = ignored -> {};
    private LongConsumer edgeSelectionListener = ignored -> {};
    private VisualDensity density = VisualDensity.DETAIL;

    public GraphVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        surface.setFrameworkManagedCamera(true);
        renderFramework.registerSurface(SESSION_ID, this);
    }

    @Override
    protected synchronized void submitFrameworkRender(GraphViewState state) {
        GraphViewState previous = lastSubmittedState;
        boolean initial = previous == null;
        boolean structural = initial || requiresStructuralLayout(previous, state);
        lastSubmittedState = state;
        if (structural) {
            CameraPolicy cameraPolicy =
                    initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE;
            renderFramework.submit(
                    new StructuralRenderIntent<>(SESSION_ID, state, cameraPolicy, initial));
        } else {
            renderFramework.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
    }

    private void requestGeometryRefresh() {
        var state = currentState();
        if (state == null || !isModuleAttached() || isDisposed()) {
            return;
        }
        renderFramework.submit(
                new StructuralRenderIntent<>(
                        SESSION_ID,
                        state,
                        CameraPolicy.ENSURE_VISIBLE,
                        false,
                        StructuralChange.GEOMETRY));
    }

    @Override
    public LayoutRequest captureLayout(GraphViewState state, RenderCaptureContext context) {
        cleanupDetachedViews();
        List<Animation> transitions = new ArrayList<>();
        Set<Long> newNodeIds = new HashSet<>();

        density = densityFor(state.nodes().size());
        Map<Long, GraphViewState.Node> previousNodes = renderedState.nodesById();
        for (GraphViewState.Node node : state.nodes()) {
            NodeView view = nodeViews.get(node.id());
            if (view == null) {
                view = new NodeView(new CircleGeometry(MIN_RADIUS), node.value().text());
                view.layoutBoundsProperty().addListener(elementSizeListener);
                long visualNodeId = node.id();
                view.setOnMouseClicked(
                        event -> {
                            selectedNodeId = visualNodeId;
                            selectedEdgeId = null;
                            syncSelectionState();
                            nodeSelectionListener.accept(visualNodeId);
                            event.consume();
                        });
                nodeViews.put(node.id(), view);
                surface.nodeLayer().getChildren().add(view);
                installNodeIdLabel(node.id(), view);
                newNodeIds.add(node.id());
                if (!firstRender) {
                    transitions.add(
                            animations.together(
                                    animations.fadeIn(view, APPEAR_DURATION),
                                    animations.scaleIn(view, APPEAR_DURATION)));
                }
            } else {
                view.setText(node.value().text());
            }
            view.setVisited(state.visitedNodeIds().contains(node.id()));
            view.setHighlighted(isObservedNode(state.observation(), node.id()));
        }

        syncEdges(state, transitions);
        boolean pendingNodeSelectionApplied = false;
        boolean pendingEdgeSelectionApplied = false;
        if (pendingSelectedNodeId != null) {
            boolean exists =
                    state.nodes().stream().anyMatch(node -> node.id() == pendingSelectedNodeId);
            if (exists) {
                selectedNodeId = pendingSelectedNodeId;
                selectedEdgeId = null;
                pendingNodeSelectionApplied = true;
            }
            pendingSelectedNodeId = null;
            pendingSelectedEdgeId = null;
        } else if (pendingSelectedEdgeId != null) {
            boolean exists =
                    state.edges().stream().anyMatch(edge -> edge.id() == pendingSelectedEdgeId);
            if (exists) {
                selectedEdgeId = pendingSelectedEdgeId;
                selectedNodeId = null;
                pendingEdgeSelectionApplied = true;
            }
            pendingSelectedEdgeId = null;
        }
        syncSelectionState();
        if (pendingNodeSelectionApplied) {
            nodeSelectionListener.accept(selectedNodeId);
        } else if (pendingEdgeSelectionApplied) {
            edgeSelectionListener.accept(selectedEdgeId);
        }
        resolveEdgeLabelCollisions();

        Set<Long> currentNodeIds =
                state.nodes().stream()
                        .map(GraphViewState.Node::id)
                        .collect(java.util.stream.Collectors.toSet());
        List<Long> removedIds =
                nodeViews.keySet().stream().filter(id -> !currentNodeIds.contains(id)).toList();
        for (Long nodeId : removedIds) {
            NodeView view = nodeViews.remove(nodeId);
            removeNodeIdLabel(nodeId);
            if (selectedNodeId != null && selectedNodeId.equals(nodeId)) {
                selectedNodeId = null;
            }
            view.layoutBoundsProperty().removeListener(elementSizeListener);
            if (firstRender) {
                surface.nodeLayer().getChildren().remove(view);
            } else {
                Animation fade = animations.fadeOut(view, DISAPPEAR_DURATION);
                fade.setOnFinished(event -> surface.nodeLayer().getChildren().remove(view));
                transitions.add(fade);
            }
        }

        pendingLayout =
                new PendingLayout(
                        List.copyOf(transitions), Set.copyOf(newNodeIds), context.initialFrame());
        return buildLayoutInput(state, context);
    }

    private LayoutRequest buildLayoutInput(GraphViewState state, RenderCaptureContext context) {
        List<GraphViewState.Node> orderedNodes =
                state.nodes().stream()
                        .sorted(Comparator.comparingLong(GraphViewState.Node::id))
                        .toList();
        List<LayoutElement> nodes = new ArrayList<>(orderedNodes.size());
        measuringElements = true;
        try {
            for (GraphViewState.Node node : orderedNodes) {
                NodeView view = nodeViews.get(node.id());
                if (view == null) {
                    continue;
                }
                resizeToMeasuredLabel(view);
                CircleGeometry geometry = (CircleGeometry) view.getGeometry();
                nodes.add(
                        new LayoutElement(
                                GraphElkLayout.nodeId(node.id()),
                                quantize(geometry.width()),
                                quantize(geometry.height())));
            }
        } finally {
            measuringElements = false;
        }

        Set<String> available =
                nodes.stream().map(LayoutElement::id).collect(java.util.stream.Collectors.toSet());
        List<LayoutLink> links =
                state.edges().stream()
                        .filter(
                                edge ->
                                        available.contains(GraphElkLayout.nodeId(edge.fromId()))
                                                && available.contains(
                                                        GraphElkLayout.nodeId(edge.toId())))
                        .sorted(Comparator.comparingLong(GraphViewState.Edge::id))
                        .map(
                                edge ->
                                        new LayoutLink(
                                                GraphElkLayout.edgeId(edge.id()),
                                                GraphElkLayout.nodeId(edge.fromId()),
                                                GraphElkLayout.nodeId(edge.toId())))
                        .toList();
        return new LayoutRequest(
                context.requestId(),
                SESSION_ID,
                context.modelRevision(),
                context.geometryRevision(),
                GraphElkLayout.ID,
                nodes,
                links,
                Map.of("directed", Boolean.toString(state.directed())));
    }

    private void resizeToMeasuredLabel(NodeView view) {
        view.applyCss();
        Bounds label = view.labelBounds();
        double diameter =
                Math.max(
                        MIN_RADIUS * 2.0d,
                        Math.ceil(Math.max(label.getWidth(), label.getHeight()) + LABEL_PADDING));
        double radius = diameter / 2.0d;
        CircleGeometry geometry = (CircleGeometry) view.getGeometry();
        if (Math.abs(geometry.radius() - radius) > 0.01d) {
            view.setGeometry(new CircleGeometry(radius));
        }
    }

    @Override
    public CompletionStage<Void> commitLayout(
            GraphViewState state, LayoutPatch patch, RenderCommitContext context) {
        PendingLayout pending = pendingLayout;
        List<Animation> transitions = new ArrayList<>(pending.transitions());
        Set<Long> newNodeIds = pending.newNodeIds();
        boolean snap = context.initialFrame() || animations.isScrubbing() || !hasAppliedLayout;
        for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
            ElementGeometry bounds = patch.elements().get(GraphElkLayout.nodeId(entry.getKey()));
            if (bounds == null) continue;
            Point2D target =
                    new Point2D(
                            bounds.x() + bounds.width() / 2.0d,
                            bounds.y() + bounds.height() / 2.0d);
            NodeView view = entry.getValue();
            if (snap) {
                view.setCenter(target.getX(), target.getY());
            } else if (newNodeIds.contains(entry.getKey())) {
                Point2D origin = newNodeOrigin(entry.getKey(), target, newNodeIds);
                view.setCenter(origin.getX(), origin.getY());
                if (!close(origin, target))
                    transitions.add(animations.move(view, target, MOVE_DURATION));
            } else if (!close(view.center(), target)) {
                transitions.add(animations.move(view, target, MOVE_DURATION));
            }
        }
        pendingLayout = PendingLayout.empty();
        hasAppliedLayout = !patch.elements().isEmpty();
        renderedState = state;
        firstRender = false;
        Runnable finish = () -> applyRoutes(patch);
        if (snap || transitions.isEmpty()) {
            transitions.forEach(animation -> animation.stop());
            finish.run();
            return CompletableFuture.completedFuture(null);
        }
        return playAsync(transitions).thenRun(finish);
    }

    @Override
    public CompletionStage<Void> commitPresentation(
            GraphViewState state, RenderCommitContext context) {
        stopActiveAnimation();
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
        applyPendingSelection(state);
        syncSelectionState();
        resolveEdgeLabelCollisions();
        renderedState = state;
        return CompletableFuture.completedFuture(null);
    }

    private void applyRoutes(LayoutPatch patch) {
        Map<String, EdgeGeometry> routes = new LinkedHashMap<>();
        for (EdgeGeometry route : patch.edges()) routes.put(route.id(), route);
        for (Map.Entry<Long, EdgeView> entry : edgeViews.entrySet()) {
            EdgeGeometry route = routes.get(GraphElkLayout.edgeId(entry.getKey()));
            if (route == null || route.points().size() < 2) {
                entry.getValue().clearRoute();
            } else {
                entry.getValue()
                        .setRoute(
                                route.points().stream()
                                        .map(point -> new Point2D(point.x(), point.y()))
                                        .toList());
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

    private void syncEdges(GraphViewState state, List<Animation> transitions) {
        Map<Long, GraphViewState.Edge> expected = new LinkedHashMap<>();
        for (GraphViewState.Edge edge : state.edges()) {
            expected.put(edge.id(), edge);
        }

        for (GraphViewState.Edge edge : state.edges()) {
            EdgeView existing = edgeViews.get(edge.id());
            if (existing != null) {
                existing.setDirected(state.directed());
                existing.setLabelText(weightText(edge.weight()));
                existing.setLabelNormalOffset(edgeLabelOffset(edge.id()));
                existing.setHighlighted(isObservedEdge(state, edge));
                continue;
            }
            NodeView source = nodeViews.get(edge.fromId());
            NodeView target = nodeViews.get(edge.toId());
            if (source == null || target == null) {
                continue;
            }
            EdgeView view = new EdgeView(source, target, state.directed());
            view.setLabelText(weightText(edge.weight()));
            view.setLabelNormalOffset(edgeLabelOffset(edge.id()));
            long visualEdgeId = edge.id();
            view.setOnMouseClicked(
                    event -> {
                        selectedEdgeId = visualEdgeId;
                        selectedNodeId = null;
                        syncSelectionState();
                        edgeSelectionListener.accept(visualEdgeId);
                        event.consume();
                    });
            view.setCurved(source == target);
            view.setHighlighted(isObservedEdge(state, edge));
            edgeViews.put(edge.id(), view);
            surface.edgeLayer().getChildren().add(view);
            if (!firstRender) {
                transitions.add(animations.reveal(view, APPEAR_DURATION));
            }
        }

        List<Long> removed =
                edgeViews.keySet().stream().filter(id -> !expected.containsKey(id)).toList();
        for (Long edgeId : removed) {
            EdgeView view = edgeViews.remove(edgeId);
            if (selectedEdgeId != null && selectedEdgeId.equals(edgeId)) {
                selectedEdgeId = null;
            }
            if (firstRender) {
                surface.edgeLayer().getChildren().remove(view);
            } else {
                Animation fade = animations.fadeOut(view, DISAPPEAR_DURATION);
                fade.setOnFinished(event -> surface.edgeLayer().getChildren().remove(view));
                transitions.add(fade);
            }
        }
    }

    public void setNodeSelectionListener(LongConsumer listener) {
        if (listener == null) {
            nodeSelectionListener = ignored -> {};
        } else {
            nodeSelectionListener = listener;
        }
    }

    public void setEdgeSelectionListener(LongConsumer listener) {
        if (listener == null) {
            edgeSelectionListener = ignored -> {};
        } else {
            edgeSelectionListener = listener;
        }
    }

    public void clearSelection() {
        selectedNodeId = null;
        selectedEdgeId = null;
        pendingSelectedNodeId = null;
        pendingSelectedEdgeId = null;
        syncSelectionState();
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
        GraphViewState state = currentState();
        boolean exists =
                state != null && state.nodes().stream().anyMatch(node -> node.id() == nodeId);
        if (!exists) {
            return false;
        }
        selectedNodeId = nodeId;
        selectedEdgeId = null;
        if (!nodeViews.containsKey(nodeId)) {
            pendingSelectedNodeId = nodeId;
            pendingSelectedEdgeId = null;
            requestRender();
            return true;
        }
        pendingSelectedNodeId = null;
        pendingSelectedEdgeId = null;
        syncSelectionState();
        return true;
    }

    public void selectEdge(long edgeId) {
        if (!showEdgeSelection(edgeId)) {
            return;
        }
        edgeSelectionListener.accept(edgeId);
    }

    public boolean showEdgeSelection(long edgeId) {
        GraphViewState state = currentState();
        boolean exists =
                state != null && state.edges().stream().anyMatch(edge -> edge.id() == edgeId);
        if (!exists) {
            return false;
        }
        selectedEdgeId = edgeId;
        selectedNodeId = null;
        if (!edgeViews.containsKey(edgeId)) {
            pendingSelectedEdgeId = edgeId;
            pendingSelectedNodeId = null;
            requestRender();
            return true;
        }
        pendingSelectedEdgeId = null;
        pendingSelectedNodeId = null;
        syncSelectionState();
        return true;
    }

    private void syncSelectionState() {
        nodeViews.forEach(
                (id, view) ->
                        view.setSelected(selectedNodeId != null && selectedNodeId.equals(id)));
        edgeViews.forEach(
                (id, view) ->
                        view.setSelected(selectedEdgeId != null && selectedEdgeId.equals(id)));
        syncNodeIdLabelVisibility();
    }

    private void installNodeIdLabel(long nodeId, NodeView view) {
        Label label = new Label("#" + nodeId);
        label.getStyleClass().add("graph-node-id-label");
        label.setMouseTransparent(true);
        label.layoutXProperty()
                .bind(view.centerXProperty().subtract(label.widthProperty().divide(2.0d)));
        label.layoutYProperty().bind(view.centerYProperty().add(MIN_RADIUS + 6.0d));
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
        nodeIdLabels.forEach(
                (id, label) -> {
                    boolean visible =
                            density == VisualDensity.DETAIL
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

    private Point2D newNodeOrigin(long nodeId, Point2D fallback, Set<Long> newNodeIds) {
        List<Point2D> neighbors = new ArrayList<>();
        for (GraphViewState.Edge edge : renderedState.edges()) {
            Long neighborId = null;
            if (edge.fromId() == nodeId) {
                neighborId = edge.toId();
            } else if (edge.toId() == nodeId) {
                neighborId = edge.fromId();
            }
            if (neighborId == null || neighborId == nodeId || newNodeIds.contains(neighborId)) {
                continue;
            }
            NodeView neighbor = nodeViews.get(neighborId);
            if (neighbor != null) {
                neighbors.add(neighbor.center());
            }
        }
        if (neighbors.isEmpty()) {
            return fallback;
        }
        double x = 0.0d;
        double y = 0.0d;
        for (Point2D point : neighbors) {
            x += point.getX();
            y += point.getY();
        }
        Point2D anchor = new Point2D(x / neighbors.size(), y / neighbors.size());
        return entryOrigin(anchor, fallback, NEW_NODE_ENTRY_DISTANCE);
    }

    private Point2D entryOrigin(Point2D anchor, Point2D target, double preferredDistance) {
        Point2D delta = target.subtract(anchor);
        double distance = delta.magnitude();
        if (distance <= 0.01d) {
            return anchor.add(preferredDistance, 0.0d);
        }
        if (distance <= preferredDistance) {
            return target;
        }
        return anchor.add(delta.normalize().multiply(preferredDistance));
    }

    private void cleanupDetachedViews() {
        surface.nodeLayer()
                .getChildren()
                .removeIf(node -> node instanceof NodeView && !nodeViews.containsValue(node));
        surface.edgeLayer()
                .getChildren()
                .removeIf(node -> node instanceof EdgeView && !edgeViews.containsValue(node));
    }

    private void play(List<Animation> transitions, Runnable onFinished) {
        if (transitions.isEmpty()) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }
        ParallelTransition parallel = new ParallelTransition();
        parallel.getChildren().addAll(transitions);
        activeAnimation = parallel;
        parallel.setOnFinished(
                event -> {
                    if (activeAnimation == parallel) {
                        activeAnimation = null;
                    }
                    if (onFinished != null) {
                        onFinished.run();
                    }
                });
        parallel.play();
    }

    private CompletionStage<Void> playAsync(List<Animation> transitions) {
        if (transitions.isEmpty()) return CompletableFuture.completedFuture(null);
        CompletableFuture<Void> future = new CompletableFuture<>();
        ParallelTransition parallel = new ParallelTransition();
        parallel.getChildren().addAll(transitions);
        activeAnimation = parallel;
        parallel.setOnFinished(
                event -> {
                    if (activeAnimation == parallel) activeAnimation = null;
                    future.complete(null);
                });
        parallel.play();
        return future;
    }

    private void stopActiveAnimation() {
        if (activeAnimation != null) {
            activeAnimation.stop();
            activeAnimation = null;
        }
    }

    @Override
    public ViewportSnapshot viewportSnapshot() {
        return surface.viewportSnapshot();
    }

    @Override
    public CameraState cameraState() {
        return surface.cameraState();
    }

    @Override
    public void applyCameraState(CameraState cameraState) {
        surface.applyCameraState(cameraState);
    }

    @Override
    public boolean userControlledCamera() {
        return surface.isUserViewportChanged();
    }

    @Override
    public void prepareInitialFrame() {
        surface.markViewportPristine();
    }

    @Override
    public void revealFrame() {
        surface.setWorldVisible(true);
    }

    @Override
    public void setViewportListener(Consumer<ViewportSnapshot> listener) {
        surface.setViewportListener(listener);
    }

    @Override
    public void onModuleAttached(String moduleId) {
        renderFramework.activateSession(SESSION_ID);
        super.onModuleAttached(moduleId);
    }

    @Override
    public void onModuleDetached(String moduleId) {
        renderFramework.deactivateSession(SESSION_ID);
        super.onModuleDetached(moduleId);
    }

    @Override
    public void setViewportObstructionInsets(javafx.geometry.Insets insets) {
        surface.setObstructionInsets(insets);
    }

    @Override
    public void setPlaybackSpeed(double speed) {
        animations.setPlaybackSpeed(speed);
    }

    @Override
    public void setScrubbing(boolean scrubbing) {
        animations.setScrubbing(scrubbing);
    }

    @Override
    public void onVisualizationReset() {
        stopActiveAnimation();
        renderedState = emptyState();
        nodeViews
                .values()
                .forEach(view -> view.layoutBoundsProperty().removeListener(elementSizeListener));
        nodeViews.clear();
        edgeViews.clear();
        nodeIdLabels.clear();
        surface.nodeLayer().getChildren().clear();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
        pendingLayout = PendingLayout.empty();
        lastSubmittedState = null;
        hasAppliedLayout = false;
        selectedNodeId = null;
        selectedEdgeId = null;
        pendingSelectedNodeId = null;
        pendingSelectedEdgeId = null;
        firstRender = true;
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        stopActiveAnimation();
        nodeViews
                .values()
                .forEach(view -> view.layoutBoundsProperty().removeListener(elementSizeListener));
        renderFramework.unregisterSurface(SESSION_ID);
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private static boolean requiresStructuralLayout(
            GraphViewState previous, GraphViewState current) {
        if (previous == null
                || previous.directed() != current.directed()
                || !previous.nodes().equals(current.nodes())) return true;
        if (previous.edges().size() != current.edges().size()) return true;
        for (int index = 0; index < previous.edges().size(); index++) {
            GraphViewState.Edge left = previous.edges().get(index);
            GraphViewState.Edge right = current.edges().get(index);
            if (left.id() != right.id()
                    || left.fromId() != right.fromId()
                    || left.toId() != right.toId()) return true;
        }
        return false;
    }

    private record PendingLayout(
            List<Animation> transitions, Set<Long> newNodeIds, boolean initialFrame) {
        private PendingLayout {
            transitions = List.copyOf(transitions);
            newNodeIds = Set.copyOf(newNodeIds);
        }

        private static PendingLayout empty() {
            return new PendingLayout(List.of(), Set.of(), false);
        }
    }

    private static boolean isObservedNode(GraphViewState.Observation observation, long nodeId) {
        return switch (observation.type()) {
            case VISITED -> false;
            case EXAMINED ->
                    (observation.firstNodeId() != null && observation.firstNodeId() == nodeId)
                            || (observation.secondNodeId() != null
                                    && observation.secondNodeId() == nodeId);
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
     * Labels first keep the deterministic side chosen by edge id, then move farther from the edge
     * only when that position intersects a node, node-id label, or an already placed weight.
     */
    private void resolveEdgeLabelCollisions() {
        List<Bounds> occupied = new ArrayList<>();
        for (NodeView node : nodeViews.values()) {
            occupied.add(expanded(node.getBoundsInParent(), EDGE_LABEL_COLLISION_PADDING));
        }
        for (Label nodeIdLabel : nodeIdLabels.values()) {
            if (nodeIdLabel.isVisible()) {
                occupied.add(
                        expanded(nodeIdLabel.getBoundsInParent(), EDGE_LABEL_COLLISION_PADDING));
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
                Bounds candidateBounds =
                        expanded(
                                edge.labelNode().getBoundsInParent(), EDGE_LABEL_COLLISION_PADDING);
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
            occupied.add(
                    expanded(edge.labelNode().getBoundsInParent(), EDGE_LABEL_COLLISION_PADDING));
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
            double width =
                    Math.min(candidate.getMaxX(), other.getMaxX())
                            - Math.max(candidate.getMinX(), other.getMinX());
            double height =
                    Math.min(candidate.getMaxY(), other.getMaxY())
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
                || observation.firstNodeId() == null
                || observation.secondNodeId() == null) {
            return false;
        }
        boolean direct =
                edge.fromId() == observation.firstNodeId()
                        && edge.toId() == observation.secondNodeId();
        if (state.directed()) {
            return direct;
        }
        return direct
                || (edge.fromId() == observation.secondNodeId()
                        && edge.toId() == observation.firstNodeId());
    }

    private static GraphViewState emptyState() {
        return new GraphViewState(
                false, List.of(), List.of(), Set.of(), GraphViewState.Observation.none(), false);
    }

    private static double quantize(double value) {
        return Math.rint(value * 100.0d) / 100.0d;
    }

    private static boolean close(Point2D a, Point2D b) {
        return a.distance(b) <= 0.01d;
    }
}
