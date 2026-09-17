package com.majortom.algorithms.visualization.impl.visualizer.linked;

import com.majortom.algorithms.visualization.impl.visualizer.semantic.LinkedListStructureVisualization;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.RectangleGeometry;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.RenderPort;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.fx.RenderCommitContext;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;

import javafx.geometry.Point2D;
import javafx.scene.text.Text;

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

/** Linked-list renderer with pure capture and stable node identity keyed by factual node id. */
public final class LinkedListVisualizer extends BaseVisualizer<LinkedListViewState> {
    private static final RenderSessionId SESSION_ID = RenderSessionId.of("LINKED_LIST");
    private static final StructureVisualization<LinkedListViewState> STRUCTURE_VISUALIZATION = new LinkedListStructureVisualization();
    private static final double MIN_NODE_WIDTH = 112.0d;
    private static final double MIN_NODE_HEIGHT = 72.0d;
    private static final double LABEL_HORIZONTAL_PADDING = 40.0d;

    private final VisualizationSurface surface = new VisualizationSurface();
    private final RenderPort renderPort;
    private final Map<Long, NodeView> nodeViews = new LinkedHashMap<>();
    private final Map<Long, LinkedNodeDecoration> nodeDecorations = new LinkedHashMap<>();
    private final Map<EdgeKey, EdgeView> edgeViews = new LinkedHashMap<>();
    private final Text headLabel = new Text();
    private final Text tailLabel = new Text();

    private volatile LinkedListViewState lastSubmittedState;
    private LayoutPatch lastPatch;
    private Long selectedNodeId;
    private Long pendingSelectedNodeId;
    private LongConsumer selectionListener = ignored -> {};

    public LinkedListVisualizer(RenderPort renderPort) {
        this.renderPort = java.util.Objects.requireNonNull(renderPort, "renderPort");
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
    }

    @Override
    public RenderSessionId sessionId() { return SESSION_ID; }

    @Override
    protected synchronized void submitFrameworkRender(LinkedListViewState state) {
        LinkedListViewState previous = lastSubmittedState;
        boolean initial = previous == null;
        boolean structural = initial || !previous.nodes().equals(state.nodes());
        lastSubmittedState = state;
        if (structural) {
            renderPort.submit(
                    new StructuralRenderIntent<>(
                            SESSION_ID,
                            state,
                            initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE,
                            initial));
        } else {
            renderPort.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
    }


    @Override
    public CompletionStage<Void> commitLayout(
            LinkedListViewState state, LayoutPatch patch, RenderCommitContext context) {
        reconcileNodes(state);
        applyPendingSelection(state);
        applyPresentation(state);

        // Existing edges temporarily follow their endpoints while factual node positions are committed.
        clearCurrentRoutes();
        for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
            ElementGeometry bounds = patch.elements().get(LinkedListLayout.nodeId(entry.getKey()));
            if (bounds == null) continue;
            NodeView view = entry.getValue();
            view.setGeometry(new RectangleGeometry(bounds.width(), bounds.height()));
            Point2D target = center(bounds);
            view.setCenter(target.getX(), target.getY());
            view.setOpacity(1.0d);
            view.setScaleX(1.0d);
            view.setScaleY(1.0d);
        }

        reconcileEdges(state);
        applyRoutes(patch);
        positionRoleLabels(state, patch);
        lastPatch = patch;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> commitPresentation(
            LinkedListViewState state, RenderCommitContext context) {
        reconcileNodes(state);
        applyPendingSelection(state);
        applyPresentation(state);
        reconcileEdges(state);
        if (lastPatch != null) {
            applyRoutes(lastPatch);
            positionRoleLabels(state, lastPatch);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void reconcileNodes(LinkedListViewState state) {
        List<Long> removed = nodeViews.keySet().stream()
                .filter(id -> !state.nodes().containsKey(id))
                .toList();
        for (Long id : removed) removeNode(id);

        for (LinkedListViewState.Node node : state.nodes().values()) {
            if (nodeViews.containsKey(node.id())) continue;
            NodeView view = createNode(node);
            nodeViews.put(node.id(), view);
            surface.nodeLayer().getChildren().add(view);
            LinkedNodeDecoration decoration = new LinkedNodeDecoration(view);
            nodeDecorations.put(node.id(), decoration);
            surface.decorationLayer().getChildren().add(decoration);
        }
    }

    private NodeView createNode(LinkedListViewState.Node node) {
        NodeView view = new NodeView(
                new RectangleGeometry(MIN_NODE_WIDTH, MIN_NODE_HEIGHT), label(node));
        view.getStyleClass().add("linked-node");
        long nodeId = node.id();
        view.setOnMouseClicked(
                event -> {
                    selectNode(nodeId);
                    event.consume();
                });
        return view;
    }

    private void removeNode(long nodeId) {
        if (java.util.Objects.equals(selectedNodeId, nodeId)) {
            selectedNodeId = null;
            selectionListener.accept(-1L);
        }
        NodeView view = nodeViews.remove(nodeId);
        if (view != null) surface.nodeLayer().getChildren().remove(view);
        LinkedNodeDecoration decoration = nodeDecorations.remove(nodeId);
        if (decoration != null) {
            decoration.dispose();
            surface.decorationLayer().getChildren().remove(decoration);
        }
        List<EdgeKey> attached = edgeViews.keySet().stream()
                .filter(key -> key.sourceId() == nodeId || key.targetId() == nodeId)
                .toList();
        for (EdgeKey key : attached) {
            EdgeView edge = edgeViews.remove(key);
            if (edge != null) {
                edge.dispose();
                surface.edgeLayer().getChildren().remove(edge);
            }
        }
    }

    private void applyPresentation(LinkedListViewState state) {
        for (LinkedListViewState.Node node : state.nodes().values()) {
            NodeView view = nodeViews.get(node.id());
            if (view == null) continue;
            view.setText(label(node));
            view.setHighlighted(false);
            view.setSelected(java.util.Objects.equals(selectedNodeId, node.id()));
            LinkedNodeDecoration decoration = nodeDecorations.get(node.id());
            if (decoration != null) decoration.setLinks(node.previousId(), node.nextId());
        }
    }

    private void reconcileEdges(LinkedListViewState state) {
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

        List<EdgeKey> removed = edgeViews.keySet().stream()
                .filter(key -> !expected.containsKey(key))
                .toList();
        for (EdgeKey key : removed) {
            EdgeView edge = edgeViews.remove(key);
            if (edge != null) {
                edge.dispose();
                surface.edgeLayer().getChildren().remove(edge);
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
            edge.getStyleClass().add(
                    entry.getKey().relation() == Relation.NEXT
                            ? "linked-next-edge"
                            : "linked-previous-edge");
            edgeViews.put(entry.getKey(), edge);
            surface.edgeLayer().getChildren().add(edge);
        }
    }

    private void applyRoutes(LayoutPatch patch) {
        Map<String, EdgeGeometry> routes = new LinkedHashMap<>();
        for (EdgeGeometry route : patch.edges()) routes.put(route.id(), route);
        for (Map.Entry<EdgeKey, EdgeView> entry : edgeViews.entrySet()) {
            EdgeGeometry route = routes.get(routeId(entry.getKey()));
            if (route == null || route.points().size() < 2) {
                entry.getValue().clearRoute();
            } else {
                entry.getValue().setRoute(
                        route.points().stream()
                                .map(point -> new Point2D(point.x(), point.y()))
                                .toList());
            }
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
        ElementGeometry head = patch.elements().get(LinkedListLayout.nodeId(order.getFirst()));
        ElementGeometry tail = patch.elements().get(LinkedListLayout.nodeId(order.getLast()));
        if (head != null) headLabel.relocate(head.x() + 8.0d, Math.max(2.0d, head.y() - 28.0d));
        if (tail != null) {
            tailLabel.relocate(
                    tail.x() + tail.width() - 42.0d, tail.y() + tail.height() + 10.0d);
        }
    }

    private List<Long> orderedNodeIds(LinkedListViewState state) {
        List<Long> order = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        List<LinkedListViewState.Node> roots = state.nodes().values().stream()
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
            renderPort.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
    }

    private void applyPendingSelection(LinkedListViewState state) {
        if (selectedNodeId != null && !state.nodes().containsKey(selectedNodeId)) {
            selectedNodeId = null;
        }
        if (pendingSelectedNodeId == null) return;
        if (state.nodes().containsKey(pendingSelectedNodeId)) {
            selectedNodeId = pendingSelectedNodeId;
        }
        pendingSelectedNodeId = null;
    }

    @Override
    public StructureVisualization<LinkedListViewState> structureVisualization() {
        return STRUCTURE_VISUALIZATION;
    }

    @Override
    public FxSurfaceAdapter fxSurfaceAdapter() {
        return surface;
    }
    @Override public void setViewportObstructionInsets(javafx.geometry.Insets insets) { surface.setObstructionInsets(insets); }
@Override
    public void onVisualizationReset() {
        nodeDecorations.values().forEach(LinkedNodeDecoration::dispose);
        nodeViews.clear();
        nodeDecorations.clear();
        edgeViews.values().forEach(EdgeView::dispose);
        edgeViews.clear();
        surface.nodeLayer().getChildren().clear();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().setAll(headLabel, tailLabel);
        selectedNodeId = null;
        pendingSelectedNodeId = null;
        lastSubmittedState = null;
        lastPatch = null;
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        nodeDecorations.values().forEach(LinkedNodeDecoration::dispose);
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private static String label(LinkedListViewState.Node node) {
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

    private static Point2D center(ElementGeometry geometry) {
        return new Point2D(
                geometry.x() + geometry.width() / 2.0d,
                geometry.y() + geometry.height() / 2.0d);
    }

    private enum Relation {
        NEXT,
        PREVIOUS
    }

    private record EdgeKey(long sourceId, long targetId, Relation relation) {}

    private record EdgeSpec(long sourceId, long targetId, boolean curved) {}
}
