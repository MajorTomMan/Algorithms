package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.ViewportPane;
import com.majortom.algorithms.visualization.common.geometry.CircleGeometry;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphLayout;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure JavaFX graph renderer using deterministic project-owned layout. */
public final class GraphVisualizer extends BaseVisualizer<GraphViewState> {
    private static final CircleGeometry NODE_GEOMETRY = new CircleGeometry(27.0d);
    private static final Duration MOVE_DURATION = Duration.millis(300.0d);
    private static final Duration APPEAR_DURATION = Duration.millis(180.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(140.0d);

    private final ViewportPane viewport = new ViewportPane();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final GraphLayout layout = new GraphLayout();
    private final Map<Long, NodeView> nodeViews = new LinkedHashMap<>();
    private final Map<Long, EdgeView> edgeViews = new LinkedHashMap<>();
    private GraphViewState renderedState = new GraphViewState(false, List.of(), List.of(), false);
    private Animation activeAnimation;
    private boolean firstRender = true;

    public GraphVisualizer() {
        getChildren().setAll(viewport);
        viewport.prefWidthProperty().bind(widthProperty());
        viewport.prefHeightProperty().bind(heightProperty());
    }

    @Override
    protected void draw(GraphViewState state) {
        stopActiveAnimation();
        Map<Long, Point2D> positions = layout.layout(state).positions();
        List<Animation> transitions = new ArrayList<>();

        for (GraphViewState.Node node : state.nodes()) {
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
                view.setText(Integer.toString(node.value()));
                if (firstRender) {
                    view.setCenter(target.getX(), target.getY());
                } else if (!view.center().equals(target)) {
                    transitions.add(animations.move(view, target, MOVE_DURATION));
                }
            }
            view.setHighlighted(state.completed());
        }

        syncEdges(state, transitions);

        List<Long> removedNodes = nodeViews.keySet().stream()
                .filter(id -> state.nodes().stream().noneMatch(node -> node.id() == id))
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

    private void syncEdges(GraphViewState state, List<Animation> transitions) {
        Map<Long, GraphViewState.Edge> expected = new LinkedHashMap<>();
        for (GraphViewState.Edge edge : state.edges()) {
            expected.put(edge.id(), edge);
        }

        for (GraphViewState.Edge edge : state.edges()) {
            EdgeView existing = edgeViews.get(edge.id());
            if (existing != null) {
                existing.setDirected(state.directed());
                continue;
            }
            NodeView source = nodeViews.get(edge.fromId());
            NodeView target = nodeViews.get(edge.toId());
            if (source == null || target == null) {
                continue;
            }
            EdgeView view = new EdgeView(source, target, state.directed());
            view.setCurved(source == target);
            edgeViews.put(edge.id(), view);
            viewport.content().getChildren().add(view);
            if (!firstRender) {
                transitions.add(animations.reveal(view, APPEAR_DURATION));
            }
        }

        List<Long> removed = edgeViews.keySet().stream()
                .filter(id -> !expected.containsKey(id))
                .toList();
        for (Long edgeId : removed) {
            EdgeView view = edgeViews.remove(edgeId);
            if (view == null) {
                continue;
            }
            if (firstRender) {
                viewport.content().getChildren().remove(view);
                continue;
            }
            Animation fade = animations.fadeOut(view, DISAPPEAR_DURATION);
            fade.setOnFinished(event -> viewport.content().getChildren().remove(view));
            transitions.add(fade);
        }
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
        renderedState = new GraphViewState(false, List.of(), List.of(), false);
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
}
