package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.RectangleGeometry;
import com.majortom.algorithms.visualization.common.layout.ElementBounds;
import com.majortom.algorithms.visualization.common.layout.LayoutResult;
import com.majortom.algorithms.visualization.common.layout.LayoutFailureReporter;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import com.majortom.algorithms.visualization.impl.visualizer.linear.StackQueueElkLayout;
import com.majortom.algorithms.visualization.impl.visualizer.linear.StackQueueElkLayout.ElementSize;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.Bounds;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/** Stack/Queue renderer using family ordering, measured JavaFX nodes, ELK geometry and GestureFX viewport. */
public final class StackQueueVisualizer extends BaseVisualizer<LinearStructureViewState> {
    private static final RectangleGeometry CELL_GEOMETRY = new RectangleGeometry(82.0d, 46.0d);
    private static final double EMPTY_X = 48.0d;
    private static final double EMPTY_Y = 48.0d;
    private static final Duration APPEAR_DURATION = Duration.millis(150.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(120.0d);

    private final VisualizationSurface surface = new VisualizationSurface();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final StackQueueElkLayout layout = new StackQueueElkLayout();
    private final ExecutorService layoutExecutor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "stack-queue-elk-layout");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong layoutVersion = new AtomicLong();
    private final Map<Integer, NodeView> cells = new LinkedHashMap<>();
    private final Map<Integer, EdgeView> queueEdges = new LinkedHashMap<>();
    private final Text roleLabel = new Text();
    private final InvalidationListener elementSizeListener = observable -> requestRender();

    private LayoutInput lastLayoutInput = LayoutInput.empty();
    private Animation activeAnimation;
    private boolean firstRender = true;

    public StackQueueVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        roleLabel.getStyleClass().add("linear-role-label");
        surface.decorationLayer().getChildren().add(roleLabel);
    }

    @Override
    protected void draw(LinearStructureViewState state) {
        stopActiveAnimation();
        boolean stack = "stack".equals(state.kind());
        List<Animation> transitions = new ArrayList<>();
        roleLabel.setText(stack ? "TOP" : "FRONT → REAR");

        if (firstRender) {
            surface.markViewportPristine();
        }

        for (int index = 0; index < state.values().size(); index++) {
            NodeView cell = cells.get(index);
            boolean added = false;
            if (cell == null) {
                cell = new NodeView(CELL_GEOMETRY, Integer.toString(state.values().get(index)));
                cell.layoutBoundsProperty().addListener(elementSizeListener);
                cells.put(index, cell);
                surface.nodeLayer().getChildren().add(cell);
                added = true;
            } else {
                cell.setText(Integer.toString(state.values().get(index)));
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
            cell.layoutBoundsProperty().removeListener(elementSizeListener);
            if (firstRender) {
                surface.nodeLayer().getChildren().remove(cell);
            } else {
                Animation fade = animations.fadeOut(cell, DISAPPEAR_DURATION);
                fade.setOnFinished(event -> surface.nodeLayer().getChildren().remove(cell));
                transitions.add(fade);
            }
        }

        syncQueueEdges(stack, state.values().size(), transitions);

        if (state.values().isEmpty()) {
            invalidateLayout();
            lastLayoutInput = new LayoutInput(stack, List.of());
            roleLabel.relocate(EMPTY_X, EMPTY_Y);
            play(transitions);
            surface.fitIfPristine();
            firstRender = false;
            return;
        }

        LayoutInput measured = new LayoutInput(stack, measureElements(state.values().size()));
        if (!measured.equals(lastLayoutInput)) {
            lastLayoutInput = measured;
            scheduleLayout(measured);
        }
        play(transitions);
        firstRender = false;
    }

    private List<ElementSize> measureElements(int size) {
        List<ElementSize> measured = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            NodeView cell = cells.get(index);
            cell.applyCss();
            cell.autosize();
            Bounds bounds = cell.getLayoutBounds();
            double width = positive(bounds.getWidth(), cell.prefWidth(-1.0d));
            double height = positive(bounds.getHeight(), cell.prefHeight(-1.0d));
            measured.add(new ElementSize(id(index), quantize(width), quantize(height)));
        }
        return List.copyOf(measured);
    }

    private void scheduleLayout(LayoutInput input) {
        long version = layoutVersion.incrementAndGet();
        layoutExecutor.execute(() -> {
            try {
                LayoutResult result = input.stack() ? layout.layoutStack(input.elements()) : layout.layoutQueue(input.elements());
                Platform.runLater(() -> applyLayout(version, input.stack(), result));
            } catch (Throwable failure) {
                Platform.runLater(() -> handleLayoutFailure(version, failure));
            }
        });
    }

    private void applyLayout(long version, boolean stack, LayoutResult result) {
        if (isDisposed() || version != layoutVersion.get()) {
            return;
        }
        for (Map.Entry<Integer, NodeView> entry : cells.entrySet()) {
            ElementBounds bounds = result.elements().get(id(entry.getKey()));
            if (bounds != null) {
                entry.getValue().setCenter(bounds.x() + bounds.width() / 2.0d, bounds.y() + bounds.height() / 2.0d);
            }
        }
        ElementBounds first = result.elements().get(id(0));
        if (first != null) {
            roleLabel.relocate(stack ? first.x() - 6.0d : first.x(), Math.max(0.0d, first.y() - 30.0d));
        }
        surface.fitIfPristine();
    }

    private void handleLayoutFailure(long version, Throwable failure) {
        if (isDisposed() || version != layoutVersion.get()) {
            return;
        }
        LayoutFailureReporter.report("Stack/Queue", failure);
    }

    private void syncQueueEdges(boolean stack, int size, List<Animation> transitions) {
        int expected = stack ? 0 : Math.max(0, size - 1);
        List<Integer> removed = queueEdges.keySet().stream().filter(index -> index >= expected || stack).toList();
        for (Integer index : removed) {
            EdgeView edge = queueEdges.remove(index);
            if (firstRender) {
                surface.edgeLayer().getChildren().remove(edge);
            } else {
                Animation fade = animations.fadeOut(edge, DISAPPEAR_DURATION);
                fade.setOnFinished(event -> surface.edgeLayer().getChildren().remove(edge));
                transitions.add(fade);
            }
        }
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
            surface.edgeLayer().getChildren().add(edge);
            if (!firstRender) {
                transitions.add(animations.reveal(edge, APPEAR_DURATION));
            }
        }
    }

    private void invalidateLayout() {
        layoutVersion.incrementAndGet();
        lastLayoutInput = LayoutInput.empty();
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
        invalidateLayout();
        cells.values().forEach(cell -> cell.layoutBoundsProperty().removeListener(elementSizeListener));
        cells.clear();
        queueEdges.clear();
        surface.nodeLayer().getChildren().clear();
        surface.edgeLayer().getChildren().clear();
        if (!surface.decorationLayer().getChildren().contains(roleLabel)) {
            surface.decorationLayer().getChildren().add(roleLabel);
        }
        firstRender = true;
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        stopActiveAnimation();
        invalidateLayout();
        cells.values().forEach(cell -> cell.layoutBoundsProperty().removeListener(elementSizeListener));
        layoutExecutor.shutdownNow();
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private static String id(int index) {
        return "linear:" + index;
    }

    private static double positive(double actual, double fallback) {
        if (actual > 0.0d) {
            return actual;
        }
        return fallback > 0.0d ? fallback : 1.0d;
    }

    private static double quantize(double value) {
        return Math.rint(value * 100.0d) / 100.0d;
    }

    private record LayoutInput(boolean stack, List<ElementSize> elements) {
        private LayoutInput {
            elements = List.copyOf(elements);
        }

        private static LayoutInput empty() {
            return new LayoutInput(false, List.of());
        }
    }
}
