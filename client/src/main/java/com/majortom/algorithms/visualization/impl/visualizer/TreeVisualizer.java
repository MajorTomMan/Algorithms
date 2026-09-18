package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.animation.api.AnimationControl;
import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.fx.AnimationSceneAdapter;
import com.majortom.algorithms.visualization.animation.runtime.StructureAnimationRuntime;
import com.majortom.algorithms.visualization.impl.visualizer.semantic.TreeStructureVisualization;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.CircleGeometry;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.visualizer.tree.TreeElkLayout;
import com.majortom.algorithms.visualization.impl.visualizer.tree.animation.TreeAnimationIds;
import com.majortom.algorithms.visualization.impl.visualizer.tree.animation.TreeAnimationPlanner;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import javafx.geometry.Point2D;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/** General/binary/AVL tree renderer using measured JavaFX nodes, transient ELK layout and GestureFX viewport. */
public final class TreeVisualizer extends BaseVisualizer<TreeViewState> {
    private static final double MIN_RADIUS = 24.0d;
    private static final double LABEL_PADDING = 18.0d;

    private static final RenderSessionId SESSION_ID = RenderSessionId.of("TREE");
    private static final StructureVisualization<TreeViewState> STRUCTURE_VISUALIZATION = new TreeStructureVisualization();
    private final VisualizationSurface surface = new VisualizationSurface();
    private final Map<Long, NodeView> nodeViews = new LinkedHashMap<>();
    private final Map<EdgeKey, EdgeView> edgeViews = new LinkedHashMap<>();
    private final StructureAnimationRuntime<TreeViewState> animationRuntime =
            new StructureAnimationRuntime<>(new TreeAnimationPlanner());
    private final TreeAnimationSceneAdapter animationScene = new TreeAnimationSceneAdapter();
    private LayoutPatch lastPatch;
    private Long selectedNodeId;
    private Long pendingSelectedNodeId;
    private LongConsumer selectionListener = ignored -> { };

    public TreeVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        surface.setFrameworkManagedCamera(true);
    }
    @Override
    public RenderSessionId sessionId() { return SESSION_ID; }




    @Override
    public CompletionStage<Void> commitLayout(
            TreeViewState state, LayoutPatch patch, RenderCommitContext context) {
        boolean animate = context.modelChange() && !context.initialFrame();
        AnimationPlan plan = animationRuntime.beginTransition(state, patch, animate);
        animationScene.prepare(plan, state, patch);

        reconcileNodes(state);
        reconcileEdges(state);
        applyPendingSelection(state);
        applyPresentation(state);

        for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
            ElementGeometry bounds = patch.elements().get(TreeElkLayout.nodeId(entry.getKey()));
            if (bounds == null) continue;
            NodeView view = entry.getValue();
            view.setGeometry(new CircleGeometry(Math.max(MIN_RADIUS, bounds.width() / 2.0d)));
            view.setCenter(bounds.x() + bounds.width() / 2.0d, bounds.y() + bounds.height() / 2.0d);
        }
        applyRoutes(patch);
        lastPatch = patch;
        animationRuntime.play(plan, animationScene, context.presentationProgress()::publish);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> commitPresentation(
            TreeViewState state, RenderCommitContext context) {
        reconcileNodes(state);
        reconcileEdges(state);
        applyPendingSelection(state);
        applyPresentation(state);
        return CompletableFuture.completedFuture(null);
    }

    private void reconcileNodes(TreeViewState state) {
        List<Long> removed = nodeViews.keySet().stream()
                .filter(id -> !state.nodes().containsKey(id))
                .toList();
        for (Long nodeId : removed) {
            if (java.util.Objects.equals(selectedNodeId, nodeId)) selectedNodeId = null;
            NodeView view = nodeViews.remove(nodeId);
            if (view != null) surface.nodeLayer().getChildren().remove(view);
        }
        for (TreeViewState.Node node : state.nodes().values()) {
            if (nodeViews.containsKey(node.id())) continue;
            NodeView view = new NodeView(new CircleGeometry(MIN_RADIUS), node.value().text());
            long nodeId = node.id();
            view.setOnMouseClicked(event -> {
                selectNode(nodeId);
                event.consume();
            });
            nodeViews.put(node.id(), view);
            surface.nodeLayer().getChildren().add(view);
        }
    }

    private void reconcileEdges(TreeViewState state) {
        Map<EdgeKey, EdgeSpec> expected = new LinkedHashMap<>();
        for (TreeViewState.Node node : state.nodes().values()) {
            if (state.kind() == TreeViewState.Kind.GENERAL) {
                for (int index = 0; index < node.childIds().size(); index++) {
                    addExpectedEdge(
                            expected, state, node.id(), node.childIds().get(index), Relation.CHILD, index);
                }
            } else {
                addExpectedEdge(expected, state, node.id(), node.leftId(), Relation.LEFT, 0);
                addExpectedEdge(expected, state, node.id(), node.rightId(), Relation.RIGHT, 1);
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
            EdgeView edge = new EdgeView(source, target, false);
            edge.setCurved(source == target);
            edge.getStyleClass().add(entry.getKey().relation().styleClass());
            edgeViews.put(entry.getKey(), edge);
            surface.edgeLayer().getChildren().add(edge);
        }
    }

    private void applyPresentation(TreeViewState state) {
        for (TreeViewState.Node node : state.nodes().values()) {
            NodeView view = nodeViews.get(node.id());
            if (view == null) continue;
            view.setText(node.value().text());
            view.setCurrent(state.currentNodeIds().contains(node.id()));
            view.setHighlighted(state.observedNodeIds().contains(node.id()));
            view.setVisited(state.visitedNodeIds().contains(node.id()));
        }
        syncSelectionState();
    }

    private void applyPendingSelection(TreeViewState state) {
        if (pendingSelectedNodeId == null) return;
        if (state.nodes().containsKey(pendingSelectedNodeId)) selectedNodeId = pendingSelectedNodeId;
        pendingSelectedNodeId = null;
    }

    private void applyRoutes(LayoutPatch patch) {
        Map<String, EdgeGeometry> routes = new LinkedHashMap<>();
        for (EdgeGeometry route : patch.edges()) routes.put(route.id(), route);
        for (Map.Entry<EdgeKey, EdgeView> entry : edgeViews.entrySet()) {
            EdgeGeometry route = routes.get(routeId(entry.getKey()));
            if (route == null || route.points().size() < 2) {
                entry.getValue().clearRoute();
            } else {
                entry.getValue().setRoute(route.points().stream().map(point -> new Point2D(point.x(), point.y())).toList());
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


    public void setSelectionListener(LongConsumer listener) {
        if (listener == null) {
            selectionListener = ignored -> { };
        } else {
            selectionListener = listener;
        }
    }

    public void clearSelection() {
        selectedNodeId = null;
        pendingSelectedNodeId = null;
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
        selectedNodeId = nodeId;
        pendingSelectedNodeId = nodeViews.containsKey(nodeId) ? null : nodeId;
        return true;
    }

    private void syncSelectionState() {
        for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
            entry.getValue().setSelected(selectedNodeId != null && selectedNodeId.equals(entry.getKey()));
        }
    }

    @Override
    protected AnimationControl animationControl() {
        return animationRuntime;
    }

    @Override
    public StructureVisualization<TreeViewState> structureVisualization() {
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
        nodeViews.clear();
        edgeViews.values().forEach(EdgeView::dispose);
        edgeViews.clear();
        surface.nodeLayer().getChildren().clear();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
        selectedNodeId = null;
        pendingSelectedNodeId = null;
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



    private static String routeId(EdgeKey key) {
        return switch (key.relation()) {
            case CHILD -> TreeAnimationIds.childEdge(key.index(), key.sourceId(), key.targetId());
            case LEFT -> TreeAnimationIds.leftEdge(key.sourceId(), key.targetId());
            case RIGHT -> TreeAnimationIds.rightEdge(key.sourceId(), key.targetId());
        };
    }



    private final class TreeAnimationSceneAdapter implements AnimationSceneAdapter {
        private final Map<String, Point2D> capturedCenters = new LinkedHashMap<>();
        private final Map<String, List<Point2D>> capturedRoutes = new LinkedHashMap<>();
        private final Map<Long, NodeView> exitingNodes = new LinkedHashMap<>();
        private final Map<String, EdgeView> exitingEdges = new LinkedHashMap<>();
        private LayoutPatch targetPatch;

        void prepare(AnimationPlan plan, TreeViewState state, LayoutPatch patch) {
            capturedCenters.clear();
            capturedRoutes.clear();
            for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
                capturedCenters.put(TreeAnimationIds.node(entry.getKey()), entry.getValue().visualCenter());
            }
            for (Map.Entry<EdgeKey, EdgeView> entry : edgeViews.entrySet()) {
                capturedRoutes.put(routeId(entry.getKey()), entry.getValue().routeSnapshot());
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
                    .filter(candidate -> TreeAnimationIds.node(candidate).equals(logicalId))
                    .findFirst().orElse(null);
            if (id == null) return;
            NodeView view = nodeViews.remove(id);
            if (view != null) exitingNodes.put(id, view);
            if (java.util.Objects.equals(selectedNodeId, id)) selectedNodeId = null;
        }

        private void detachEdge(String logicalId) {
            EdgeKey key = edgeViews.keySet().stream()
                    .filter(candidate -> routeId(candidate).equals(logicalId))
                    .findFirst().orElse(null);
            if (key == null) return;
            EdgeView edge = edgeViews.remove(key);
            if (edge != null) exitingEdges.put(logicalId, edge);
        }

        @Override
        public Optional<NodeTarget> node(String logicalId) {
            for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
                if (TreeAnimationIds.node(entry.getKey()).equals(logicalId)) {
                    return Optional.of(new NodeTarget(logicalId, entry.getValue(), List.of()));
                }
            }
            for (Map.Entry<Long, NodeView> entry : exitingNodes.entrySet()) {
                if (TreeAnimationIds.node(entry.getKey()).equals(logicalId)) {
                    return Optional.of(new NodeTarget(logicalId, entry.getValue(), List.of()));
                }
            }
            return Optional.empty();
        }

        @Override
        public Optional<EdgeTarget> edge(String logicalId) {
            EdgeView active = edgeViews.entrySet().stream()
                    .filter(entry -> routeId(entry.getKey()).equals(logicalId))
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
            nodeViews.forEach((id, view) -> result.add(
                    new NodeTarget(TreeAnimationIds.node(id), view, List.of())));
            return result;
        }

        @Override
        public Collection<EdgeTarget> activeEdges() {
            return edgeViews.entrySet().stream()
                    .map(entry -> new EdgeTarget(routeId(entry.getKey()), entry.getValue()))
                    .toList();
        }

        @Override
        public void discardExitedVisuals() {
            exitingEdges.values().forEach(edge -> {
                surface.edgeLayer().getChildren().remove(edge);
                edge.dispose();
            });
            exitingEdges.clear();
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
            }
            for (EdgeTarget target : activeEdges()) {
                target.edge().setRevealProgress(1.0d);
                target.edge().setOpacity(1.0d);
            }
            discardExitedVisuals();
            if (targetPatch != null) applyRoutes(targetPatch);
            capturedCenters.clear();
            capturedRoutes.clear();
        }
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


    private record EdgeKey(long sourceId, long targetId, Relation relation, int index) {}
    private record EdgeSpec(long sourceId, long targetId) {}
}
