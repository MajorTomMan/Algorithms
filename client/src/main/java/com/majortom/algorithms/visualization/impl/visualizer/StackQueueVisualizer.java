package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.ViewportPane;
import com.majortom.algorithms.visualization.common.geometry.RectangleGeometry;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure JavaFX renderer with distinct Stack vertical and Queue horizontal layouts. */
public final class StackQueueVisualizer extends BaseVisualizer<LinearStructureViewState> {
    private static final RectangleGeometry CELL_GEOMETRY = new RectangleGeometry(82.0d, 46.0d);
    private static final double START_X = 90.0d;
    private static final double START_Y = 80.0d;
    private static final double HORIZONTAL_GAP = 130.0d;
    private static final double VERTICAL_GAP = 72.0d;
    private static final Duration MOVE_DURATION = Duration.millis(220.0d);
    private static final Duration APPEAR_DURATION = Duration.millis(150.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(120.0d);

    private final ViewportPane viewport = new ViewportPane();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final Map<Integer, NodeView> cells = new LinkedHashMap<>();
    private final Map<Integer, EdgeView> queueEdges = new LinkedHashMap<>();
    private final Text roleLabel = new Text();
    private Animation activeAnimation;
    private boolean firstRender = true;

    public StackQueueVisualizer() {
        getChildren().setAll(viewport);
        viewport.prefWidthProperty().bind(widthProperty());
        viewport.prefHeightProperty().bind(heightProperty());
        roleLabel.getStyleClass().add("linear-role-label");
    }

    @Override
    protected void draw(LinearStructureViewState state) {
        stopActiveAnimation();
        boolean stack = "stack".equals(state.kind());
        List<Animation> transitions = new ArrayList<>();
        roleLabel.setText(stack ? "TOP" : "FRONT → REAR");
        if (!viewport.content().getChildren().contains(roleLabel)) {
            viewport.content().getChildren().add(roleLabel);
        }
        roleLabel.relocate(START_X - 42.0d, START_Y - 54.0d);

        for (int index = 0; index < state.values().size(); index++) {
            NodeView cell = cells.get(index);
            Point2D target = position(stack, index);
            boolean added = false;
            if (cell == null) {
                cell = new NodeView(CELL_GEOMETRY, Integer.toString(state.values().get(index)));
                cells.put(index, cell);
                viewport.content().getChildren().add(cell);
                cell.setCenter(target.getX(), target.getY());
                added = true;
            } else {
                cell.setText(Integer.toString(state.values().get(index)));
                if (firstRender) {
                    cell.setCenter(target.getX(), target.getY());
                } else if (!cell.center().equals(target)) {
                    transitions.add(animations.move(cell, target, MOVE_DURATION));
                }
            }
            cell.setHighlighted(index == 0);
            if (added && !firstRender) {
                transitions.add(animations.together(
                        animations.fadeIn(cell, APPEAR_DURATION),
                        animations.scaleIn(cell, APPEAR_DURATION)));
            }
        }

        List<Integer> removed = cells.keySet().stream().filter(index -> index >= state.values().size()).toList();
        for (Integer index : removed) {
            NodeView cell = cells.remove(index);
            if (firstRender) {
                viewport.content().getChildren().remove(cell);
            } else {
                Animation fade = animations.fadeOut(cell, DISAPPEAR_DURATION);
                fade.setOnFinished(event -> viewport.content().getChildren().remove(cell));
                transitions.add(fade);
            }
        }

        syncQueueEdges(stack, state.values().size(), transitions);
        reorderLayers();
        if (firstRender) {
            firstRender = false;
            viewport.fitToViewport();
            return;
        }
        play(transitions);
    }

    private Point2D position(boolean stack, int index) {
        if (stack) {
            return new Point2D(START_X, START_Y + index * VERTICAL_GAP);
        }
        return new Point2D(START_X + index * HORIZONTAL_GAP, START_Y);
    }

    private void syncQueueEdges(boolean stack, int size, List<Animation> transitions) {
        int expected = stack ? 0 : Math.max(0, size - 1);
        for (int index = 0; index < expected; index++) {
            if (queueEdges.containsKey(index)) {
                continue;
            }
            NodeView source = cells.get(index);
            NodeView target = cells.get(index + 1);
            if (source == null || target == null) {
                continue;
            }
            EdgeView edge = new EdgeView(source, target, true);
            edge.getStyleClass().add("queue-order-edge");
            queueEdges.put(index, edge);
            viewport.content().getChildren().add(edge);
            if (!firstRender) {
                transitions.add(animations.reveal(edge, APPEAR_DURATION));
            }
        }
        List<Integer> removed = queueEdges.keySet().stream().filter(index -> index >= expected).toList();
        for (Integer index : removed) {
            EdgeView edge = queueEdges.remove(index);
            if (firstRender) {
                viewport.content().getChildren().remove(edge);
            } else {
                Animation fade = animations.fadeOut(edge, DISAPPEAR_DURATION);
                fade.setOnFinished(event -> viewport.content().getChildren().remove(edge));
                transitions.add(fade);
            }
        }
        if (stack && !queueEdges.isEmpty()) {
            for (EdgeView edge : queueEdges.values()) {
                viewport.content().getChildren().remove(edge);
            }
            queueEdges.clear();
        }
    }

    private void reorderLayers() {
        List<Node> ordered = new ArrayList<>(queueEdges.values());
        ordered.addAll(cells.values());
        ordered.add(roleLabel);
        viewport.content().getChildren().setAll(ordered);
    }

    private void play(List<Animation> transitions) {
        if (transitions.isEmpty()) {
            return;
        }
        ParallelTransition parallel = new ParallelTransition();
        parallel.getChildren().addAll(transitions);
        activeAnimation = parallel;
        parallel.play();
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
        cells.clear();
        queueEdges.clear();
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
