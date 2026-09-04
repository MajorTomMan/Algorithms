package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.layout.ElementBounds;
import com.majortom.algorithms.visualization.common.layout.LayoutResult;
import com.majortom.algorithms.visualization.common.layout.LayoutFailureReporter;
import com.majortom.algorithms.visualization.impl.visualizer.string.StringCellView;
import com.majortom.algorithms.visualization.impl.visualizer.string.StringElkLayout;
import com.majortom.algorithms.visualization.impl.visualizer.string.StringElkLayout.ElementSize;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;
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

/** String renderer using measured JavaFX cells, transient ELK layout and GestureFX viewport. */
public final class StringVisualizer extends BaseVisualizer<StringViewState> {
    private static final double EMPTY_X = 36.0d;
    private static final double EMPTY_Y = 64.0d;
    private static final Duration APPEAR_DURATION = Duration.millis(150.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(120.0d);

    private final VisualizationSurface surface = new VisualizationSurface();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final StringElkLayout layout = new StringElkLayout();
    private final ExecutorService layoutExecutor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "string-elk-layout");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong layoutVersion = new AtomicLong();
    private final Map<Integer, StringCellView> cells = new LinkedHashMap<>();
    private final Text emptyLabel = new Text("EMPTY STRING");
    private final Text observationLabel = new Text();
    private final InvalidationListener elementSizeListener = observable -> requestRender();

    private List<ElementSize> lastLayoutInput = List.of();
    private Animation activeAnimation;
    private boolean firstRender = true;

    public StringVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        emptyLabel.getStyleClass().add("visual-empty-label");
        observationLabel.getStyleClass().add("visual-empty-label");
        observationLabel.setMouseTransparent(true);
    }

    @Override
    protected void draw(StringViewState state) {
        stopActiveAnimation();
        List<Animation> transitions = new ArrayList<>();
        String value = state.value();

        if (firstRender) {
            surface.markViewportPristine();
        }
        if (value.isEmpty()) {
            invalidateLayout();
            clearCells(transitions);
            if (!surface.decorationLayer().getChildren().contains(emptyLabel)) {
                emptyLabel.relocate(EMPTY_X, EMPTY_Y);
                surface.decorationLayer().getChildren().add(emptyLabel);
            }
            updateObservationLabel(state.observation());
            play(transitions);
            surface.fitIfPristine();
            firstRender = false;
            return;
        }
        surface.decorationLayer().getChildren().remove(emptyLabel);

        for (int index = 0; index < value.length(); index++) {
            StringCellView cell = cells.get(index);
            boolean added = false;
            if (cell == null) {
                cell = new StringCellView(index, value.charAt(index));
                cell.layoutBoundsProperty().addListener(elementSizeListener);
                cells.put(index, cell);
                surface.nodeLayer().getChildren().add(cell);
                added = true;
            }
            cell.setIndex(index);
            cell.setValue(value.charAt(index));
            cell.setHighlighted(isMutationIndex(state.mutation(), index)
                    || isObservationIndex(state.observation(), index));
            cell.setCompleted(state.completed());
            if (added && !firstRender) {
                transitions.add(animations.together(
                        animations.fadeIn(cell, APPEAR_DURATION),
                        animations.scaleIn(cell, APPEAR_DURATION)));
            }
        }

        List<Integer> removed = cells.keySet().stream().filter(index -> index >= value.length()).toList();
        for (Integer index : removed) {
            StringCellView cell = cells.remove(index);
            cell.layoutBoundsProperty().removeListener(elementSizeListener);
            if (firstRender) {
                surface.nodeLayer().getChildren().remove(cell);
            } else {
                Animation fade = animations.fadeOut(cell, DISAPPEAR_DURATION);
                fade.setOnFinished(event -> surface.nodeLayer().getChildren().remove(cell));
                transitions.add(fade);
            }
        }

        List<ElementSize> measured = measureElements(value.length());
        if (!measured.equals(lastLayoutInput)) {
            lastLayoutInput = measured;
            scheduleLayout(measured);
        }
        updateObservationLabel(state.observation());
        play(transitions);
        firstRender = false;
    }

    private List<ElementSize> measureElements(int size) {
        List<ElementSize> measured = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            StringCellView cell = cells.get(index);
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
        for (Map.Entry<Integer, StringCellView> entry : cells.entrySet()) {
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
        LayoutFailureReporter.report("String", failure);
    }

    private boolean isMutationIndex(StringViewState.Mutation mutation, int index) {
        if (mutation.type() == StringViewState.Type.NONE || mutation.index() < 0) {
            return false;
        }
        int end = mutation.index() + Math.max(1, mutation.length());
        return index >= mutation.index() && index < end;
    }


    private boolean isObservationIndex(StringViewState.Observation observation, int index) {
        return switch (observation.type()) {
            case COMPARED -> observation.firstIndex() == index;
            case MATCHED -> index >= observation.firstIndex()
                    && index < observation.firstIndex() + observation.length();
            case FALLBACK, NONE -> false;
        };
    }

    private void updateObservationLabel(StringViewState.Observation observation) {
        String text = switch (observation.type()) {
            case COMPARED -> "COMPARE target[" + observation.firstIndex() + "] ↔ pattern["
                    + observation.secondIndex() + "]";
            case MATCHED -> "MATCH @" + observation.firstIndex() + " ×" + observation.length();
            case FALLBACK -> "FALLBACK " + observation.firstIndex() + " → " + observation.secondIndex();
            case NONE -> "";
        };
        observationLabel.setText(text);
        if (text.isEmpty()) {
            surface.decorationLayer().getChildren().remove(observationLabel);
            return;
        }
        observationLabel.relocate(EMPTY_X, 118.0d);
        if (!surface.decorationLayer().getChildren().contains(observationLabel)) {
            surface.decorationLayer().getChildren().add(observationLabel);
        }
    }

    private void clearCells(List<Animation> transitions) {
        if (firstRender) {
            cells.values().forEach(cell -> cell.layoutBoundsProperty().removeListener(elementSizeListener));
            cells.clear();
            surface.nodeLayer().getChildren().clear();
            return;
        }
        for (StringCellView cell : cells.values()) {
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
        observationLabel.setText("");
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
        return "string:" + index;
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
