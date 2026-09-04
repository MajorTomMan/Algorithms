package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.layout.ElementBounds;
import com.majortom.algorithms.visualization.common.layout.LayoutResult;
import com.majortom.algorithms.visualization.impl.visualizer.array.ArrayCellView;
import com.majortom.algorithms.visualization.impl.visualizer.array.ArrayElkLayout;
import com.majortom.algorithms.visualization.impl.visualizer.array.ArrayElkLayout.ElementSize;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
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

/** Array renderer using measured JavaFX elements, transient ELK layout and GestureFX viewport. */
public final class ArrayVisualizer extends BaseVisualizer<ArrayViewState> {
    private static final double EMPTY_X = 36.0d;
    private static final double EMPTY_Y = 64.0d;
    private static final Duration APPEAR_DURATION = Duration.millis(160.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(120.0d);

    private final VisualizationSurface surface = new VisualizationSurface();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final ArrayElkLayout layout = new ArrayElkLayout();
    private final ExecutorService layoutExecutor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "array-elk-layout");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong layoutVersion = new AtomicLong();
    private final Map<Integer, ArrayCellView> cells = new LinkedHashMap<>();
    private final Text emptyLabel = new Text("EMPTY ARRAY");
    private final InvalidationListener elementSizeListener = observable -> requestRender();

    private List<ElementSize> lastLayoutInput = List.of();
    private Animation activeAnimation;
    private boolean firstRender = true;

    public ArrayVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        emptyLabel.getStyleClass().add("visual-empty-label");
    }

    @Override
    protected void draw(ArrayViewState state) {
        stopActiveAnimation();
        List<Animation> transitions = new ArrayList<>();
        int size = state.values().size();

        if (firstRender) {
            surface.markViewportPristine();
        }
        if (size == 0) {
            invalidateLayout();
            clearCells(transitions);
            if (!surface.decorationLayer().getChildren().contains(emptyLabel)) {
                emptyLabel.relocate(EMPTY_X, EMPTY_Y);
                surface.decorationLayer().getChildren().add(emptyLabel);
            }
            play(transitions);
            surface.fitIfPristine();
            firstRender = false;
            return;
        }
        surface.decorationLayer().getChildren().remove(emptyLabel);

        for (int index = 0; index < size; index++) {
            ArrayCellView cell = cells.get(index);
            boolean added = false;
            if (cell == null) {
                cell = new ArrayCellView(index, state.values().get(index));
                cell.layoutBoundsProperty().addListener(elementSizeListener);
                cells.put(index, cell);
                surface.nodeLayer().getChildren().add(cell);
                added = true;
            }
            cell.setIndex(index);
            cell.setValue(state.values().get(index));
            cell.setHighlighted(isMutationIndex(state.mutation(), index));
            cell.setCompleted(state.completed());
            if (added && !firstRender) {
                transitions.add(animations.together(
                        animations.fadeIn(cell, APPEAR_DURATION),
                        animations.scaleIn(cell, APPEAR_DURATION)));
            }
        }

        List<Integer> removed = cells.keySet().stream().filter(index -> index >= size).toList();
        for (Integer index : removed) {
            ArrayCellView cell = cells.remove(index);
            cell.layoutBoundsProperty().removeListener(elementSizeListener);
            if (firstRender) {
                surface.nodeLayer().getChildren().remove(cell);
            } else {
                Animation fade = animations.fadeOut(cell, DISAPPEAR_DURATION);
                fade.setOnFinished(event -> surface.nodeLayer().getChildren().remove(cell));
                transitions.add(fade);
            }
        }

        List<ElementSize> measured = measureElements(size);
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
            ArrayCellView cell = cells.get(index);
            cell.applyCss();
            cell.autosize();
            Bounds bounds = cell.getLayoutBounds();
            double width = positive(bounds.getWidth(), cell.prefWidth(-1.0d));
            double height = positive(bounds.getHeight(), cell.prefHeight(-1.0d));
            measured.add(new ElementSize(id(index), quantize(width), quantize(height)));
        }
        return List.copyOf(measured);
    }

    private void scheduleLayout(List<ElementSize> input) {
        long version = layoutVersion.incrementAndGet();
        layoutExecutor.execute(() -> {
            try {
                LayoutResult result = layout.layout(input);
                Platform.runLater(() -> applyLayout(version, result));
            } catch (Throwable failure) {
                Platform.runLater(() -> handleLayoutFailure(version, failure));
            }
        });
    }

    private void applyLayout(long version, LayoutResult result) {
        if (isDisposed() || version != layoutVersion.get()) {
            return;
        }
        for (Map.Entry<Integer, ArrayCellView> entry : cells.entrySet()) {
            ElementBounds bounds = result.elements().get(id(entry.getKey()));
            if (bounds != null) {
                entry.getValue().relocate(bounds.x(), bounds.y());
            }
        }
        surface.fitIfPristine();
    }

    private void handleLayoutFailure(long version, Throwable failure) {
        if (isDisposed() || version != layoutVersion.get()) {
            return;
        }
        lastLayoutInput = List.of();
        throw new IllegalStateException("Array ELK layout failed", failure);
    }

    private boolean isMutationIndex(ArrayViewState.Mutation mutation, int index) {
        return index == mutation.index() || index == mutation.otherIndex();
    }

    private void clearCells(List<Animation> transitions) {
        if (firstRender) {
            cells.values().forEach(cell -> cell.layoutBoundsProperty().removeListener(elementSizeListener));
            cells.clear();
            surface.nodeLayer().getChildren().clear();
            return;
        }
        for (ArrayCellView cell : cells.values()) {
            cell.layoutBoundsProperty().removeListener(elementSizeListener);
            Animation fade = animations.fadeOut(cell, DISAPPEAR_DURATION);
            fade.setOnFinished(event -> surface.nodeLayer().getChildren().remove(cell));
            transitions.add(fade);
        }
        cells.clear();
    }

    private void invalidateLayout() {
        layoutVersion.incrementAndGet();
        lastLayoutInput = List.of();
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
        surface.nodeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
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
        return "array:" + index;
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
}
