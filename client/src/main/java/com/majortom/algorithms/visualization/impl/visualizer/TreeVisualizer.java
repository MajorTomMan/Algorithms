package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.CircleGeometry;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.visualizer.tree.TreeElkLayout;
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
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;

import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/**
 * General/binary/AVL tree renderer using measured JavaFX nodes, transient ELK layout and GestureFX
 * viewport.
 */
public final class TreeVisualizer extends BaseVisualizer<TreeViewState>
        implements FxSurfaceAdapter<TreeViewState> {
    private static final double MIN_RADIUS = 24.0d;
    private static final double LABEL_PADDING = 18.0d;
    private static final Duration MOVE_DURATION = Duration.millis(300.0d);
    private static final Duration APPEAR_DURATION = Duration.millis(180.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(140.0d);
    private static final double NEW_NODE_ENTRY_DISTANCE = MIN_RADIUS * 2.0d + 8.0d;

    private static final RenderSessionId SESSION_ID = RenderSessionId.of("TREE");
    private final VisualizationSurface surface = new VisualizationSurface();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final DefaultRenderFramework renderFramework = RenderRuntime.shared();
    private final Map<Long, NodeView> nodeViews = new LinkedHashMap<>();
    private final Map<Long, Point2D> settledTargets = new LinkedHashMap<>();
    private final Map<EdgeKey, EdgeView> edgeViews = new LinkedHashMap<>();
    private boolean measuringElements;
    private final InvalidationListener elementSizeListener =
            observable -> {
                if (!measuringElements) {
                    requestGeometryRefresh();
                }
            };

    private TreeViewState renderedState = TreeViewState.empty(TreeViewState.Kind.GENERAL);
    private volatile TreeViewState lastSubmittedState;
    private Animation activeAnimation;
    private PendingLayout pendingLayout = PendingLayout.empty();
    private boolean firstRender = true;
    private boolean hasAppliedLayout;
    private Long selectedNodeId;
    private Long pendingSelectedNodeId;
    private LongConsumer selectionListener = ignored -> {};

    public TreeVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        surface.setFrameworkManagedCamera(true);
        renderFramework.registerSurface(SESSION_ID, this);
    }

    @Override
    protected synchronized void submitFrameworkRender(TreeViewState state) {
        TreeViewState previous = lastSubmittedState;
        boolean initial = previous == null;
        boolean structural = initial || requiresStructuralLayout(previous, state);
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
    public LayoutRequest captureLayout(TreeViewState state, RenderCaptureContext context) {
        stopActiveAnimation();
        cleanupDetachedViews();
        List<Animation> transitions = new ArrayList<>();
        Set<Long> newNodeIds = new HashSet<>();

        for (TreeViewState.Node node : state.nodes().values()) {
            NodeView view = nodeViews.get(node.id());
            if (view == null) {
                view = new NodeView(new CircleGeometry(MIN_RADIUS), node.value().text());
                view.layoutBoundsProperty().addListener(elementSizeListener);
                long visualNodeId = node.id();
                view.setOnMouseClicked(
                        event -> {
                            selectedNodeId = visualNodeId;
                            syncSelectionState();
                            selectionListener.accept(visualNodeId);
                            event.consume();
                        });
                nodeViews.put(node.id(), view);
                surface.nodeLayer().getChildren().add(view);
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
            view.setCurrent(state.currentNodeIds().contains(node.id()));
            view.setHighlighted(state.observedNodeIds().contains(node.id()));
            view.setVisited(state.visitedNodeIds().contains(node.id()));
        }

        boolean pendingSelectionApplied = false;
        if (pendingSelectedNodeId != null) {
            if (state.nodes().containsKey(pendingSelectedNodeId)) {
                selectedNodeId = pendingSelectedNodeId;
                pendingSelectionApplied = true;
            }
            pendingSelectedNodeId = null;
        }
        syncSelectionState();
        if (pendingSelectionApplied) {
            selectionListener.accept(selectedNodeId);
        }
        syncEdges(state, transitions);

        List<Long> removedIds =
                nodeViews.keySet().stream().filter(id -> !state.nodes().containsKey(id)).toList();
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

        pendingLayout =
                new PendingLayout(
                        List.copyOf(transitions), Set.copyOf(newNodeIds), context.initialFrame());
        return buildLayoutInput(state, context);
    }

    private LayoutRequest buildLayoutInput(TreeViewState state, RenderCaptureContext context) {
        List<Long> order = orderedNodeIds(state);
        List<LayoutElement> nodes = new ArrayList<>(order.size());
        measuringElements = true;
        try {
            for (Long id : order) {
                NodeView view = nodeViews.get(id);
                if (view == null) {
                    continue;
                }
                resizeToMeasuredLabel(view);
                CircleGeometry geometry = (CircleGeometry) view.getGeometry();
                nodes.add(
                        new LayoutElement(
                                TreeElkLayout.nodeId(id),
                                quantize(geometry.width()),
                                quantize(geometry.height())));
            }
        } finally {
            measuringElements = false;
        }

        List<LayoutLink> links = new ArrayList<>();
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
                    links.add(
                            new LayoutLink(
                                    routeId(key),
                                    TreeElkLayout.nodeId(node.id()),
                                    TreeElkLayout.nodeId(targetId),
                                    "CHILD",
                                    index));
                }
            } else {
                addLayoutLink(links, state, node.id(), node.leftId(), Relation.LEFT, 0);
                addLayoutLink(links, state, node.id(), node.rightId(), Relation.RIGHT, 1);
            }
        }

        return new LayoutRequest(
                context.requestId(),
                SESSION_ID,
                context.modelRevision(),
                context.geometryRevision(),
                TreeElkLayout.ID,
                nodes,
                links,
                Map.of("kind", state.kind().name()));
    }

    private void addLayoutLink(
            List<LayoutLink> links,
            TreeViewState state,
            long sourceId,
            Long targetId,
            Relation relation,
            int index) {
        if (targetId == null || !state.nodes().containsKey(targetId)) return;
        EdgeKey key = new EdgeKey(sourceId, targetId, relation, index);
        links.add(
                new LayoutLink(
                        routeId(key),
                        TreeElkLayout.nodeId(sourceId),
                        TreeElkLayout.nodeId(targetId),
                        relation.name(),
                        index));
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
            TreeViewState state, LayoutPatch patch, RenderCommitContext context) {
        PendingLayout pending = pendingLayout;
        List<Animation> transitions = new ArrayList<>(pending.transitions());
        Set<Long> newNodeIds = pending.newNodeIds();
        boolean snap = context.initialFrame() || animations.isScrubbing() || !hasAppliedLayout;
        settledTargets.clear();
        for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
            ElementGeometry bounds = patch.elements().get(TreeElkLayout.nodeId(entry.getKey()));
            if (bounds == null) continue;
            Point2D target =
                    new Point2D(
                            bounds.x() + bounds.width() / 2.0d,
                            bounds.y() + bounds.height() / 2.0d);
            settledTargets.put(entry.getKey(), target);
            NodeView view = entry.getValue();
            if (snap) {
                view.setCenter(target.getX(), target.getY());
            } else if (newNodeIds.contains(entry.getKey())) {
                Point2D origin = newNodeOrigin(entry.getKey(), target);
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
            transitions.forEach(Animation::stop);
            finish.run();
            return CompletableFuture.completedFuture(null);
        }
        return playAsync(transitions).thenRun(finish);
    }

    @Override
    public CompletionStage<Void> commitPresentation(
            TreeViewState state, RenderCommitContext context) {
        stopActiveAnimation();
        for (TreeViewState.Node node : state.nodes().values()) {
            NodeView view = nodeViews.get(node.id());
            if (view == null) continue;
            view.setText(node.value().text());
            view.setCurrent(state.currentNodeIds().contains(node.id()));
            view.setHighlighted(state.observedNodeIds().contains(node.id()));
            view.setVisited(state.visitedNodeIds().contains(node.id()));
        }
        if (pendingSelectedNodeId != null) {
            if (state.nodes().containsKey(pendingSelectedNodeId))
                selectedNodeId = pendingSelectedNodeId;
            pendingSelectedNodeId = null;
        }
        syncSelectionState();
        renderedState = state;
        return CompletableFuture.completedFuture(null);
    }

    private void applyRoutes(LayoutPatch patch) {
        Map<String, EdgeGeometry> routes = new LinkedHashMap<>();
        for (EdgeGeometry route : patch.edges()) routes.put(route.id(), route);
        for (Map.Entry<EdgeKey, EdgeView> entry : edgeViews.entrySet()) {
            EdgeGeometry route = routes.get(routeId(entry.getKey()));
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
    }

    private void syncEdges(TreeViewState state, List<Animation> transitions) {
        Map<EdgeKey, EdgeSpec> expected = new LinkedHashMap<>();
        for (TreeViewState.Node node : state.nodes().values()) {
            if (state.kind() == TreeViewState.Kind.GENERAL) {
                for (int index = 0; index < node.childIds().size(); index++) {
                    addExpectedEdge(
                            expected,
                            state,
                            node.id(),
                            node.childIds().get(index),
                            Relation.CHILD,
                            index);
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

        List<EdgeKey> removed =
                edgeViews.keySet().stream().filter(key -> !expected.containsKey(key)).toList();
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

    private void addExpectedEdge(
            Map<EdgeKey, EdgeSpec> expected,
            TreeViewState state,
            long sourceId,
            Long targetId,
            Relation relation,
            int index) {
        if (targetId == null || !state.nodes().containsKey(targetId)) {
            return;
        }
        EdgeKey key = new EdgeKey(sourceId, targetId, relation, index);
        expected.put(key, new EdgeSpec(sourceId, targetId));
    }

    private Point2D newNodeOrigin(long nodeId, Point2D fallback) {
        Long parentId = parentId(renderedState, nodeId);
        if (parentId == null) {
            return fallback;
        }
        NodeView parent = nodeViews.get(parentId);
        if (parent != null) {
            return entryOrigin(parent.center(), fallback);
        }
        Point2D settled = settledTargets.get(parentId);
        if (settled != null) {
            return entryOrigin(settled, fallback);
        }
        return fallback;
    }

    private Point2D entryOrigin(Point2D parent, Point2D target) {
        Point2D delta = target.subtract(parent);
        double distance = delta.magnitude();
        if (distance <= 0.01d) {
            return parent.add(NEW_NODE_ENTRY_DISTANCE, 0.0d);
        }
        if (distance <= NEW_NODE_ENTRY_DISTANCE) {
            return target;
        }
        return parent.add(delta.normalize().multiply(NEW_NODE_ENTRY_DISTANCE));
    }

    private Long parentId(TreeViewState state, long nodeId) {
        for (TreeViewState.Node node : state.nodes().values()) {
            if (state.kind() == TreeViewState.Kind.GENERAL) {
                if (node.childIds().contains(nodeId)) {
                    return node.id();
                }
            } else {
                if (node.leftId() != null && node.leftId() == nodeId) {
                    return node.id();
                }
                if (node.rightId() != null && node.rightId() == nodeId) {
                    return node.id();
                }
            }
        }
        return null;
    }

    private List<Long> orderedNodeIds(TreeViewState state) {
        List<Long> order = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        if (state.rootId() != null) {
            visit(state.rootId(), state, visited, order);
        }
        state.nodes().keySet().stream()
                .sorted(Comparator.naturalOrder())
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
        surface.nodeLayer()
                .getChildren()
                .removeIf(node -> node instanceof NodeView && !nodeViews.containsValue(node));
        surface.edgeLayer()
                .getChildren()
                .removeIf(node -> node instanceof EdgeView && !edgeViews.containsValue(node));
    }

    public void setSelectionListener(LongConsumer listener) {
        if (listener == null) {
            selectionListener = ignored -> {};
        } else {
            selectionListener = listener;
        }
    }

    public void clearSelection() {
        selectedNodeId = null;
        pendingSelectedNodeId = null;
        syncSelectionState();
    }

    public Long selectedNodeId() {
        return selectedNodeId;
    }

    public void selectNode(long nodeId) {
        if (!showSelection(nodeId)) {
            return;
        }
        selectionListener.accept(nodeId);
    }

    public boolean showSelection(long nodeId) {
        TreeViewState state = currentState();
        if (state == null || !state.nodes().containsKey(nodeId)) {
            return false;
        }
        selectedNodeId = nodeId;
        if (!nodeViews.containsKey(nodeId)) {
            pendingSelectedNodeId = nodeId;
            requestRender();
            return true;
        }
        pendingSelectedNodeId = null;
        syncSelectionState();
        return true;
    }

    private void syncSelectionState() {
        for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
            entry.getValue()
                    .setSelected(selectedNodeId != null && selectedNodeId.equals(entry.getKey()));
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
        // A newer factual topology may arrive before a prior transition completes.
        // Keep the currently displayed geometry as the next transition origin; only
        // normalize transient presentation properties.
        nodeViews
                .values()
                .forEach(
                        view -> {
                            view.setOpacity(1.0d);
                            view.setScaleX(1.0d);
                            view.setScaleY(1.0d);
                        });
        edgeViews.values().forEach(edge -> edge.setOpacity(1.0d));
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
        renderedState = TreeViewState.empty(TreeViewState.Kind.GENERAL);
        nodeViews
                .values()
                .forEach(view -> view.layoutBoundsProperty().removeListener(elementSizeListener));
        nodeViews.clear();
        settledTargets.clear();
        edgeViews.clear();
        surface.nodeLayer().getChildren().clear();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
        pendingLayout = PendingLayout.empty();
        lastSubmittedState = null;
        hasAppliedLayout = false;
        selectedNodeId = null;
        pendingSelectedNodeId = null;
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
        renderFramework.unregisterSurface(SESSION_ID, this);
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private static boolean requiresStructuralLayout(TreeViewState previous, TreeViewState current) {
        return previous == null
                || previous.kind() != current.kind()
                || !java.util.Objects.equals(previous.rootId(), current.rootId())
                || !previous.nodes().equals(current.nodes());
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

    private static String routeId(EdgeKey key) {
        return "tree:"
                + key.relation().name().toLowerCase()
                + ":"
                + key.index()
                + ":"
                + key.sourceId()
                + ":"
                + key.targetId();
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
