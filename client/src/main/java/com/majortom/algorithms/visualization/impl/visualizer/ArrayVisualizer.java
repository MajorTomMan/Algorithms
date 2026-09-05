package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.layout.ElementBounds;
import com.majortom.algorithms.visualization.common.layout.LayoutFailureReporter;
import com.majortom.algorithms.visualization.common.layout.LayoutResult;
import com.majortom.algorithms.visualization.impl.visualizer.array.ArrayCellView;
import com.majortom.algorithms.visualization.impl.visualizer.array.ArrayElkLayout;
import com.majortom.algorithms.visualization.impl.visualizer.array.ArrayElkLayout.ElementSize;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;

/**
 * Logical Array visualizer: one continuous memory strip with presentation-only selection,
 * density and structure-driven spatial mutation animation.
 */
public final class ArrayVisualizer extends BaseVisualizer<ArrayViewState> {
    private static final double EMPTY_X = 36.0d;
    private static final double EMPTY_Y = 64.0d;
    private static final double MINIMUM_AUTO_FIT_SCALE = 0.70d;
    private static final Duration MOVE_DURATION = Duration.millis(220.0d);
    private static final Duration APPEAR_DURATION = Duration.millis(180.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(150.0d);

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
    private final Map<ArrayCellView, Point2D> settledPositions = new LinkedHashMap<>();
    private final Set<ArrayCellView> exitingCells = new LinkedHashSet<>();
    private final Set<Animation> activeAnimations = new LinkedHashSet<>();
    private final Text emptyLabel = new Text("EMPTY ARRAY");

    private List<ElementSize> lastLayoutInput = List.of();
    private List<Integer> lastRenderedValues = List.of();
    private boolean firstRender = true;
    private int selectedIndex = -1;
    private IntConsumer onIndexSelected = ignored -> { };

    public ArrayVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        emptyLabel.getStyleClass().add("visual-empty-label");
    }

    public void setOnIndexSelected(IntConsumer onIndexSelected) {
        this.onIndexSelected = onIndexSelected == null ? ignored -> { } : onIndexSelected;
    }

    public void clearSelection() {
        selectedIndex = -1;
        applySelectionState();
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    @Override
    protected void draw(ArrayViewState state) {
        stopActiveAnimation();
        int size = state.values().size();
        boolean sourceReplacement = !firstRender
                && state.mutation().type() == ArrayViewState.Type.NONE
                && !state.completed()
                && !state.values().equals(lastRenderedValues);
        if (firstRender || sourceReplacement) {
            surface.markViewportPristine();
        }
        if (sourceReplacement) {
            resetCellsForSourceReplacement();
            firstRender = true;
        }
        if (selectedIndex >= size) {
            selectedIndex = -1;
        }

        List<Animation> immediateTransitions = new ArrayList<>();
        Set<ArrayCellView> enteringCells = new LinkedHashSet<>();
        Set<ArrayCellView> swappedCells = new LinkedHashSet<>();
        boolean geometryMutation = prepareCellIdentityForMutation(
                state.mutation(), lastRenderedValues.size(), size, immediateTransitions, swappedCells);

        if (size == 0) {
            invalidateLayout();
            clearCells(immediateTransitions);
            if (!surface.decorationLayer().getChildren().contains(emptyLabel)) {
                emptyLabel.relocate(EMPTY_X, EMPTY_Y);
                surface.decorationLayer().getChildren().add(emptyLabel);
            }
            play(immediateTransitions);
            surface.fitWithMinimumScale(MINIMUM_AUTO_FIT_SCALE);
            firstRender = false;
            lastRenderedValues = state.values();
            return;
        }
        surface.decorationLayer().getChildren().remove(emptyLabel);

        VisualDensity density = densityFor(size);
        for (int index = 0; index < size; index++) {
            ArrayCellView cell = cells.get(index);
            if (cell == null) {
                cell = createCell(index, state.values().get(index));
                cells.put(index, cell);
                surface.nodeLayer().getChildren().add(cell);
                if (!firstRender) {
                    enteringCells.add(cell);
                }
            }
            cell.setIndex(index);
            cell.setValue(state.values().get(index));
            cell.setStripPosition(index, size);
            boolean mutationIndex = isMutationIndex(state.mutation(), index);
            boolean observationIndex = isObservationIndex(state.observation(), index);
            cell.setObserved((mutationIndex || observationIndex) && !state.completed());
            cell.setCompleted(state.completed());
            cell.setSelected(index == selectedIndex);
            cell.setDensity(density, mutationIndex || observationIndex || index == selectedIndex);
        }
        normalizeCellOrder();

        List<Integer> strayIndexes = cells.keySet().stream().filter(index -> index >= size).toList();
        for (Integer index : strayIndexes) {
            ArrayCellView stray = cells.remove(index);
            removeCell(stray, immediateTransitions);
        }

        List<ElementSize> measured = measureElements(size);
        if (!measured.equals(lastLayoutInput) || geometryMutation || !enteringCells.isEmpty()) {
            lastLayoutInput = measured;
            scheduleLayout(measured, enteringCells, swappedCells, firstRender);
        } else {
            surface.fitWithMinimumScale(MINIMUM_AUTO_FIT_SCALE);
        }
        play(immediateTransitions);
        firstRender = false;
        lastRenderedValues = state.values();
    }

    private ArrayCellView createCell(int index, int value) {
        ArrayCellView cell = new ArrayCellView(index, value);
        cell.setSelectionHandler(this::selectIndex);
        return cell;
    }

    private void selectIndex(int index) {
        if (index < 0 || index >= lastRenderedValues.size()) {
            return;
        }
        selectedIndex = index;
        applySelectionState();
        onIndexSelected.accept(index);
    }

    private void applySelectionState() {
        cells.forEach((index, cell) -> {
            cell.setSelected(index == selectedIndex);
            cell.setDensity(densityFor(Math.max(lastRenderedValues.size(), cells.size())), index == selectedIndex);
        });
    }

    private boolean prepareCellIdentityForMutation(
            ArrayViewState.Mutation mutation,
            int oldSize,
            int newSize,
            List<Animation> transitions,
            Set<ArrayCellView> swappedCells) {
        if (firstRender || mutation == null || mutation.type() == ArrayViewState.Type.NONE || cells.isEmpty()) {
            return false;
        }
        switch (mutation.type()) {
            case INSERTED -> {
                int index = mutation.index();
                if (newSize == oldSize + 1 && index >= 0 && index <= oldSize) {
                    for (int oldIndex = oldSize - 1; oldIndex >= index; oldIndex--) {
                        ArrayCellView cell = cells.remove(oldIndex);
                        if (cell != null) {
                            cells.put(oldIndex + 1, cell);
                        }
                    }
                    return true;
                }
            }
            case REMOVED -> {
                int index = mutation.index();
                if (newSize + 1 == oldSize && index >= 0 && index < oldSize) {
                    ArrayCellView removed = cells.remove(index);
                    if (removed != null) {
                        removed.setObserved(false);
                        removed.setCompleted(false);
                        removed.setSelected(false);
                        removed.setCurrent(true);
                        exitingCells.add(removed);
                        transitions.add(removalAnimation(removed));
                    }
                    for (int oldIndex = index + 1; oldIndex < oldSize; oldIndex++) {
                        ArrayCellView cell = cells.remove(oldIndex);
                        if (cell != null) {
                            cells.put(oldIndex - 1, cell);
                        }
                    }
                    return true;
                }
            }
            case SWAPPED -> {
                int left = mutation.index();
                int right = mutation.otherIndex();
                if (left >= 0 && right >= 0 && left < oldSize && right < oldSize && left != right) {
                    ArrayCellView leftCell = cells.get(left);
                    ArrayCellView rightCell = cells.get(right);
                    if (leftCell != null && rightCell != null) {
                        cells.put(left, rightCell);
                        cells.put(right, leftCell);
                        swappedCells.add(leftCell);
                        swappedCells.add(rightCell);
                        return true;
                    }
                }
            }
            case UPDATED -> {
                return false;
            }
            case NONE -> {
                return false;
            }
        }
        return false;
    }

    private Animation removalAnimation(ArrayCellView cell) {
        Duration duration = animations.effectiveDuration(DISAPPEAR_DURATION);
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            surface.nodeLayer().getChildren().remove(cell);
            return new javafx.animation.PauseTransition(Duration.ZERO);
        }
        FadeTransition fade = new FadeTransition(duration, cell);
        fade.setToValue(0.0d);
        Timeline lift = new Timeline(new KeyFrame(duration,
                new KeyValue(cell.translateYProperty(), -18.0d)));
        ParallelTransition transition = new ParallelTransition(fade, lift);
        transition.setOnFinished(event -> {
            exitingCells.remove(cell);
            settledPositions.remove(cell);
            surface.nodeLayer().getChildren().remove(cell);
            cell.setTranslateY(0.0d);
            cell.setOpacity(1.0d);
        });
        return transition;
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

    private void scheduleLayout(
            List<ElementSize> input,
            Set<ArrayCellView> enteringCells,
            Set<ArrayCellView> swappedCells,
            boolean initialLayout) {
        long version = layoutVersion.incrementAndGet();
        Set<ArrayCellView> entering = Set.copyOf(enteringCells);
        Set<ArrayCellView> swapping = Set.copyOf(swappedCells);
        layoutExecutor.execute(() -> {
            try {
                LayoutResult result = layout.layout(input);
                Platform.runLater(() -> applyLayout(version, result, entering, swapping, initialLayout));
            } catch (Throwable failure) {
                Platform.runLater(() -> handleLayoutFailure(version, failure));
            }
        });
    }

    private void applyLayout(
            long version,
            LayoutResult result,
            Set<ArrayCellView> enteringCells,
            Set<ArrayCellView> swappedCells,
            boolean initialLayout) {
        if (isDisposed() || version != layoutVersion.get()) {
            return;
        }
        List<Animation> transitions = new ArrayList<>();
        for (Map.Entry<Integer, ArrayCellView> entry : cells.entrySet()) {
            ElementBounds target = result.elements().get(id(entry.getKey()));
            if (target == null) {
                continue;
            }
            ArrayCellView cell = entry.getValue();
            settledPositions.put(cell, new Point2D(target.x(), target.y()));
            if (initialLayout || animations.isScrubbing()) {
                snapTo(cell, target.x(), target.y());
                continue;
            }
            if (enteringCells.contains(cell)) {
                snapTo(cell, target.x(), target.y() + 20.0d);
                cell.setOpacity(0.0d);
                transitions.add(moveAndFade(cell, target.x(), target.y(), APPEAR_DURATION));
            } else if (swappedCells.contains(cell)) {
                transitions.add(swapArc(cell, target.x(), target.y(), MOVE_DURATION));
            } else if (distance(cell.getLayoutX(), cell.getLayoutY(), target.x(), target.y()) > 0.5d) {
                transitions.add(move(cell, target.x(), target.y(), MOVE_DURATION));
            } else {
                snapTo(cell, target.x(), target.y());
            }
        }
        play(transitions);
        Platform.runLater(() -> surface.fitWithMinimumScale(MINIMUM_AUTO_FIT_SCALE));
    }

    private Animation move(ArrayCellView cell, double x, double y, Duration baseDuration) {
        Duration duration = animations.effectiveDuration(baseDuration);
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            snapTo(cell, x, y);
            return new javafx.animation.PauseTransition(Duration.ZERO);
        }
        return new Timeline(new KeyFrame(duration,
                new KeyValue(cell.layoutXProperty(), x),
                new KeyValue(cell.layoutYProperty(), y)));
    }

    private Animation moveAndFade(ArrayCellView cell, double x, double y, Duration baseDuration) {
        Duration duration = animations.effectiveDuration(baseDuration);
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            snapTo(cell, x, y);
            cell.setOpacity(1.0d);
            return new javafx.animation.PauseTransition(Duration.ZERO);
        }
        Timeline move = new Timeline(new KeyFrame(duration,
                new KeyValue(cell.layoutXProperty(), x),
                new KeyValue(cell.layoutYProperty(), y),
                new KeyValue(cell.opacityProperty(), 1.0d)));
        return move;
    }

    private Animation swapArc(ArrayCellView cell, double x, double y, Duration baseDuration) {
        Duration duration = animations.effectiveDuration(baseDuration);
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            snapTo(cell, x, y);
            return new javafx.animation.PauseTransition(Duration.ZERO);
        }
        double startX = cell.getLayoutX();
        double startY = cell.getLayoutY();
        double arc = x > startX ? -16.0d : 16.0d;
        return new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(cell.layoutXProperty(), startX),
                        new KeyValue(cell.layoutYProperty(), startY),
                        new KeyValue(cell.translateYProperty(), 0.0d)),
                new KeyFrame(duration.multiply(0.5d),
                        new KeyValue(cell.layoutXProperty(), (startX + x) / 2.0d),
                        new KeyValue(cell.layoutYProperty(), (startY + y) / 2.0d),
                        new KeyValue(cell.translateYProperty(), arc)),
                new KeyFrame(duration,
                        new KeyValue(cell.layoutXProperty(), x),
                        new KeyValue(cell.layoutYProperty(), y),
                        new KeyValue(cell.translateYProperty(), 0.0d)));
    }

    private void handleLayoutFailure(long version, Throwable failure) {
        if (isDisposed() || version != layoutVersion.get()) {
            return;
        }
        LayoutFailureReporter.report("Array", failure);
    }

    private boolean isMutationIndex(ArrayViewState.Mutation mutation, int index) {
        if (mutation == null || mutation.type() == ArrayViewState.Type.NONE || mutation.type() == ArrayViewState.Type.REMOVED) {
            return false;
        }
        return index == mutation.index() || index == mutation.otherIndex();
    }

    private boolean isObservationIndex(ArrayViewState.Observation observation, int index) {
        return switch (observation.type()) {
            case COMPARED_INDEXES -> observation.firstIndex() == index || observation.secondIndex() == index;
            case COMPARED_VALUE -> observation.firstIndex() == index;
            case NONE -> false;
        };
    }

    private void clearCells(List<Animation> transitions) {
        if (firstRender) {
            cells.clear();
            surface.nodeLayer().getChildren().clear();
            return;
        }
        for (ArrayCellView cell : List.copyOf(cells.values())) {
            removeCell(cell, transitions);
        }
        cells.clear();
    }

    private void removeCell(ArrayCellView cell, List<Animation> transitions) {
        if (cell == null) {
            return;
        }
        if (firstRender || animations.isScrubbing()) {
            settledPositions.remove(cell);
            surface.nodeLayer().getChildren().remove(cell);
            return;
        }
        Animation fade = animations.fadeOut(cell, DISAPPEAR_DURATION);
        exitingCells.add(cell);
        fade.setOnFinished(event -> {
            exitingCells.remove(cell);
            settledPositions.remove(cell);
            surface.nodeLayer().getChildren().remove(cell);
            cell.setOpacity(1.0d);
        });
        transitions.add(fade);
    }

    private void resetCellsForSourceReplacement() {
        invalidateLayout();
        for (ArrayCellView cell : cells.values()) {
        }
        for (ArrayCellView cell : exitingCells) {
        }
        cells.clear();
        settledPositions.clear();
        exitingCells.clear();
        surface.nodeLayer().getChildren().clear();
        lastLayoutInput = List.of();
    }

    private void normalizeCellOrder() {
        List<Map.Entry<Integer, ArrayCellView>> entries = new ArrayList<>(cells.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        cells.clear();
        for (Map.Entry<Integer, ArrayCellView> entry : entries) {
            cells.put(entry.getKey(), entry.getValue());
        }
        List<ArrayCellView> visualOrder = cells.values().stream()
                .sorted(Comparator.comparingInt(ArrayCellView::index))
                .toList();
        surface.nodeLayer().getChildren().removeAll(visualOrder);
        surface.nodeLayer().getChildren().addAll(visualOrder);
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
        activeAnimations.add(parallel);
        parallel.setOnFinished(event -> activeAnimations.remove(parallel));
        parallel.play();
    }

    /**
     * Interrupted Structure animations settle at their factual target geometry.
     * This prevents rapid CRUD, resize or another render from leaving a half-entered cell behind.
     */
    private void stopActiveAnimation() {
        for (Animation animation : List.copyOf(activeAnimations)) {
            animation.stop();
        }
        activeAnimations.clear();
        for (ArrayCellView cell : cells.values()) {
            Point2D target = settledPositions.get(cell);
            if (target != null) {
                snapTo(cell, target.getX(), target.getY());
            }
            cell.setOpacity(1.0d);
            cell.setTranslateY(0.0d);
        }
        for (ArrayCellView cell : List.copyOf(exitingCells)) {
            settledPositions.remove(cell);
            surface.nodeLayer().getChildren().remove(cell);
            cell.setOpacity(1.0d);
            cell.setTranslateY(0.0d);
        }
        exitingCells.clear();
    }

    @Override
    public void setViewportObstructionInsets(javafx.geometry.Insets insets) {
        surface.setObstructionInsets(insets);
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
        cells.clear();
        settledPositions.clear();
        exitingCells.clear();
        selectedIndex = -1;
        lastRenderedValues = List.of();
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
        settledPositions.clear();
        exitingCells.clear();
        layoutExecutor.shutdownNow();
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private static VisualDensity densityFor(int size) {
        if (size <= 16) {
            return VisualDensity.DETAIL;
        }
        if (size <= 40) {
            return VisualDensity.COMPACT;
        }
        return VisualDensity.DENSE;
    }

    private static void snapTo(ArrayCellView cell, double x, double y) {
        cell.relocate(x, y);
        cell.setTranslateY(0.0d);
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
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
