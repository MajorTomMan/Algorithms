package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.ViewportPane;
import com.majortom.algorithms.visualization.common.geometry.CircleGeometry;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.visualizer.tree.TreeLayout;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure JavaFX general/binary tree renderer using project-owned deterministic tidy layout. */
public final class TreeVisualizer extends BaseVisualizer<TreeViewState> {
    private static final CircleGeometry NODE_GEOMETRY = new CircleGeometry(27.0d);
    private static final Duration MOVE_DURATION = Duration.millis(300.0d);
    private static final Duration APPEAR_DURATION = Duration.millis(180.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(140.0d);

    private final ViewportPane viewport = new ViewportPane();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final TreeLayout layout = new TreeLayout();
    private final Map<Long, NodeView> nodeViews = new LinkedHashMap<>();
    private final Map<EdgeKey, EdgeView> edgeViews = new LinkedHashMap<>();
    private TreeViewState renderedState = TreeViewState.empty(TreeViewState.Kind.GENERAL);
    private Animation activeAnimation;
    private boolean firstRender = true;

    public TreeVisualizer() {
        getChildren().setAll(viewport);
        viewport.prefWidthProperty().bind(widthProperty());
        viewport.prefHeightProperty().bind(heightProperty());
    }

    @Override
    protected void draw(TreeViewState state) {
        stopActiveAnimation();
        Map<Long, Point2D> positions = layout.layout(state).positions();
        List<Animation> transitions = new ArrayList<>();

        for (TreeViewState.Node node : state.nodes().values()) {
            Point2D target = positions.get(node.id());
            if (target == null) {
                continue;
            }
            NodeView view = nodeViews.get(node.id());
            if (view == null) {
                view = new NodeView(NODE_GEOMETRY, Integer.toString(node.value()));
                view.setCenter(target.getX(), target.getY());
                nodeViews.put(node.id(), view);
                viewport.content().getChildren().add(view);
                if (!firstRender) {
                    transitions.add(animations.together(
                            animations.fadeIn(view, APPEAR_DURATION),
                            animations.scaleIn(view, APPEAR_DURATION)));
                }
            } else {
                TreeViewState.Node previous = renderedState.nodes().get(node.id());
                view.setText(Integer.toString(node.value()));
                if (previous != null && previous.value() != node.value()) {
                    view.setHighlighted(true);
                } else {
                    view.setHighlighted(false);
                }
                if (firstRender) {
                    view.setCenter(target.getX(), target.getY());
                } else if (!view.center().equals(target)) {
                    transitions.add(animations.move(view, target, MOVE_DURATION));
                }
            }
        }

        syncEdges(state, transitions);

        List<Long> removedNodes = nodeViews.keySet().stream()
                .filter(id -> !state.nodes().containsKey(id))
                .toList();
        for (Long nodeId : removedNodes) {
            NodeView view = nodeViews.get(nodeId);
            if (firstRender) {
                removeNode(nodeId);
                continue;
            }
            Animation fade = animations.fadeOut(view, DISAPPEAR_DURATION);
            fade.setOnFinished(event -> removeNode(nodeId));
            transitions.add(fade);
        }

        reorderLayers();
        renderedState = state;
        if (firstRender) {
            firstRender = false;
            viewport.fitToViewport();
            return;
        }
        if (!transitions.isEmpty()) {
            ParallelTransition parallel = new ParallelTransition();
            parallel.getChildren().addAll(transitions);
            activeAnimation = parallel;
            parallel.play();
        }
    }

    private void syncEdges(TreeViewState state, List<Animation> transitions) {
        Map<EdgeKey, EdgeSpec> expected = new LinkedHashMap<>();
        for (TreeViewState.Node node : state.nodes().values()) {
            if (state.kind() == TreeViewState.Kind.GENERAL) {
                for (int index = 0; index < node.childIds().size(); index++) {
                    addExpectedEdge(expected, state, node.id(), node.childIds().get(index), Relation.child(index));
                }
            } else {
                addExpectedEdge(expected, state, node.id(), node.leftId(), Relation.left());
                addExpectedEdge(expected, state, node.id(), node.rightId(), Relation.right());
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
            viewport.content().getChildren().add(edge);
            if (!firstRender) {
                transitions.add(animations.reveal(edge, APPEAR_DURATION));
            }
        }

        List<EdgeKey> removed = edgeViews.keySet().stream()
                .filter(key -> !expected.containsKey(key))
                .toList();
        for (EdgeKey key : removed) {
            EdgeView edge = edgeViews.remove(key);
            if (edge == null) {
                continue;
            }
            if (firstRender) {
                viewport.content().getChildren().remove(edge);
                continue;
            }
            Animation fade = animations.fadeOut(edge, DISAPPEAR_DURATION);
            fade.setOnFinished(event -> viewport.content().getChildren().remove(edge));
            transitions.add(fade);
        }
    }

    private void addExpectedEdge(Map<EdgeKey, EdgeSpec> expected, TreeViewState state, long sourceId, Long targetId, Relation relation) {
        if (targetId == null || !state.nodes().containsKey(targetId)) {
            return;
        }
        expected.put(new EdgeKey(sourceId, targetId, relation), new EdgeSpec(sourceId, targetId));
    }

    private void reorderLayers() {
        List<Node> ordered = new ArrayList<>(edgeViews.values());
        ordered.addAll(nodeViews.values());
        viewport.content().getChildren().setAll(ordered);
    }

    private void removeNode(long nodeId) {
        NodeView view = nodeViews.remove(nodeId);
        if (view != null) {
            viewport.content().getChildren().remove(view);
        }
    }

    private void stopActiveAnimation() {
        if (activeAnimation != null) {
            activeAnimation.stop();
            activeAnimation = null;
        }
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
        nodeViews.clear();
        edgeViews.clear();
        viewport.content().getChildren().clear();
        firstRender = true;
        viewport.reset();
    }

    @Override
    public void dispose() {
        stopActiveAnimation();
        viewport.prefWidthProperty().unbind();
        viewport.prefHeightProperty().unbind();
        super.dispose();
    }

    private record Relation(String name, int index, String styleClass) {
        private static Relation left() {
            return new Relation("left", 0, "tree-left-edge");
        }

        private static Relation right() {
            return new Relation("right", 1, "tree-right-edge");
        }

        private static Relation child(int index) {
            return new Relation("child", index, "tree-child-edge");
        }
    }

    private record EdgeKey(long sourceId, long targetId, Relation relation) {}

    private record EdgeSpec(long sourceId, long targetId) {}
}
