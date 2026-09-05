package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.geometry.CircleGeometry;
import com.majortom.algorithms.visualization.common.layout.EdgeRoute;
import com.majortom.algorithms.visualization.common.layout.ElementBounds;
import com.majortom.algorithms.visualization.common.layout.LayoutResult;
import com.majortom.algorithms.visualization.common.layout.LayoutFailureReporter;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphElkLayout;
import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphElkLayout.LayoutRequest;
import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphElkLayout.Link;
import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphElkLayout.NodeSize;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

/** Graph renderer using measured JavaFX nodes, transient ELK Layered routes and GestureFX viewport. */
public final class GraphVisualizer extends BaseVisualizer<GraphViewState> {
    private static final double MIN_RADIUS = 28.0d;
    private static final double LABEL_PADDING = 24.0d;
    private static final Duration MOVE_DURATION = Duration.millis(300.0d);
    private static final Duration APPEAR_DURATION = Duration.millis(180.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(140.0d);

    private final VisualizationSurface surface = new VisualizationSurface();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final GraphElkLayout layout = new GraphElkLayout();
    private final ExecutorService layoutExecutor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "graph-elk-layout");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong layoutVersion = new AtomicLong();
    private final Map<Long, NodeView> nodeViews = new LinkedHashMap<>();
    private final Map<Long, EdgeView> edgeViews = new LinkedHashMap<>();
    private final Map<Long, Label> nodeIdLabels = new LinkedHashMap<>();
    private boolean measuringElements;
    private final InvalidationListener elementSizeListener = observable -> {
        if (!measuringElements) {
            requestRender();
        }
    };

    private GraphViewState renderedState = emptyState();
    private LayoutRequest lastLayoutInput = LayoutRequest.empty();
    private Animation activeAnimation;
    private List<Animation> pendingTransitions = List.of();
    private Set<Long> pendingNewNodeIds = Set.of();
    private long pendingVersion = -1L;
    private boolean firstRender = true;
    private boolean hasAppliedLayout;
    private Long selectedNodeId;
    private Long selectedEdgeId;
    private LongConsumer nodeSelectionListener = ignored -> { };
    private LongConsumer edgeSelectionListener = ignored -> { };
    private VisualDensity density = VisualDensity.DETAIL;

    public GraphVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
    }

    @Override
    protected void draw(GraphViewState state) {
        stopActiveAnimation();
        cleanupDetachedViews();
        List<Animation> transitions = new ArrayList<>();
        Set<Long> newNodeIds = new HashSet<>();

        if (firstRender) {
            surface.markViewportPristine();
        }

        density = densityFor(state.nodes().size());
        Map<Long, GraphViewState.Node> previousNodes = renderedState.nodesById();
        for (GraphViewState.Node node : state.nodes()) {
            NodeView view = nodeViews.get(node.id());
            if (view == null) {
                view = new NodeView(new CircleGeometry(MIN_RADIUS), Integer.toString(node.value()));
                view.layoutBoundsProperty().addListener(elementSizeListener);
                long visualNodeId = node.id();
                view.setOnMouseClicked(event -> {
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
                    transitions.add(animations.together(
                            animations.fadeIn(view, APPEAR_DURATION),
                            animations.scaleIn(view, APPEAR_DURATION)));
                }
            } else {
                view.setText(Integer.toString(node.value()));
            }
            view.setVisited(state.visitedNodeIds().contains(node.id()));
            view.setHighlighted(isObservedNode(state.observation(), node.id()));
        }

        syncEdges(state, transitions);
        syncSelectionState();

        Set<Long> currentNodeIds = state.nodes().stream().map(GraphViewState.Node::id)
                .collect(java.util.stream.Collectors.toSet());
        List<Long> removedIds = nodeViews.keySet().stream()
                .filter(id -> !currentNodeIds.contains(id))
                .toList();
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

        LayoutRequest request = buildLayoutInput(state);
        boolean layoutChanged = !request.equals(lastLayoutInput);
        if (layoutChanged) {
            lastLayoutInput = request;
            if (request.nodes().isEmpty()) {
                invalidateLayout();
                pendingTransitions = List.of();
                pendingNewNodeIds = Set.of();
                pendingVersion = -1L;
                hasAppliedLayout = false;
                play(transitions, null);
                surface.fitWithMinimumScale(0.72d);
            } else {
                scheduleLayout(request, transitions, newNodeIds);
            }
        } else {
            play(transitions, null);
        }

        renderedState = state;
        firstRender = false;
    }

    private LayoutRequest buildLayoutInput(GraphViewState state) {
        List<GraphViewState.Node> orderedNodes = state.nodes().stream()
                .sorted(Comparator.comparingLong(GraphViewState.Node::id)).toList();
        List<NodeSize> nodes = new ArrayList<>(orderedNodes.size());
        measuringElements = true;
        try {
            for (GraphViewState.Node node : orderedNodes) {
                NodeView view = nodeViews.get(node.id());
                if (view == null) {
                    continue;
                }
                resizeToMeasuredLabel(view);
                CircleGeometry geometry = (CircleGeometry) view.getGeometry();
                nodes.add(new NodeSize(node.id(), quantize(geometry.width()), quantize(geometry.height())));
            }
        } finally {
            measuringElements = false;
        }

        Set<Long> available = nodes.stream().map(NodeSize::id).collect(java.util.stream.Collectors.toSet());
        List<Link> links = state.edges().stream()
                .filter(edge -> available.contains(edge.fromId()) && available.contains(edge.toId()))
                .sorted(Comparator.comparingLong(GraphViewState.Edge::id))
                .map(edge -> new Link(GraphElkLayout.edgeId(edge.id()), edge.id(), edge.fromId(), edge.toId()))
                .toList();
        return new LayoutRequest(state.directed(), nodes, links);
    }

    private void resizeToMeasuredLabel(NodeView view) {
        view.applyCss();
        Bounds label = view.labelBounds();
        double diameter = Math.max(MIN_RADIUS * 2.0d,
                Math.ceil(Math.max(label.getWidth(), label.getHeight()) + LABEL_PADDING));
        double radius = diameter / 2.0d;
        CircleGeometry geometry = (CircleGeometry) view.getGeometry();
        if (Math.abs(geometry.radius() - radius) > 0.01d) {
            view.setGeometry(new CircleGeometry(radius));
        }
    }

    private void scheduleLayout(LayoutRequest request, List<Animation> transitions, Set<Long> newNodeIds) {
        long version = layoutVersion.incrementAndGet();
        pendingVersion = version;
        pendingTransitions = List.copyOf(transitions);
        pendingNewNodeIds = Set.copyOf(newNodeIds);
        layoutExecutor.execute(() -> {
            try {
                LayoutResult result = layout.layout(request);
                Platform.runLater(() -> applyLayout(version, result));
            } catch (Throwable failure) {
                Platform.runLater(() -> handleLayoutFailure(version, failure));
            }
        });
    }

    private void applyLayout(long version, LayoutResult result) {
        if (isDisposed() || version != layoutVersion.get()) {
            return;
        }

        clearCurrentRoutes();
        List<Animation> transitions = new ArrayList<>();
        if (pendingVersion == version) {
            transitions.addAll(pendingTransitions);
        }
        Set<Long> newNodeIds = pendingVersion == version ? pendingNewNodeIds : Set.of();

        for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
            ElementBounds bounds = result.elements().get(GraphElkLayout.nodeId(entry.getKey()));
            if (bounds == null) {
                continue;
            }
            Point2D target = new Point2D(bounds.x() + bounds.width() / 2.0d, bounds.y() + bounds.height() / 2.0d);
            NodeView view = entry.getValue();
            if (!hasAppliedLayout || newNodeIds.contains(entry.getKey())) {
                view.setCenter(target.getX(), target.getY());
            } else if (!close(view.center(), target)) {
                transitions.add(animations.move(view, target, MOVE_DURATION));
            }
        }

        pendingVersion = -1L;
        pendingTransitions = List.of();
        pendingNewNodeIds = Set.of();
        hasAppliedLayout = true;

        Runnable finish = () -> {
            if (!isDisposed() && version == layoutVersion.get()) {
                applyRoutes(result);
                surface.fitWithMinimumScale(0.72d);
            }
        };
        play(transitions, finish);
    }

    private void applyRoutes(LayoutResult result) {
        for (Map.Entry<Long, EdgeView> entry : edgeViews.entrySet()) {
            EdgeRoute route = result.edges().get(GraphElkLayout.edgeId(entry.getKey()));
            if (route == null || route.points().size() < 2) {
                entry.getValue().clearRoute();
            } else {
                entry.getValue().setRoute(route.points());
            }
        }
    }

    private void handleLayoutFailure(long version, Throwable failure) {
        if (isDisposed() || version != layoutVersion.get()) {
            return;
        }
        List<Animation> transitions = pendingVersion == version ? pendingTransitions : List.of();
        pendingVersion = -1L;
        pendingTransitions = List.of();
        pendingNewNodeIds = Set.of();
        LayoutFailureReporter.report("Graph", failure);
        play(transitions, null);
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
                existing.setHighlighted(isObservedEdge(state, edge));
                continue;
            }
            NodeView source = nodeViews.get(edge.fromId());
            NodeView target = nodeViews.get(edge.toId());
            if (source == null || target == null) {
                continue;
            }
            EdgeView view = new EdgeView(source, target, state.directed());
            long visualEdgeId = edge.id();
            view.setOnMouseClicked(event -> {
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

        List<Long> removed = edgeViews.keySet().stream()
                .filter(id -> !expected.containsKey(id)).toList();
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
        nodeSelectionListener = listener == null ? ignored -> { } : listener;
    }

    public void setEdgeSelectionListener(LongConsumer listener) {
        edgeSelectionListener = listener == null ? ignored -> { } : listener;
    }

    public void clearSelection() {
        selectedNodeId = null;
        selectedEdgeId = null;
        syncSelectionState();
    }

    public Long selectedNodeId() {
        return selectedNodeId;
    }

    public Long selectedEdgeId() {
        return selectedEdgeId;
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
        label.layoutXProperty().bind(view.centerXProperty().subtract(label.widthProperty().divide(2.0d)));
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

    private void clearCurrentRoutes() {
        edgeViews.values().forEach(EdgeView::clearRoute);
    }

    private void cleanupDetachedViews() {
        surface.nodeLayer().getChildren().removeIf(node -> node instanceof NodeView && !nodeViews.containsValue(node));
        surface.edgeLayer().getChildren().removeIf(node -> node instanceof EdgeView && !edgeViews.containsValue(node));
    }

    private void invalidateLayout() {
        layoutVersion.incrementAndGet();
        lastLayoutInput = LayoutRequest.empty();
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
        if (onFinished != null) {
            parallel.setOnFinished(event -> onFinished.run());
        }
        activeAnimation = parallel;
        parallel.play();
    }

    private void stopActiveAnimation() {
        if (activeAnimation != null) {
            activeAnimation.stop();
            activeAnimation = null;
        }
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
        invalidateLayout();
        renderedState = emptyState();
        nodeViews.values().forEach(view -> view.layoutBoundsProperty().removeListener(elementSizeListener));
        nodeViews.clear();
        edgeViews.clear();
        nodeIdLabels.clear();
        surface.nodeLayer().getChildren().clear();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
        pendingTransitions = List.of();
        pendingNewNodeIds = Set.of();
        pendingVersion = -1L;
        hasAppliedLayout = false;
        selectedNodeId = null;
        selectedEdgeId = null;
        firstRender = true;
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        stopActiveAnimation();
        invalidateLayout();
        nodeViews.values().forEach(view -> view.layoutBoundsProperty().removeListener(elementSizeListener));
        layoutExecutor.shutdownNow();
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private static boolean isObservedNode(GraphViewState.Observation observation, long nodeId) {
        return switch (observation.type()) {
            case VISITED -> false;
            case EXAMINED -> (observation.firstNodeId() != null && observation.firstNodeId() == nodeId)
                    || (observation.secondNodeId() != null && observation.secondNodeId() == nodeId);
            case NONE -> false;
        };
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

    private static GraphViewState emptyState() {
        return new GraphViewState(false, List.of(), List.of(), GraphViewState.Observation.none(), false);
    }

    private static double quantize(double value) {
        return Math.rint(value * 100.0d) / 100.0d;
    }

    private static boolean close(Point2D a, Point2D b) {
        return a.distance(b) <= 0.01d;
    }
}
