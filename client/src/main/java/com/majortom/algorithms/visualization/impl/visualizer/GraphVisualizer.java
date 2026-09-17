package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.geometry.CircleGeometry;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphElkLayout;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.RenderPort;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.fx.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.fx.RenderCommitContext;
import com.majortom.algorithms.visualization.render.layout.DetachedMetrics;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final VisualizationSurface surface = new VisualizationSurface();
    private final RenderPort renderPort;
    private final Map<Long, NodeView> nodeViews = new LinkedHashMap<>();
    private final Map<Long, EdgeView> edgeViews = new LinkedHashMap<>();
    private final Map<Long, Label> nodeIdLabels = new LinkedHashMap<>();

    private volatile GraphViewState lastSubmittedState;
    private Long selectedNodeId;
    private Long selectedEdgeId;
    private Long pendingSelectedNodeId;
    private Long pendingSelectedEdgeId;
    private LongConsumer nodeSelectionListener = ignored -> { };
    private LongConsumer edgeSelectionListener = ignored -> { };
    private VisualDensity density = VisualDensity.DETAIL;

    public GraphVisualizer(RenderPort renderPort) {
        this.renderPort = java.util.Objects.requireNonNull(renderPort, "renderPort");
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
    protected synchronized void submitFrameworkRender(GraphViewState state) {
        GraphViewState previous = lastSubmittedState;
        boolean initial = previous == null;
        boolean structural = initial || requiresStructuralLayout(previous, state);
        lastSubmittedState = state;
        if (structural) {
            CameraPolicy cameraPolicy = initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE;
            renderPort.submit(new StructuralRenderIntent<>(SESSION_ID, state, cameraPolicy, initial));
        } else {
            renderPort.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
    }

    @Override
    public LayoutRequest captureLayout(GraphViewState state, RenderCaptureContext context) {
        List<GraphViewState.Node> orderedNodes = state.nodes().stream()
                .sorted(Comparator.comparingLong(GraphViewState.Node::id))
                .toList();
        List<LayoutElement> nodes = new ArrayList<>(orderedNodes.size());
        for (GraphViewState.Node node : orderedNodes) {
            double diameter = Math.max(
                    MIN_RADIUS * 2.0d,
                    DetachedMetrics.boxWidth(
                            node.value().text(), context.contentStyle(), MIN_RADIUS * 2.0d, LABEL_PADDING));
            nodes.add(new LayoutElement(
                    GraphElkLayout.nodeId(node.id()), quantize(diameter), quantize(diameter)));
        }

        Set<String> available = nodes.stream()
                .map(LayoutElement::id)
                .collect(java.util.stream.Collectors.toSet());
        List<LayoutLink> links = state.edges().stream()
                .filter(edge -> available.contains(GraphElkLayout.nodeId(edge.fromId()))
                        && available.contains(GraphElkLayout.nodeId(edge.toId())))
                .sorted(Comparator.comparingLong(GraphViewState.Edge::id))
                .map(edge -> new LayoutLink(
                        GraphElkLayout.edgeId(edge.id()),
                        GraphElkLayout.nodeId(edge.fromId()),
                        GraphElkLayout.nodeId(edge.toId())))
                .toList();
        return new LayoutRequest(
                context.requestId(),
                context.sessionId(),
                context.modelRevision(),
                context.geometryRevision(),
                GraphElkLayout.ID,
                nodes,
                links,
                Map.of("directed", Boolean.toString(state.directed())));
    }

    @Override
    public CompletionStage<Void> commitLayout(
            GraphViewState state, LayoutPatch patch, RenderCommitContext context) {
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
            view.setOpacity(1.0d);
            view.setScaleX(1.0d);
            view.setScaleY(1.0d);
        }
        applyRoutes(patch);
        resolveEdgeLabelCollisions();
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
            if (edgeViews.containsKey(edge.id())) continue;
            NodeView source = nodeViews.get(edge.fromId());
            NodeView target = nodeViews.get(edge.toId());
            if (source == null || target == null) continue;
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
        submitCurrentPresentation();
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
        boolean exists = state != null && state.nodes().stream().anyMatch(node -> node.id() == nodeId);
        if (!exists) {
            return false;
        }
        selectedNodeId = nodeId;
        selectedEdgeId = null;
        pendingSelectedNodeId = nodeViews.containsKey(nodeId) ? null : nodeId;
        pendingSelectedEdgeId = null;
        submitCurrentPresentation();
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
        boolean exists = state != null && state.edges().stream().anyMatch(edge -> edge.id() == edgeId);
        if (!exists) {
            return false;
        }
        selectedEdgeId = edgeId;
        selectedNodeId = null;
        pendingSelectedEdgeId = edgeViews.containsKey(edgeId) ? null : edgeId;
        pendingSelectedNodeId = null;
        submitCurrentPresentation();
        return true;
    }

    private void submitCurrentPresentation() {
        GraphViewState state = currentState();
        if (state != null && isModuleAttached() && !isDisposed()) {
            renderPort.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
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
        edgeViews.values().forEach(EdgeView::dispose);
        nodeViews.clear();
        edgeViews.clear();
        nodeIdLabels.clear();
        surface.nodeLayer().getChildren().clear();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
        lastSubmittedState = null;
        selectedNodeId = null;
        selectedEdgeId = null;
        pendingSelectedNodeId = null;
        pendingSelectedEdgeId = null;
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        edgeViews.values().forEach(EdgeView::dispose);
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private static boolean requiresStructuralLayout(GraphViewState previous, GraphViewState current) {
        if (previous == null || previous.directed() != current.directed() || !previous.nodes().equals(current.nodes())) return true;
        if (previous.edges().size() != current.edges().size()) return true;
        for (int index = 0; index < previous.edges().size(); index++) {
            GraphViewState.Edge left = previous.edges().get(index);
            GraphViewState.Edge right = current.edges().get(index);
            if (left.id() != right.id() || left.fromId() != right.fromId() || left.toId() != right.toId()) return true;
        }
        return false;
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


    private static double quantize(double value) {
        return Math.rint(value * 100.0d) / 100.0d;
    }

}
