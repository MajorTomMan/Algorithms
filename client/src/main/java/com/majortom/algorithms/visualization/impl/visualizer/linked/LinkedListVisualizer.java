package com.majortom.algorithms.visualization.impl.visualizer.linked;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.RectangleGeometry;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.international.I18N;
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
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;

import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/**
 * Linked-list renderer whose scheduling, stale rejection and Camera are owned by RenderFramework.
 */
public final class LinkedListVisualizer extends BaseVisualizer<LinkedListViewState>
        implements FxSurfaceAdapter<LinkedListViewState> {
    private static final RenderSessionId SESSION_ID = RenderSessionId.of("LINKED_LIST");
    private static final double MIN_NODE_WIDTH = 112.0d;
    private static final double MIN_NODE_HEIGHT = 72.0d;
    private static final double LABEL_HORIZONTAL_PADDING = 40.0d;
    private static final Duration MOVE_DURATION = Duration.millis(260.0d);
    private static final Duration APPEAR_DURATION = Duration.millis(180.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(140.0d);

    private final VisualizationSurface surface = new VisualizationSurface();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final DefaultRenderFramework renderFramework = RenderRuntime.shared();
    private final Map<Long, NodeView> nodeViews = new LinkedHashMap<>();
    private final Map<Long, LinkedNodeDecoration> nodeDecorations = new LinkedHashMap<>();
    private final Map<EdgeKey, EdgeView> edgeViews = new LinkedHashMap<>();
    private final Text headLabel = new Text();
    private final Text tailLabel = new Text();
    private boolean measuringElements;
    private final InvalidationListener elementSizeListener =
            observable -> {
                if (!measuringElements) requestGeometryRefresh();
            };

    private volatile LinkedListViewState lastSubmittedState;
    private LinkedListViewState renderedState = LinkedListViewState.empty();
    private PendingLayout pendingLayout = PendingLayout.empty();
    private LayoutPatch lastPatch;
    private Animation activeAnimation;
    private CompletableFuture<Void> activeAnimationFuture;
    private boolean hasAppliedLayout;
    private Long selectedNodeId;
    private Long pendingSelectedNodeId;
    private LongConsumer selectionListener = ignored -> {};

    public LinkedListVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        surface.setSafeInsets(new javafx.geometry.Insets(26.0d, 16.0d, 62.0d, 16.0d));
        surface.setFrameworkManagedCamera(true);
        headLabel.textProperty().bind(I18N.createStringBinding("label.visual.linked.head"));
        tailLabel.textProperty().bind(I18N.createStringBinding("label.visual.linked.tail"));
        headLabel.getStyleClass().addAll("linear-role-label", "linked-head-label");
        tailLabel.getStyleClass().addAll("linear-role-label", "linked-tail-label");
        surface.decorationLayer().getChildren().addAll(headLabel, tailLabel);
        renderFramework.registerSurface(SESSION_ID, this);
    }

    @Override
    protected synchronized void submitFrameworkRender(LinkedListViewState state) {
        LinkedListViewState previous = lastSubmittedState;
        boolean initial = previous == null;
        boolean structural = initial || !previous.nodes().equals(state.nodes());
        lastSubmittedState = state;
        if (structural) {
            renderFramework.submit(
                    new StructuralRenderIntent<>(
                            SESSION_ID,
                            state,
                            initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE,
                            initial));
        } else {
            renderFramework.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
    }

    private void requestGeometryRefresh() {
        LinkedListViewState state = currentState();
        if (state == null || !isModuleAttached() || isDisposed()) return;
        renderFramework.submit(
                new StructuralRenderIntent<>(
                        SESSION_ID,
                        state,
                        CameraPolicy.ENSURE_VISIBLE,
                        false,
                        StructuralChange.GEOMETRY));
    }

    @Override
    public LayoutRequest captureLayout(LinkedListViewState state, RenderCaptureContext context) {
        stopActiveAnimation();
        cleanupDetachedViews();
        List<Animation> transitions = new ArrayList<>();
        Set<Long> newNodeIds = new HashSet<>();

        for (LinkedListViewState.Node node : state.nodes().values()) {
            NodeView view = nodeViews.get(node.id());
            if (view == null) {
                view = createNode(node);
                nodeViews.put(node.id(), view);
                surface.nodeLayer().getChildren().add(view);
                LinkedNodeDecoration decoration = new LinkedNodeDecoration(view);
                decoration.setLinks(node.previousId(), node.nextId());
                nodeDecorations.put(node.id(), decoration);
                surface.decorationLayer().getChildren().add(decoration);
                newNodeIds.add(node.id());
                if (!context.initialFrame() && !animations.isScrubbing()) {
                    view.setOpacity(0.0d);
                    view.setScaleX(0.86d);
                    view.setScaleY(0.86d);
                    transitions.add(
                            animations.together(
                                    animations.fadeIn(view, APPEAR_DURATION),
                                    animations.scaleIn(view, APPEAR_DURATION)));
                }
            } else {
                LinkedListViewState.Node previous = renderedState.nodes().get(node.id());
                view.setText(label(node));
                LinkedNodeDecoration decoration = nodeDecorations.get(node.id());
                if (decoration != null) decoration.setLinks(node.previousId(), node.nextId());
                view.setHighlighted(false);
                if (previous != null
                        && !java.util.Objects.equals(previous.value(), node.value())
                        && !animations.isScrubbing()) {
                    view.setHighlighted(true);
                    PauseTransition clearHighlight = new PauseTransition(Duration.millis(360.0d));
                    NodeView highlighted = view;
                    clearHighlight.setOnFinished(event -> highlighted.setHighlighted(false));
                    transitions.add(clearHighlight);
                }
            }
        }

        applyPendingSelection(state);
        syncSelection();
        syncEdges(state, transitions, context.initialFrame());

        List<Long> removedIds =
                nodeViews.keySet().stream().filter(id -> !state.nodes().containsKey(id)).toList();
        for (Long nodeId : removedIds) removeNode(nodeId, transitions, context.initialFrame());

        List<Long> order = orderedNodeIds(state);
        List<LayoutElement> elements = measureNodes(order);
        List<LayoutLink> links = new ArrayList<>();
        for (Long id : order) {
            LinkedListViewState.Node node = state.nodes().get(id);
            if (node != null && node.nextId() != null && state.nodes().containsKey(node.nextId())) {
                EdgeKey key = new EdgeKey(node.id(), node.nextId(), Relation.NEXT);
                links.add(
                        new LayoutLink(
                                routeId(key),
                                LinkedListElkLayout.nodeId(node.id()),
                                LinkedListElkLayout.nodeId(node.nextId()),
                                "NEXT",
                                links.size()));
            }
        }
        pendingLayout = new PendingLayout(transitions, newNodeIds);
        return new LayoutRequest(
                context.requestId(),
                context.sessionId(),
                context.modelRevision(),
                context.geometryRevision(),
                LinkedListElkLayout.ID,
                elements,
                links,
                Map.of("structure", "linked-list"));
    }

    @Override
    public CompletionStage<Void> commitLayout(
            LinkedListViewState state, LayoutPatch patch, RenderCommitContext context) {
        clearCurrentRoutes();
        PendingLayout pending = pendingLayout;
        List<Animation> transitions = new ArrayList<>(pending.transitions());
        boolean snap = context.initialFrame() || animations.isScrubbing();
        for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
            ElementGeometry bounds =
                    patch.elements().get(LinkedListElkLayout.nodeId(entry.getKey()));
            if (bounds == null) continue;
            Point2D target = center(bounds);
            NodeView view = entry.getValue();
            if (snap || !hasAppliedLayout || pending.newNodeIds().contains(entry.getKey())) {
                view.setCenter(target.getX(), target.getY());
                if (snap) {
                    view.setOpacity(1.0d);
                    view.setScaleX(1.0d);
                    view.setScaleY(1.0d);
                }
            } else if (!close(view.center(), target)) {
                transitions.add(animations.move(view, target, MOVE_DURATION));
            }
        }

        pendingLayout = PendingLayout.empty();
        hasAppliedLayout = !patch.elements().isEmpty();
        renderedState = state;
        lastPatch = patch;
        Runnable finish =
                () -> {
                    applyRoutes(patch);
                    positionRoleLabels(state, patch);
                    cleanupDetachedViews();
                };
        if (snap || transitions.isEmpty()) {
            transitions.forEach(Animation::stop);
            finish.run();
            return CompletableFuture.completedFuture(null);
        }
        return playAsync(transitions).thenRun(finish);
    }

    @Override
    public CompletionStage<Void> commitPresentation(
            LinkedListViewState state, RenderCommitContext context) {
        stopActiveAnimation();
        applyPendingSelection(state);
        syncSelection();
        if (lastPatch != null) positionRoleLabels(state, lastPatch);
        return CompletableFuture.completedFuture(null);
    }

    private NodeView createNode(LinkedListViewState.Node node) {
        NodeView view =
                new NodeView(new RectangleGeometry(MIN_NODE_WIDTH, MIN_NODE_HEIGHT), label(node));
        view.layoutBoundsProperty().addListener(elementSizeListener);
        view.getStyleClass().add("linked-node");
        long nodeId = node.id();
        view.setOnMouseClicked(
                event -> {
                    selectedNodeId = nodeId;
                    syncSelection();
                    selectionListener.accept(nodeId);
                    event.consume();
                });
        return view;
    }

    private void removeNode(long nodeId, List<Animation> transitions, boolean initialFrame) {
        if (java.util.Objects.equals(selectedNodeId, nodeId)) {
            selectedNodeId = null;
            selectionListener.accept(-1L);
        }
        NodeView view = nodeViews.remove(nodeId);
        if (view == null) return;
        view.layoutBoundsProperty().removeListener(elementSizeListener);
        LinkedNodeDecoration decoration = nodeDecorations.remove(nodeId);
        if (decoration != null) {
            decoration.dispose();
            surface.decorationLayer().getChildren().remove(decoration);
        }
        if (initialFrame || animations.isScrubbing()) {
            surface.nodeLayer().getChildren().remove(view);
        } else {
            Animation fade = animations.fadeOut(view, DISAPPEAR_DURATION);
            fade.setOnFinished(event -> surface.nodeLayer().getChildren().remove(view));
            transitions.add(fade);
        }
    }

    private List<LayoutElement> measureNodes(List<Long> order) {
        measuringElements = true;
        try {
            List<LayoutElement> elements = new ArrayList<>(order.size());
            for (Long id : order) {
                NodeView view = nodeViews.get(id);
                if (view == null) continue;
                resizeToMeasuredLabel(view);
                view.applyCss();
                view.autosize();
                Bounds bounds = view.getLayoutBounds();
                elements.add(
                        new LayoutElement(
                                LinkedListElkLayout.nodeId(id),
                                quantize(positive(bounds.getWidth(), view.prefWidth(-1.0d))),
                                quantize(positive(bounds.getHeight(), view.prefHeight(-1.0d)))));
            }
            return List.copyOf(elements);
        } finally {
            measuringElements = false;
        }
    }

    private void resizeToMeasuredLabel(NodeView view) {
        view.applyCss();
        Bounds label = view.labelBounds();
        double width =
                Math.max(MIN_NODE_WIDTH, Math.ceil(label.getWidth() + LABEL_HORIZONTAL_PADDING));
        RectangleGeometry geometry = (RectangleGeometry) view.getGeometry();
        if (Math.abs(geometry.width() - width) > 0.01d
                || Math.abs(geometry.height() - MIN_NODE_HEIGHT) > 0.01d) {
            view.setGeometry(new RectangleGeometry(width, MIN_NODE_HEIGHT));
        }
    }

    private void syncEdges(
            LinkedListViewState state, List<Animation> transitions, boolean initialFrame) {
        Map<EdgeKey, EdgeSpec> expected = new LinkedHashMap<>();
        for (LinkedListViewState.Node node : state.nodes().values()) {
            if (node.nextId() != null && state.nodes().containsKey(node.nextId())) {
                expected.put(
                        new EdgeKey(node.id(), node.nextId(), Relation.NEXT),
                        new EdgeSpec(node.id(), node.nextId(), false));
            }
            if (node.previousId() != null && state.nodes().containsKey(node.previousId())) {
                expected.put(
                        new EdgeKey(node.id(), node.previousId(), Relation.PREVIOUS),
                        new EdgeSpec(node.id(), node.previousId(), true));
            }
        }

        for (Map.Entry<EdgeKey, EdgeSpec> entry : expected.entrySet()) {
            if (edgeViews.containsKey(entry.getKey())) continue;
            EdgeSpec spec = entry.getValue();
            NodeView source = nodeViews.get(spec.sourceId());
            NodeView target = nodeViews.get(spec.targetId());
            if (source == null || target == null) continue;
            EdgeView edge = new EdgeView(source, target, true);
            edge.setCurved(spec.curved());
            edge.getStyleClass()
                    .add(
                            entry.getKey().relation() == Relation.NEXT
                                    ? "linked-next-edge"
                                    : "linked-previous-edge");
            edgeViews.put(entry.getKey(), edge);
            surface.edgeLayer().getChildren().add(edge);
            if (!initialFrame && !animations.isScrubbing())
                transitions.add(animations.reveal(edge, APPEAR_DURATION));
        }

        List<EdgeKey> removed =
                edgeViews.keySet().stream().filter(key -> !expected.containsKey(key)).toList();
        for (EdgeKey key : removed) {
            EdgeView edge = edgeViews.remove(key);
            if (initialFrame || animations.isScrubbing()) {
                surface.edgeLayer().getChildren().remove(edge);
            } else {
                Animation fade = animations.fadeOut(edge, DISAPPEAR_DURATION);
                fade.setOnFinished(event -> surface.edgeLayer().getChildren().remove(edge));
                transitions.add(fade);
            }
        }
    }

    private void applyRoutes(LayoutPatch patch) {
        Map<String, EdgeGeometry> routes = new LinkedHashMap<>();
        for (EdgeGeometry route : patch.edges()) routes.put(route.id(), route);
        for (Map.Entry<EdgeKey, EdgeView> entry : edgeViews.entrySet()) {
            EdgeGeometry route = routes.get(routeId(entry.getKey()));
            if (route == null || route.points().size() < 2) entry.getValue().clearRoute();
            else
                entry.getValue()
                        .setRoute(
                                route.points().stream()
                                        .map(point -> new Point2D(point.x(), point.y()))
                                        .toList());
        }
    }

    private void clearCurrentRoutes() {
        edgeViews.values().forEach(EdgeView::clearRoute);
    }

    private void positionRoleLabels(LinkedListViewState state, LayoutPatch patch) {
        List<Long> order = orderedNodeIds(state);
        if (order.isEmpty()) {
            headLabel.relocate(40.0d, 30.0d);
            tailLabel.relocate(120.0d, 30.0d);
            return;
        }
        ElementGeometry head = patch.elements().get(LinkedListElkLayout.nodeId(order.getFirst()));
        ElementGeometry tail = patch.elements().get(LinkedListElkLayout.nodeId(order.getLast()));
        if (head != null) headLabel.relocate(head.x() + 8.0d, Math.max(2.0d, head.y() - 28.0d));
        if (tail != null)
            tailLabel.relocate(tail.x() + tail.width() - 42.0d, tail.y() + tail.height() + 10.0d);
    }

    private List<Long> orderedNodeIds(LinkedListViewState state) {
        List<Long> order = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        List<LinkedListViewState.Node> roots =
                state.nodes().values().stream()
                        .filter(node -> node.previousId() == null)
                        .sorted(Comparator.comparingLong(LinkedListViewState.Node::id))
                        .toList();
        for (LinkedListViewState.Node root : roots) followNext(root.id(), state, visited, order);
        state.nodes().keySet().stream()
                .sorted()
                .forEach(id -> followNext(id, state, visited, order));
        return order;
    }

    private void followNext(
            long startId, LinkedListViewState state, Set<Long> visited, List<Long> order) {
        Long currentId = startId;
        while (currentId != null
                && state.nodes().containsKey(currentId)
                && visited.add(currentId)) {
            order.add(currentId);
            currentId = state.nodes().get(currentId).nextId();
        }
    }

    private void cleanupDetachedViews() {
        surface.nodeLayer()
                .getChildren()
                .removeIf(node -> node instanceof NodeView && !nodeViews.containsValue(node));
        surface.edgeLayer()
                .getChildren()
                .removeIf(node -> node instanceof EdgeView && !edgeViews.containsValue(node));
    }

    private CompletionStage<Void> playAsync(List<Animation> transitions) {
        if (transitions.isEmpty()) return CompletableFuture.completedFuture(null);
        CompletableFuture<Void> future = new CompletableFuture<>();
        ParallelTransition parallel = new ParallelTransition();
        parallel.getChildren().addAll(transitions);
        activeAnimation = parallel;
        activeAnimationFuture = future;
        parallel.setOnFinished(
                event -> {
                    if (activeAnimation == parallel) activeAnimation = null;
                    if (activeAnimationFuture == future) activeAnimationFuture = null;
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
        if (activeAnimationFuture != null) {
            activeAnimationFuture.complete(null);
            activeAnimationFuture = null;
        }
        cleanupDetachedViews();
    }

    public void setSelectionListener(LongConsumer listener) {
        selectionListener = listener == null ? ignored -> {} : listener;
    }

    public void clearSelection() {
        selectedNodeId = null;
        pendingSelectedNodeId = null;
        submitCurrentPresentation();
    }

    public void selectNode(long nodeId) {
        if (showSelection(nodeId)) selectionListener.accept(nodeId);
    }

    public boolean showSelection(long nodeId) {
        LinkedListViewState state = currentState();
        if (state == null || !state.nodes().containsKey(nodeId)) return false;
        selectedNodeId = nodeId;
        pendingSelectedNodeId = nodeViews.containsKey(nodeId) ? null : nodeId;
        submitCurrentPresentation();
        return true;
    }

    private void submitCurrentPresentation() {
        LinkedListViewState state = currentState();
        if (state != null && isModuleAttached() && !isDisposed()) {
            renderFramework.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
    }

    private void applyPendingSelection(LinkedListViewState state) {
        if (selectedNodeId != null && !state.nodes().containsKey(selectedNodeId))
            selectedNodeId = null;
        if (pendingSelectedNodeId == null) return;
        if (state.nodes().containsKey(pendingSelectedNodeId))
            selectedNodeId = pendingSelectedNodeId;
        pendingSelectedNodeId = null;
    }

    private void syncSelection() {
        for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
            entry.getValue().setSelected(java.util.Objects.equals(selectedNodeId, entry.getKey()));
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
    public void onVisualizationReset() {
        stopActiveAnimation();
        renderedState = LinkedListViewState.empty();
        nodeViews
                .values()
                .forEach(view -> view.layoutBoundsProperty().removeListener(elementSizeListener));
        nodeDecorations.values().forEach(LinkedNodeDecoration::dispose);
        nodeViews.clear();
        nodeDecorations.clear();
        edgeViews.clear();
        selectedNodeId = null;
        pendingSelectedNodeId = null;
        lastSubmittedState = null;
        pendingLayout = PendingLayout.empty();
        lastPatch = null;
        hasAppliedLayout = false;
        surface.reset();
        surface.decorationLayer().getChildren().setAll(headLabel, tailLabel);
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        stopActiveAnimation();
        nodeViews
                .values()
                .forEach(view -> view.layoutBoundsProperty().removeListener(elementSizeListener));
        nodeDecorations.values().forEach(LinkedNodeDecoration::dispose);
        renderFramework.unregisterSurface(SESSION_ID, this);
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private String label(LinkedListViewState.Node node) {
        return node.value().text();
    }

    private static String routeId(EdgeKey key) {
        return "linked:"
                + key.relation().name().toLowerCase()
                + ":"
                + key.sourceId()
                + ":"
                + key.targetId();
    }

    private static double positive(double actual, double fallback) {
        if (actual > 0.0d) return actual;
        return fallback > 0.0d ? fallback : 1.0d;
    }

    private static double quantize(double value) {
        return Math.rint(value * 100.0d) / 100.0d;
    }

    private static boolean close(Point2D a, Point2D b) {
        return a.distance(b) <= 0.01d;
    }

    private static Point2D center(ElementGeometry geometry) {
        return new Point2D(
                geometry.x() + geometry.width() / 2.0d, geometry.y() + geometry.height() / 2.0d);
    }

    private enum Relation {
        NEXT,
        PREVIOUS
    }

    private record EdgeKey(long sourceId, long targetId, Relation relation) {}

    private record EdgeSpec(long sourceId, long targetId, boolean curved) {}

    private record PendingLayout(List<Animation> transitions, Set<Long> newNodeIds) {
        private PendingLayout {
            transitions = List.copyOf(transitions);
            newNodeIds = Set.copyOf(newNodeIds);
        }

        private static PendingLayout empty() {
            return new PendingLayout(List.of(), Set.of());
        }
    }
}
