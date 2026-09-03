package com.majortom.algorithms.visualization.impl.visualizer.linked;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.ViewportPane;
import com.majortom.algorithms.visualization.common.geometry.RectangleGeometry;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure JavaFX linked-list renderer driven only by factual node/link ViewState. */
public final class LinkedListVisualizer extends BaseVisualizer<LinkedListViewState> {
    private static final RectangleGeometry NODE_GEOMETRY = new RectangleGeometry(84.0d, 46.0d);
    private static final double START_X = 90.0d;
    private static final double START_Y = 100.0d;
    private static final double HORIZONTAL_GAP = 150.0d;
    private static final double VERTICAL_GAP = 125.0d;
    private static final Duration MOVE_DURATION = Duration.millis(260.0d);
    private static final Duration APPEAR_DURATION = Duration.millis(180.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(140.0d);

    private final ViewportPane viewport = new ViewportPane();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final Map<Long, NodeView> nodeViews = new LinkedHashMap<>();
    private final Map<EdgeKey, EdgeView> edgeViews = new LinkedHashMap<>();
    private LinkedListViewState renderedState = LinkedListViewState.empty();
    private Animation activeAnimation;
    private boolean firstRender = true;

    public LinkedListVisualizer() {
        getChildren().setAll(viewport);
        viewport.prefWidthProperty().bind(widthProperty());
        viewport.prefHeightProperty().bind(heightProperty());
    }

    @Override
    protected void draw(LinkedListViewState state) {
        stopActiveAnimation();
        Map<Long, Point2D> positions = layout(state);
        List<Animation> transitions = new ArrayList<>();
        Set<Long> newNodeIds = new HashSet<>();

        for (LinkedListViewState.Node node : state.nodes().values()) {
            NodeView view = nodeViews.get(node.id());
            if (view == null) {
                view = new NodeView(NODE_GEOMETRY, label(node));
                Point2D target = positions.get(node.id());
                view.setCenter(target.getX(), target.getY());
                nodeViews.put(node.id(), view);
                viewport.content().getChildren().add(view);
                newNodeIds.add(node.id());
                if (!firstRender) {
                    transitions.add(animations.together(
                            animations.fadeIn(view, APPEAR_DURATION),
                            animations.scaleIn(view, APPEAR_DURATION)));
                }
            } else {
                LinkedListViewState.Node previous = renderedState.nodes().get(node.id());
                view.setText(label(node));
                if (previous != null && !java.util.Objects.equals(previous.value(), node.value())) {
                    view.setHighlighted(true);
                    PauseTransition clearHighlight = new PauseTransition(Duration.millis(360.0d));
                    NodeView highlightedView = view;
                    clearHighlight.setOnFinished(event -> highlightedView.setHighlighted(false));
                    transitions.add(clearHighlight);
                }
                Point2D target = positions.get(node.id());
                if (firstRender) {
                    view.setCenter(target.getX(), target.getY());
                } else if (!view.center().equals(target)) {
                    transitions.add(animations.move(view, target, MOVE_DURATION));
                }
            }
        }

        syncEdges(state, transitions);

        List<Long> removedIds = nodeViews.keySet().stream()
                .filter(id -> !state.nodes().containsKey(id))
                .toList();
        for (Long nodeId : removedIds) {
            NodeView view = nodeViews.get(nodeId);
            if (firstRender) {
                removeNodeView(nodeId);
                continue;
            }
            Animation fade = animations.fadeOut(view, DISAPPEAR_DURATION);
            fade.setOnFinished(event -> removeNodeView(nodeId));
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

    private Map<Long, Point2D> layout(LinkedListViewState state) {
        List<Long> order = orderedNodeIds(state);
        int columns = Math.max(1, (int) Math.floor(Math.max(520.0d, getWidth()) / HORIZONTAL_GAP));
        Map<Long, Point2D> positions = new HashMap<>();
        for (int index = 0; index < order.size(); index++) {
            int row = index / columns;
            int column = index % columns;
            double x = START_X + column * HORIZONTAL_GAP;
            double y = START_Y + row * VERTICAL_GAP;
            positions.put(order.get(index), new Point2D(x, y));
        }
        return positions;
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

    private void reorderLayers() {
        List<Node> ordered = new ArrayList<>(edgeViews.values());
        ordered.addAll(nodeViews.values());
        viewport.content().getChildren().setAll(ordered);
    }

    private void removeNodeView(long nodeId) {
        NodeView removed = nodeViews.remove(nodeId);
        if (removed != null) {
            viewport.content().getChildren().remove(removed);
        }
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
        renderedState = LinkedListViewState.empty();
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

    private enum Relation {
        NEXT,
        PREVIOUS
    }

    private record EdgeKey(long sourceId, long targetId, Relation relation) {}

    private record EdgeSpec(long sourceId, long targetId, boolean curved) {}
}
