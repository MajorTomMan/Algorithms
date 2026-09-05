package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.CircleGeometry;
import com.majortom.algorithms.visualization.common.layout.EdgeRoute;
import com.majortom.algorithms.visualization.common.layout.ElementBounds;
import com.majortom.algorithms.visualization.common.layout.LayoutResult;
import com.majortom.algorithms.visualization.common.layout.LayoutFailureReporter;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.visualizer.tree.TreeElkLayout;
import com.majortom.algorithms.visualization.impl.visualizer.tree.TreeElkLayout.LayoutRequest;
import com.majortom.algorithms.visualization.impl.visualizer.tree.TreeElkLayout.Link;
import com.majortom.algorithms.visualization.impl.visualizer.tree.TreeElkLayout.NodeSize;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
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

/** General/binary/AVL tree renderer using measured JavaFX nodes, transient ELK layout and GestureFX viewport. */
public final class TreeVisualizer extends BaseVisualizer<TreeViewState> {
    private static final double MIN_RADIUS = 24.0d;
    private static final double LABEL_PADDING = 18.0d;
    private static final Duration MOVE_DURATION = Duration.millis(300.0d);
    private static final Duration APPEAR_DURATION = Duration.millis(180.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(140.0d);

    private final VisualizationSurface surface = new VisualizationSurface();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final TreeElkLayout layout = new TreeElkLayout();
    private final ExecutorService layoutExecutor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "tree-elk-layout");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong layoutVersion = new AtomicLong();
    private final Object layoutQueueLock = new Object();
    private LayoutJob queuedLayout;
    private boolean layoutWorkerActive;
    private final Map<Long, NodeView> nodeViews = new LinkedHashMap<>();
    private final Map<Long, Point2D> settledTargets = new LinkedHashMap<>();
    private final Map<EdgeKey, EdgeView> edgeViews = new LinkedHashMap<>();
    private boolean measuringElements;
    private final InvalidationListener elementSizeListener = observable -> {
        if (!measuringElements) {
            requestRender();
        }
    };

    private TreeViewState renderedState = TreeViewState.empty(TreeViewState.Kind.GENERAL);
    private LayoutRequest lastLayoutInput = LayoutRequest.empty();
    private Animation activeAnimation;
    private List<Animation> pendingTransitions = List.of();
    private Set<Long> pendingNewNodeIds = Set.of();
    private long pendingVersion = -1L;
    private boolean firstRender = true;
    private boolean hasAppliedLayout;
    private Long selectedNodeId;
    private LongConsumer selectionListener = ignored -> { };

    public TreeVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
    }

    @Override
    protected void draw(TreeViewState state) {
        if (layoutAffectingChange(renderedState, state)) {
            stopActiveAnimation();
        }
        cleanupDetachedViews();
        List<Animation> transitions = new ArrayList<>();
        Set<Long> newNodeIds = new HashSet<>();

        if (firstRender) {
            surface.markViewportPristine();
        }

        for (TreeViewState.Node node : state.nodes().values()) {
            NodeView view = nodeViews.get(node.id());
            if (view == null) {
                view = new NodeView(new CircleGeometry(MIN_RADIUS), Integer.toString(node.value()));
                view.layoutBoundsProperty().addListener(elementSizeListener);
                long visualNodeId = node.id();
                view.setOnMouseClicked(event -> {
                    selectedNodeId = visualNodeId;
                    syncSelectionState();
                    selectionListener.accept(visualNodeId);
                    event.consume();
                });
                nodeViews.put(node.id(), view);
                surface.nodeLayer().getChildren().add(view);
                newNodeIds.add(node.id());
                if (!firstRender) {
                    transitions.add(animations.together(
                            animations.fadeIn(view, APPEAR_DURATION),
                            animations.scaleIn(view, APPEAR_DURATION)));
                }
            } else {
                view.setText(Integer.toString(node.value()));
            }
            view.setCurrent(state.currentNodeIds().contains(node.id()));
            view.setHighlighted(state.observedNodeIds().contains(node.id()));
            view.setVisited(state.visitedNodeIds().contains(node.id()));
        }

        syncSelectionState();
        syncEdges(state, transitions);

        List<Long> removedIds = nodeViews.keySet().stream()
                .filter(id -> !state.nodes().containsKey(id))
                .toList();
        for (Long nodeId : removedIds) {
            NodeView view = nodeViews.remove(nodeId);
            settledTargets.remove(nodeId);
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
                surface.fitWithMinimumScale(0.78d);
            } else {
                scheduleLayout(request, transitions, newNodeIds);
            }
        } else {
            play(transitions, null);
        }

        renderedState = state;
        firstRender = false;
    }

    private LayoutRequest buildLayoutInput(TreeViewState state) {
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
                CircleGeometry geometry = (CircleGeometry) view.getGeometry();
                nodes.add(new NodeSize(id, quantize(geometry.width()), quantize(geometry.height())));
            }
        } finally {
            measuringElements = false;
        }

        List<Link> links = new ArrayList<>();
        for (Long id : order) {
            TreeViewState.Node node = state.nodes().get(id);
            if (node == null) {
                continue;
            }
            if (state.kind() == TreeViewState.Kind.GENERAL) {
                for (int index = 0; index < node.childIds().size(); index++) {
                    Long targetId = node.childIds().get(index);
                    if (targetId == null || !state.nodes().containsKey(targetId)) {
                        continue;
                    }
                    EdgeKey key = new EdgeKey(node.id(), targetId, Relation.CHILD, index);
                    links.add(new Link(routeId(key), node.id(), targetId, TreeElkLayout.Relation.CHILD, index));
                }
            } else {
                addLayoutLink(links, state, node.id(), node.leftId(), Relation.LEFT, 0);
                addLayoutLink(links, state, node.id(), node.rightId(), Relation.RIGHT, 1);
            }
        }

        TreeElkLayout.Kind kind = state.kind() == TreeViewState.Kind.GENERAL
                ? TreeElkLayout.Kind.GENERAL : TreeElkLayout.Kind.BINARY;
        return new LayoutRequest(kind, nodes, links);
    }

    private void addLayoutLink(List<Link> links, TreeViewState state, long sourceId, Long targetId, Relation relation, int index) {
        if (targetId == null || !state.nodes().containsKey(targetId)) {
            return;
        }
        EdgeKey key = new EdgeKey(sourceId, targetId, relation, index);
        TreeElkLayout.Relation elkRelation = relation == Relation.LEFT
                ? TreeElkLayout.Relation.LEFT : TreeElkLayout.Relation.RIGHT;
        links.add(new Link(routeId(key), sourceId, targetId, elkRelation, index));
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

        boolean startWorker = false;
        synchronized (layoutQueueLock) {
            queuedLayout = new LayoutJob(version, request);
            if (!layoutWorkerActive) {
                layoutWorkerActive = true;
                startWorker = true;
            }
        }
        if (startWorker) {
            layoutExecutor.execute(this::runLatestLayouts);
        }
    }

    private void runLatestLayouts() {
        while (!Thread.currentThread().isInterrupted()) {
            LayoutJob job;
            synchronized (layoutQueueLock) {
                job = queuedLayout;
                queuedLayout = null;
                if (job == null) {
                    layoutWorkerActive = false;
                    return;
                }
            }
            try {
                LayoutResult result = layout.layout(job.request());
                Platform.runLater(() -> applyLayout(job.version(), result));
            } catch (Throwable failure) {
                Platform.runLater(() -> handleLayoutFailure(job.version(), failure));
            }
        }
        synchronized (layoutQueueLock) {
            layoutWorkerActive = false;
        }
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
            ElementBounds bounds = result.elements().get(TreeElkLayout.nodeId(entry.getKey()));
            if (bounds == null) {
                continue;
            }
            Point2D target = new Point2D(bounds.x() + bounds.width() / 2.0d, bounds.y() + bounds.height() / 2.0d);
            settledTargets.put(entry.getKey(), target);
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

    private void handleLayoutFailure(long version, Throwable failure) {
        if (isDisposed() || version != layoutVersion.get()) {
            return;
        }
        List<Animation> transitions = pendingVersion == version ? pendingTransitions : List.of();
        pendingVersion = -1L;
        pendingTransitions = List.of();
        pendingNewNodeIds = Set.of();
        LayoutFailureReporter.report("Tree", failure);
        play(transitions, null);
    }

    private void syncEdges(TreeViewState state, List<Animation> transitions) {
        Map<EdgeKey, EdgeSpec> expected = new LinkedHashMap<>();
        for (TreeViewState.Node node : state.nodes().values()) {
            if (state.kind() == TreeViewState.Kind.GENERAL) {
                for (int index = 0; index < node.childIds().size(); index++) {
                    addExpectedEdge(expected, state, node.id(), node.childIds().get(index), Relation.CHILD, index);
                }
            } else {
                addExpectedEdge(expected, state, node.id(), node.leftId(), Relation.LEFT, 0);
                addExpectedEdge(expected, state, node.id(), node.rightId(), Relation.RIGHT, 1);
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
            EdgeView edge = new EdgeView(source, target, false);
            edge.setCurved(source == target);
            edge.getStyleClass().add(entry.getKey().relation().styleClass());
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

    private void addExpectedEdge(Map<EdgeKey, EdgeSpec> expected, TreeViewState state,
            long sourceId, Long targetId, Relation relation, int index) {
        if (targetId == null || !state.nodes().containsKey(targetId)) {
            return;
        }
        EdgeKey key = new EdgeKey(sourceId, targetId, relation, index);
        expected.put(key, new EdgeSpec(sourceId, targetId));
    }

    private void clearCurrentRoutes() {
        edgeViews.values().forEach(EdgeView::clearRoute);
    }

    private List<Long> orderedNodeIds(TreeViewState state) {
        List<Long> order = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        if (state.rootId() != null) {
            visit(state.rootId(), state, visited, order);
        }
        state.nodes().keySet().stream().sorted(Comparator.naturalOrder())
                .forEach(id -> visit(id, state, visited, order));
        return order;
    }

    private void visit(long id, TreeViewState state, Set<Long> visited, List<Long> order) {
        if (!state.nodes().containsKey(id) || !visited.add(id)) {
            return;
        }
        order.add(id);
        TreeViewState.Node node = state.nodes().get(id);
        if (state.kind() == TreeViewState.Kind.GENERAL) {
            for (Long childId : node.childIds()) {
                if (childId != null) {
                    visit(childId, state, visited, order);
                }
            }
        } else {
            if (node.leftId() != null) {
                visit(node.leftId(), state, visited, order);
            }
            if (node.rightId() != null) {
                visit(node.rightId(), state, visited, order);
            }
        }
    }

    private void cleanupDetachedViews() {
        surface.nodeLayer().getChildren().removeIf(node -> node instanceof NodeView && !nodeViews.containsValue(node));
        surface.edgeLayer().getChildren().removeIf(node -> node instanceof EdgeView && !edgeViews.containsValue(node));
    }

    public void setSelectionListener(LongConsumer listener) {
        selectionListener = listener == null ? ignored -> { } : listener;
    }

    public void clearSelection() {
        selectedNodeId = null;
        syncSelectionState();
    }

    public Long selectedNodeId() {
        return selectedNodeId;
    }

    private void syncSelectionState() {
        for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
            entry.getValue().setSelected(selectedNodeId != null && selectedNodeId.equals(entry.getKey()));
        }
    }

    private void invalidateLayout() {
        layoutVersion.incrementAndGet();
        lastLayoutInput = LayoutRequest.empty();
        synchronized (layoutQueueLock) {
            queuedLayout = null;
        }
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
        if (activeAnimation != null && activeAnimation.getStatus() == Animation.Status.RUNNING) {
            if (onFinished != null) {
                parallel.setOnFinished(event -> onFinished.run());
            }
            parallel.play();
            return;
        }
        activeAnimation = parallel;
        parallel.setOnFinished(event -> {
            if (activeAnimation == parallel) {
                activeAnimation = null;
            }
            if (onFinished != null) {
                onFinished.run();
            }
        });
        parallel.play();
    }

    private boolean layoutAffectingChange(TreeViewState previous, TreeViewState current) {
        return previous.kind() != current.kind()
                || !java.util.Objects.equals(previous.rootId(), current.rootId())
                || !previous.nodes().equals(current.nodes());
    }

    private void stopActiveAnimation() {
        if (activeAnimation != null) {
            activeAnimation.stop();
            activeAnimation = null;
        }
        // A new factual frame may arrive before a presentation fade/scale completes.
        // Never allow an interrupted animation to become persistent geometry/style state.
        nodeViews.forEach((id, view) -> {
            Point2D target = settledTargets.get(id);
            if (target != null) {
                view.setCenter(target.getX(), target.getY());
            }
            view.setOpacity(1.0d);
            view.setScaleX(1.0d);
            view.setScaleY(1.0d);
        });
        edgeViews.values().forEach(edge -> edge.setOpacity(1.0d));
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
        renderedState = TreeViewState.empty(TreeViewState.Kind.GENERAL);
        nodeViews.values().forEach(view -> view.layoutBoundsProperty().removeListener(elementSizeListener));
        nodeViews.clear();
        settledTargets.clear();
        edgeViews.clear();
        surface.nodeLayer().getChildren().clear();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
        pendingTransitions = List.of();
        pendingNewNodeIds = Set.of();
        pendingVersion = -1L;
        hasAppliedLayout = false;
        selectedNodeId = null;
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

    private static String routeId(EdgeKey key) {
        return "tree:" + key.relation().name().toLowerCase() + ":" + key.index() + ":"
                + key.sourceId() + ":" + key.targetId();
    }

    private static double quantize(double value) {
        return Math.rint(value * 100.0d) / 100.0d;
    }

    private static boolean close(Point2D a, Point2D b) {
        return a.distance(b) <= 0.01d;
    }

    private enum Relation {
        CHILD("tree-child-edge"),
        LEFT("tree-left-edge"),
        RIGHT("tree-right-edge");

        private final String styleClass;

        Relation(String styleClass) {
            this.styleClass = styleClass;
        }

        private String styleClass() {
            return styleClass;
        }
    }

    private record LayoutJob(long version, LayoutRequest request) {}

    private record EdgeKey(long sourceId, long targetId, Relation relation, int index) {}
    private record EdgeSpec(long sourceId, long targetId) {}
}
