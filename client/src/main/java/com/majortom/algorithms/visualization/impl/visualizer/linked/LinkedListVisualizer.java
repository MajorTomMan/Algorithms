package com.majortom.algorithms.visualization.impl.visualizer.linked;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.RectangleGeometry;
import com.majortom.algorithms.visualization.common.layout.EdgeRoute;
import com.majortom.algorithms.visualization.common.layout.ElementBounds;
import com.majortom.algorithms.visualization.common.layout.LayoutResult;
import com.majortom.algorithms.visualization.common.layout.LayoutFailureReporter;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.visualizer.linked.LinkedListElkLayout.LayoutRequest;
import com.majortom.algorithms.visualization.impl.visualizer.linked.LinkedListElkLayout.Link;
import com.majortom.algorithms.visualization.impl.visualizer.linked.LinkedListElkLayout.NodeSize;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.text.Text;
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

/** Linked-list renderer driven by factual topology, measured JavaFX nodes, ELK routes and GestureFX viewport. */
public final class LinkedListVisualizer extends BaseVisualizer<LinkedListViewState> {
    private static final double MIN_NODE_WIDTH = 112.0d;
    private static final double MIN_NODE_HEIGHT = 72.0d;
    private static final double LABEL_HORIZONTAL_PADDING = 40.0d;
    private static final Duration MOVE_DURATION = Duration.millis(260.0d);
    private static final Duration APPEAR_DURATION = Duration.millis(180.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(140.0d);

    private final VisualizationSurface surface = new VisualizationSurface();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final LinkedListElkLayout layout = new LinkedListElkLayout();
    private final ExecutorService layoutExecutor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "linked-list-elk-layout");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong layoutVersion = new AtomicLong();
    private final Map<Long, NodeView> nodeViews = new LinkedHashMap<>();
    private final Map<Long, LinkedNodeDecoration> nodeDecorations = new LinkedHashMap<>();
    private final Map<EdgeKey, EdgeView> edgeViews = new LinkedHashMap<>();
    private final Text headLabel = new Text("HEAD ↓");
    private final Text tailLabel = new Text("↑ TAIL");
    private boolean measuringElements;
    private final InvalidationListener elementSizeListener = observable -> {
        if (!measuringElements) {
            requestRender();
        }
    };

    private LinkedListViewState renderedState = LinkedListViewState.empty();
    private LayoutRequest lastLayoutInput = LayoutRequest.empty();
    private Animation activeAnimation;
    private List<Animation> pendingTransitions = List.of();
    private Set<Long> pendingNewNodeIds = Set.of();
    private long pendingVersion = -1L;
    private boolean firstRender = true;
    private boolean hasAppliedLayout;
    private Long selectedNodeId;
    private LongConsumer selectionListener = ignored -> { };

    public LinkedListVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        surface.setSafeInsets(new javafx.geometry.Insets(26.0d, 16.0d, 62.0d, 16.0d));
        headLabel.getStyleClass().addAll("linear-role-label", "linked-head-label");
        tailLabel.getStyleClass().addAll("linear-role-label", "linked-tail-label");
        surface.decorationLayer().getChildren().addAll(headLabel, tailLabel);
    }

    @Override
    protected void draw(LinkedListViewState state) {
        stopActiveAnimation();
        cleanupDetachedViews();
        List<Animation> transitions = new ArrayList<>();
        Set<Long> newNodeIds = new HashSet<>();

        if (firstRender) {
            surface.markViewportPristine();
        }

        for (LinkedListViewState.Node node : state.nodes().values()) {
            NodeView view = nodeViews.get(node.id());
            if (view == null) {
                view = new NodeView(new RectangleGeometry(MIN_NODE_WIDTH, MIN_NODE_HEIGHT), label(node));
                view.layoutBoundsProperty().addListener(elementSizeListener);
                view.getStyleClass().add("linked-node");
                long nodeId = node.id();
                view.setOnMouseClicked(event -> {
                    selectedNodeId = nodeId;
                    syncSelection();
                    selectionListener.accept(nodeId);
                    event.consume();
                });
                nodeViews.put(node.id(), view);
                surface.nodeLayer().getChildren().add(view);
                LinkedNodeDecoration decoration = new LinkedNodeDecoration(view);
                decoration.setLinks(node.previousId(), node.nextId());
                nodeDecorations.put(node.id(), decoration);
                surface.decorationLayer().getChildren().add(decoration);
                newNodeIds.add(node.id());
                if (!firstRender) {
                    transitions.add(animations.together(
                            animations.fadeIn(view, APPEAR_DURATION),
                            animations.scaleIn(view, APPEAR_DURATION)));
                }
            } else {
                LinkedListViewState.Node previous = renderedState.nodes().get(node.id());
                view.setText(label(node));
                LinkedNodeDecoration decoration = nodeDecorations.get(node.id());
                if (decoration != null) {
                    decoration.setLinks(node.previousId(), node.nextId());
                }
                view.setHighlighted(false);
                if (previous != null && !java.util.Objects.equals(previous.value(), node.value())) {
                    view.setHighlighted(true);
                    PauseTransition clearHighlight = new PauseTransition(Duration.millis(360.0d));
                    NodeView highlightedView = view;
                    clearHighlight.setOnFinished(event -> highlightedView.setHighlighted(false));
                    transitions.add(clearHighlight);
                }
            }
        }

        syncSelection();
        syncEdges(state, transitions);

        List<Long> removedIds = nodeViews.keySet().stream()
                .filter(id -> !state.nodes().containsKey(id))
                .toList();
        for (Long nodeId : removedIds) {
            if (java.util.Objects.equals(selectedNodeId, nodeId)) {
                selectedNodeId = null;
                selectionListener.accept(-1L);
            }
            NodeView view = nodeViews.remove(nodeId);
            view.layoutBoundsProperty().removeListener(elementSizeListener);
            LinkedNodeDecoration decoration = nodeDecorations.remove(nodeId);
            if (decoration != null) {
                decoration.dispose();
                surface.decorationLayer().getChildren().remove(decoration);
            }
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
                surface.fitIfPristine();
            } else {
                scheduleLayout(request, transitions, newNodeIds);
            }
        } else {
            play(transitions, null);
        }

        renderedState = state;
        firstRender = false;
    }

    private LayoutRequest buildLayoutInput(LinkedListViewState state) {
        List<Long> order = orderedNodeIds(state);
        List<NodeSize> nodes = new ArrayList<>(order.size());
        measuringElements = true;
        try {
            for (Long id : order) {
                NodeView view = nodeViews.get(id);
                if (view == null) {
                    continue;
                }
                resizeToMeasuredLabel(view);
                view.applyCss();
                view.autosize();
                Bounds bounds = view.getLayoutBounds();
                nodes.add(new NodeSize(id, quantize(positive(bounds.getWidth(), view.prefWidth(-1.0d))),
                        quantize(positive(bounds.getHeight(), view.prefHeight(-1.0d)))));
            }
        } finally {
            measuringElements = false;
        }

        List<Link> links = new ArrayList<>();
        for (Long id : order) {
            LinkedListViewState.Node node = state.nodes().get(id);
            if (node == null) {
                continue;
            }
            if (node.nextId() != null && state.nodes().containsKey(node.nextId())) {
                EdgeKey key = new EdgeKey(node.id(), node.nextId(), Relation.NEXT);
                links.add(new Link(routeId(key), node.id(), node.nextId()));
            }
        }
        return new LayoutRequest(nodes, links);
    }

    private void resizeToMeasuredLabel(NodeView view) {
        view.applyCss();
        Bounds label = view.labelBounds();
        double width = Math.max(MIN_NODE_WIDTH, Math.ceil(label.getWidth() + LABEL_HORIZONTAL_PADDING));
        double height = MIN_NODE_HEIGHT;
        RectangleGeometry geometry = (RectangleGeometry) view.getGeometry();
        if (Math.abs(geometry.width() - width) > 0.01d || Math.abs(geometry.height() - height) > 0.01d) {
            view.setGeometry(new RectangleGeometry(width, height));
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
            ElementBounds bounds = result.elements().get(LinkedListElkLayout.nodeId(entry.getKey()));
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
                positionRoleLabels(result);
                surface.fitWithMinimumScale(0.78d);
            }
        };
        play(transitions, finish);
    }

    private void applyRoutes(LayoutResult result) {
        for (Map.Entry<EdgeKey, EdgeView> entry : edgeViews.entrySet()) {
            EdgeRoute route = result.edges().get(routeId(entry.getKey()));
            if (route == null || route.points().size() < 2) {
                entry.getValue().clearRoute();
            } else {
                entry.getValue().setRoute(route.points());
            }
        }
    }

    private void positionRoleLabels(LayoutResult result) {
        List<Long> order = orderedNodeIds(renderedState);
        if (order.isEmpty()) {
            headLabel.relocate(40.0d, 30.0d);
            tailLabel.relocate(120.0d, 30.0d);
            return;
        }
        ElementBounds head = result.elements().get(LinkedListElkLayout.nodeId(order.getFirst()));
        ElementBounds tail = result.elements().get(LinkedListElkLayout.nodeId(order.getLast()));
        if (head != null) {
            headLabel.relocate(head.x() + 8.0d, Math.max(2.0d, head.y() - 28.0d));
        }
        if (tail != null) {
            tailLabel.relocate(tail.x() + tail.width() - 42.0d, tail.y() + tail.height() + 10.0d);
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
        LayoutFailureReporter.report("LinkedList", failure);
        play(transitions, null);
    }

    private void syncEdges(LinkedListViewState state, List<Animation> transitions) {
        Map<EdgeKey, EdgeSpec> expected = new LinkedHashMap<>();
        for (LinkedListViewState.Node node : state.nodes().values()) {
            if (node.nextId() != null && state.nodes().containsKey(node.nextId())) {
                EdgeKey key = new EdgeKey(node.id(), node.nextId(), Relation.NEXT);
                expected.put(key, new EdgeSpec(node.id(), node.nextId(), false));
            }
            if (node.previousId() != null && state.nodes().containsKey(node.previousId())) {
                EdgeKey key = new EdgeKey(node.id(), node.previousId(), Relation.PREVIOUS);
                expected.put(key, new EdgeSpec(node.id(), node.previousId(), true));
            }
        }

        for (Map.Entry<EdgeKey, EdgeSpec> entry : expected.entrySet()) {
            if (edgeViews.containsKey(entry.getKey())) {
                continue;
            }
            EdgeSpec spec = entry.getValue();
            NodeView source = nodeViews.get(spec.sourceId());
            NodeView target = nodeViews.get(spec.targetId());
            if (source == null || target == null) {
                continue;
            }
            EdgeView edge = new EdgeView(source, target, true);
            edge.setCurved(spec.curved());
            edge.getStyleClass().add(entry.getKey().relation() == Relation.NEXT ? "linked-next-edge" : "linked-previous-edge");
            edgeViews.put(entry.getKey(), edge);
            surface.edgeLayer().getChildren().add(edge);
            if (!firstRender) {
                transitions.add(animations.reveal(edge, APPEAR_DURATION));
            }
        }

        List<EdgeKey> removed = edgeViews.keySet().stream()
                .filter(key -> !expected.containsKey(key))
                .toList();
        for (EdgeKey key : removed) {
            EdgeView edge = edgeViews.remove(key);
            if (firstRender) {
                surface.edgeLayer().getChildren().remove(edge);
            } else {
                Animation fade = animations.fadeOut(edge, DISAPPEAR_DURATION);
                fade.setOnFinished(event -> surface.edgeLayer().getChildren().remove(edge));
                transitions.add(fade);
            }
        }
    }

    private void clearCurrentRoutes() {
        edgeViews.values().forEach(EdgeView::clearRoute);
    }

    private List<Long> orderedNodeIds(LinkedListViewState state) {
        List<Long> order = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        List<LinkedListViewState.Node> roots = state.nodes().values().stream()
                .filter(node -> node.previousId() == null)
                .sorted(Comparator.comparingLong(LinkedListViewState.Node::id))
                .toList();
        for (LinkedListViewState.Node root : roots) {
            followNext(root.id(), state, visited, order);
        }
        state.nodes().keySet().stream().sorted().forEach(id -> followNext(id, state, visited, order));
        return order;
    }

    private void followNext(long startId, LinkedListViewState state, Set<Long> visited, List<Long> order) {
        Long currentId = startId;
        while (currentId != null && state.nodes().containsKey(currentId) && visited.add(currentId)) {
            order.add(currentId);
            currentId = state.nodes().get(currentId).nextId();
        }
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

    private String label(LinkedListViewState.Node node) {
        return node.value() == null ? "null" : node.value().toString();
    }

    private void stopActiveAnimation() {
        if (activeAnimation != null) {
            activeAnimation.stop();
            activeAnimation = null;
        }
    }

    public void setSelectionListener(LongConsumer listener) {
        selectionListener = listener == null ? ignored -> { } : listener;
    }

    public void clearSelection() {
        selectedNodeId = null;
        syncSelection();
    }

    private void syncSelection() {
        for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
            entry.getValue().setSelected(java.util.Objects.equals(selectedNodeId, entry.getKey()));
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
        renderedState = LinkedListViewState.empty();
        nodeViews.values().forEach(view -> view.layoutBoundsProperty().removeListener(elementSizeListener));
        selectedNodeId = null;
        nodeViews.clear();
        edgeViews.clear();
        surface.nodeLayer().getChildren().clear();
        surface.edgeLayer().getChildren().clear();
        nodeDecorations.values().forEach(LinkedNodeDecoration::dispose);
        nodeDecorations.clear();
        surface.decorationLayer().getChildren().setAll(headLabel, tailLabel);
        pendingTransitions = List.of();
        pendingNewNodeIds = Set.of();
        pendingVersion = -1L;
        hasAppliedLayout = false;
        firstRender = true;
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        stopActiveAnimation();
        invalidateLayout();
        nodeViews.values().forEach(view -> view.layoutBoundsProperty().removeListener(elementSizeListener));
        nodeDecorations.values().forEach(LinkedNodeDecoration::dispose);
        layoutExecutor.shutdownNow();
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private static String routeId(EdgeKey key) {
        return "linked:" + key.relation().name().toLowerCase() + ":" + key.sourceId() + ":" + key.targetId();
    }

    private static double positive(double actual, double fallback) {
        if (actual > 0.0d) {
            return actual;
        }
        return fallback > 0.0d ? fallback : 1.0d;
    }

    private static double quantize(double value) {
        return Math.rint(value * 100.0d) / 100.0d;
    }

    private static boolean close(Point2D a, Point2D b) {
        return a.distance(b) <= 0.01d;
    }

    private enum Relation {
        NEXT,
        PREVIOUS
    }

    private record EdgeKey(long sourceId, long targetId, Relation relation) {}
    private record EdgeSpec(long sourceId, long targetId, boolean curved) {}
}
