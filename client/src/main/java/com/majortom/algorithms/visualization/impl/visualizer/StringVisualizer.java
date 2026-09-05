package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.layout.ElementBounds;
import com.majortom.algorithms.visualization.common.layout.LayoutFailureReporter;
import com.majortom.algorithms.visualization.common.layout.LayoutResult;
import com.majortom.algorithms.visualization.impl.visualizer.string.KmpPatternCellView;
import com.majortom.algorithms.visualization.impl.visualizer.string.StringCellView;
import com.majortom.algorithms.visualization.impl.visualizer.string.StringElkLayout;
import com.majortom.algorithms.visualization.impl.visualizer.string.StringElkLayout.ElementSize;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
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
 * Logical String visualizer: a continuous character track with presentation-only selection,
 * density and factual structure-mutation animation. KMP adds only a presentation overlay rail.
 */
public final class StringVisualizer extends BaseVisualizer<StringViewState> {
    private static final double EMPTY_X = 36.0d;
    private static final double EMPTY_Y = 64.0d;
    private static final double MINIMUM_AUTO_FIT_SCALE = 0.72d;
    private static final Duration MOVE_DURATION = Duration.millis(210.0d);
    private static final Duration APPEAR_DURATION = Duration.millis(170.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(140.0d);

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
    private final Map<StringCellView, Point2D> settledPositions = new LinkedHashMap<>();
    private final Set<StringCellView> exitingCells = new LinkedHashSet<>();
    private final Set<Animation> activeAnimations = new LinkedHashSet<>();
    private final Text emptyLabel = new Text("EMPTY STRING");
    private final Text observationLabel = new Text();
    private final Label patternCaption = new Label("PATTERN");
    private final HBox patternTrack = new HBox(0.0d);
    private final List<KmpPatternCellView> patternCells = new ArrayList<>();

    private List<ElementSize> lastLayoutInput = List.of();
    private java.lang.String lastRenderedValue = "";
    private boolean firstRender = true;
    private int selectedIndex = -1;
    private java.lang.String algorithmPattern = "";
    private double settledPatternX;
    private double settledPatternY;
    private IntConsumer onIndexSelected = ignored -> { };

    public StringVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        emptyLabel.getStyleClass().add("visual-empty-label");
        observationLabel.getStyleClass().add("string-observation-label");
        observationLabel.setMouseTransparent(true);
        patternCaption.getStyleClass().add("kmp-pattern-caption");
        patternCaption.setMouseTransparent(true);
        patternTrack.getStyleClass().add("kmp-pattern-track");
        patternTrack.setMouseTransparent(true);
    }

    public void setOnIndexSelected(IntConsumer onIndexSelected) {
        this.onIndexSelected = onIndexSelected == null ? ignored -> { } : onIndexSelected;
    }

    /** Algorithm-only KMP overlay input. The logical String track remains unchanged. */
    public void setAlgorithmPattern(java.lang.String pattern) {
        java.lang.String next = pattern == null ? "" : pattern;
        if (next.equals(algorithmPattern)) {
            return;
        }
        algorithmPattern = next;
        rebuildPatternCells();
        requestRender();
    }

    public void clearAlgorithmPattern() {
        if (algorithmPattern.isEmpty() && patternTrack.getChildren().isEmpty()) {
            return;
        }
        algorithmPattern = "";
        patternCells.clear();
        patternTrack.getChildren().clear();
        surface.decorationLayer().getChildren().removeAll(patternCaption, patternTrack);
        requestRender();
    }

    public void clearSelection() {
        selectedIndex = -1;
        applySelectionState();
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    @Override
    protected void draw(StringViewState state) {
        stopActiveAnimation();
        java.lang.String value = state.value();
        int size = value.length();
        boolean sourceReplacement = !firstRender
                && state.mutation().type() == StringViewState.Type.NONE
                && !state.completed()
                && !value.equals(lastRenderedValue);
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
        Set<StringCellView> enteringCells = new LinkedHashSet<>();
        boolean geometryMutation = prepareCellIdentityForMutation(
                state.mutation(), lastRenderedValue.length(), size, immediateTransitions);

        if (size == 0) {
            invalidateLayout();
            clearCells(immediateTransitions);
            if (!surface.decorationLayer().getChildren().contains(emptyLabel)) {
                emptyLabel.relocate(EMPTY_X, EMPTY_Y);
                surface.decorationLayer().getChildren().add(emptyLabel);
            }
            updateObservationLabel(state.observation());
            detachPatternOverlay();
            play(immediateTransitions);
            surface.fitWithMinimumScale(MINIMUM_AUTO_FIT_SCALE);
            firstRender = false;
            lastRenderedValue = value;
            return;
        }
        surface.decorationLayer().getChildren().remove(emptyLabel);

        VisualDensity density = densityFor(size);
        for (int index = 0; index < size; index++) {
            StringCellView cell = cells.get(index);
            if (cell == null) {
                cell = createCell(index, value.charAt(index));
                cells.put(index, cell);
                surface.nodeLayer().getChildren().add(cell);
                if (!firstRender) {
                    enteringCells.add(cell);
                }
            }
            cell.setIndex(index);
            cell.setValue(value.charAt(index));
            cell.setTrackPosition(index, size);
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
            StringCellView stray = cells.remove(index);
            removeCell(stray, immediateTransitions);
        }

        List<ElementSize> measured = measureElements(size);
        if (!measured.equals(lastLayoutInput) || geometryMutation || !enteringCells.isEmpty()) {
            lastLayoutInput = measured;
            scheduleLayout(measured, enteringCells, firstRender);
        } else {
            surface.fitWithMinimumScale(MINIMUM_AUTO_FIT_SCALE);
        }
        updateObservationLabel(state.observation());
        updatePatternOverlay(state, !firstRender);
        play(immediateTransitions);
        firstRender = false;
        lastRenderedValue = value;
    }

    private StringCellView createCell(int index, char value) {
        StringCellView cell = new StringCellView(index, value);
        cell.setSelectionHandler(this::selectIndex);
        return cell;
    }

    private void selectIndex(int index) {
        if (index < 0 || index >= lastRenderedValue.length()) {
            return;
        }
        selectedIndex = index;
        applySelectionState();
        onIndexSelected.accept(index);
    }

    private void applySelectionState() {
        VisualDensity density = densityFor(Math.max(lastRenderedValue.length(), cells.size()));
        cells.forEach((index, cell) -> {
            cell.setSelected(index == selectedIndex);
            cell.setDensity(density, index == selectedIndex);
        });
    }

    private boolean prepareCellIdentityForMutation(
            StringViewState.Mutation mutation,
            int oldSize,
            int newSize,
            List<Animation> transitions) {
        if (firstRender || mutation == null || mutation.type() == StringViewState.Type.NONE || cells.isEmpty()) {
            return false;
        }
        int index = mutation.index();
        int length = Math.max(0, mutation.length());
        switch (mutation.type()) {
            case INSERTED -> {
                if (length > 0 && newSize == oldSize + length && index >= 0 && index <= oldSize) {
                    for (int oldIndex = oldSize - 1; oldIndex >= index; oldIndex--) {
                        StringCellView cell = cells.remove(oldIndex);
                        if (cell != null) {
                            cells.put(oldIndex + length, cell);
                        }
                    }
                    return true;
                }
            }
            case REMOVED -> {
                if (length > 0 && newSize + length == oldSize && index >= 0 && index + length <= oldSize) {
                    for (int removedIndex = index; removedIndex < index + length; removedIndex++) {
                        StringCellView removed = cells.remove(removedIndex);
                        if (removed != null) {
                            removed.setObserved(false);
                            removed.setCompleted(false);
                            removed.setSelected(false);
                            removed.setCurrent(true);
                            exitingCells.add(removed);
                            transitions.add(removalAnimation(removed));
                        }
                    }
                    for (int oldIndex = index + length; oldIndex < oldSize; oldIndex++) {
                        StringCellView cell = cells.remove(oldIndex);
                        if (cell != null) {
                            cells.put(oldIndex - length, cell);
                        }
                    }
                    return true;
                }
            }
            case REPLACED -> {
                // Full/variable-length replace is a factual content replacement, not a stable slot move.
                // Rebuild presentation cells while preserving the same logical String track geometry.
                resetCellsForSourceReplacement();
                return true;
            }
            case UPDATED, NONE -> {
                return false;
            }
        }
        return false;
    }

    private Animation removalAnimation(StringCellView cell) {
        Duration duration = animations.effectiveDuration(DISAPPEAR_DURATION);
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            surface.nodeLayer().getChildren().remove(cell);
            return new javafx.animation.PauseTransition(Duration.ZERO);
        }
        FadeTransition fade = new FadeTransition(duration, cell);
        fade.setToValue(0.0d);
        Timeline lift = new Timeline(new KeyFrame(duration,
                new KeyValue(cell.translateYProperty(), -14.0d)));
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

    private void scheduleLayout(List<ElementSize> input, Set<StringCellView> enteringCells, boolean initialLayout) {
        long version = layoutVersion.incrementAndGet();
        Set<StringCellView> entering = Set.copyOf(enteringCells);
        layoutExecutor.execute(() -> {
            try {
                LayoutResult result = layout.layout(input);
                Platform.runLater(() -> applyLayout(version, result, entering, initialLayout));
            } catch (Throwable failure) {
                Platform.runLater(() -> handleLayoutFailure(version, failure));
            }
        });
    }

    private void applyLayout(long version, LayoutResult result, Set<StringCellView> enteringCells, boolean initialLayout) {
        if (isDisposed() || version != layoutVersion.get()) {
            return;
        }
        List<Animation> transitions = new ArrayList<>();
        for (Map.Entry<Integer, StringCellView> entry : cells.entrySet()) {
            ElementBounds target = result.elements().get(id(entry.getKey()));
            if (target == null) {
                continue;
            }
            StringCellView cell = entry.getValue();
            settledPositions.put(cell, new Point2D(target.x(), target.y()));
            if (initialLayout || animations.isScrubbing()) {
                snapTo(cell, target.x(), target.y());
                continue;
            }
            if (enteringCells.contains(cell)) {
                snapTo(cell, target.x(), target.y() + 16.0d);
                cell.setOpacity(0.0d);
                transitions.add(moveAndFade(cell, target.x(), target.y(), APPEAR_DURATION));
            } else if (distance(cell.getLayoutX(), cell.getLayoutY(), target.x(), target.y()) > 0.5d) {
                transitions.add(move(cell, target.x(), target.y(), MOVE_DURATION));
            } else {
                snapTo(cell, target.x(), target.y());
            }
        }
        play(transitions);
        updatePatternOverlay(currentState(), false);
        Platform.runLater(() -> surface.fitWithMinimumScale(MINIMUM_AUTO_FIT_SCALE));
    }

    private Animation move(StringCellView cell, double x, double y, Duration baseDuration) {
        Duration duration = animations.effectiveDuration(baseDuration);
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            snapTo(cell, x, y);
            return new javafx.animation.PauseTransition(Duration.ZERO);
        }
        return new Timeline(new KeyFrame(duration,
                new KeyValue(cell.layoutXProperty(), x),
                new KeyValue(cell.layoutYProperty(), y)));
    }

    private Animation moveAndFade(StringCellView cell, double x, double y, Duration baseDuration) {
        Duration duration = animations.effectiveDuration(baseDuration);
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            snapTo(cell, x, y);
            cell.setOpacity(1.0d);
            return new javafx.animation.PauseTransition(Duration.ZERO);
        }
        return new Timeline(new KeyFrame(duration,
                new KeyValue(cell.layoutXProperty(), x),
                new KeyValue(cell.layoutYProperty(), y),
                new KeyValue(cell.opacityProperty(), 1.0d)));
    }

    private void handleLayoutFailure(long version, Throwable failure) {
        if (isDisposed() || version != layoutVersion.get()) {
            return;
        }
        LayoutFailureReporter.report("String", failure);
    }

    private boolean isMutationIndex(StringViewState.Mutation mutation, int index) {
        if (mutation == null
                || mutation.type() == StringViewState.Type.NONE
                || mutation.type() == StringViewState.Type.REMOVED
                || mutation.type() == StringViewState.Type.REPLACED) {
            return false;
        }
        int length = Math.max(1, mutation.length());
        return index >= mutation.index() && index < mutation.index() + length;
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
        java.lang.String text = switch (observation.type()) {
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

    private void rebuildPatternCells() {
        patternCells.clear();
        patternTrack.getChildren().clear();
        for (int index = 0; index < algorithmPattern.length(); index++) {
            KmpPatternCellView cell = new KmpPatternCellView(index, algorithmPattern.charAt(index));
            patternCells.add(cell);
            patternTrack.getChildren().add(cell);
        }
    }

    private void updatePatternOverlay(StringViewState state, boolean animate) {
        if (state == null || state.completed() || algorithmPattern.isEmpty() || cells.isEmpty()) {
            detachPatternOverlay();
            return;
        }
        ensurePatternOverlayAttached();
        VisualDensity density = densityFor(state.value().length());
        for (int index = 0; index < patternCells.size(); index++) {
            KmpPatternCellView cell = patternCells.get(index);
            cell.setDensity(density);
            boolean observed = switch (state.observation().type()) {
                case COMPARED -> state.observation().secondIndex() == index;
                case MATCHED -> true;
                case FALLBACK, NONE -> false;
            };
            cell.setObserved(observed && !state.completed());
        }
        patternTrack.applyCss();
        patternTrack.autosize();

        StringCellView first = cells.get(0);
        if (first == null) {
            return;
        }
        first.applyCss();
        first.autosize();
        double slotWidth = first.getPrefWidth() > 0.0d ? first.getPrefWidth() : first.getLayoutBounds().getWidth();
        double targetX = first.getLayoutX() + Math.max(0, state.patternStart()) * slotWidth;
        StringCellView aligned = cells.get(state.patternStart());
        if (aligned != null) {
            targetX = aligned.getLayoutX();
        }
        double targetY = first.getLayoutY() + first.getLayoutBounds().getHeight() + 24.0d;
        double captionY = targetY - 14.0d;
        patternCaption.applyCss();
        patternCaption.autosize();
        patternCaption.relocate(first.getLayoutX() + 32.0d, captionY);

        double previousX = patternTrack.getLayoutX();
        settledPatternX = targetX;
        settledPatternY = targetY;
        patternTrack.setLayoutY(targetY);
        if (animate && !animations.isScrubbing() && Math.abs(previousX - targetX) > 0.5d) {
            Duration duration = animations.effectiveDuration(Duration.millis(180.0d));
            Timeline shift = new Timeline(new KeyFrame(duration,
                    new KeyValue(patternTrack.layoutXProperty(), targetX)));
            activeAnimations.add(shift);
            shift.setOnFinished(event -> activeAnimations.remove(shift));
            shift.play();
        } else {
            patternTrack.setLayoutX(targetX);
        }

        if (!observationLabel.getText().isEmpty()) {
            observationLabel.relocate(first.getLayoutX(),
                    targetY + patternTrack.getLayoutBounds().getHeight() + 10.0d);
        }
    }

    private void ensurePatternOverlayAttached() {
        if (!surface.decorationLayer().getChildren().contains(patternCaption)) {
            surface.decorationLayer().getChildren().add(patternCaption);
        }
        if (!surface.decorationLayer().getChildren().contains(patternTrack)) {
            surface.decorationLayer().getChildren().add(patternTrack);
        }
    }

    private void detachPatternOverlay() {
        surface.decorationLayer().getChildren().removeAll(patternCaption, patternTrack);
    }

    private void clearCells(List<Animation> transitions) {
        if (firstRender) {
            cells.clear();
            settledPositions.clear();
            surface.nodeLayer().getChildren().clear();
            return;
        }
        for (StringCellView cell : List.copyOf(cells.values())) {
            removeCell(cell, transitions);
        }
        cells.clear();
    }

    private void removeCell(StringCellView cell, List<Animation> transitions) {
        if (cell == null) {
            return;
        }
        if (firstRender || animations.isScrubbing()) {
            settledPositions.remove(cell);
            surface.nodeLayer().getChildren().remove(cell);
            return;
        }
        exitingCells.add(cell);
        transitions.add(removalAnimation(cell));
    }

    private void resetCellsForSourceReplacement() {
        invalidateLayout();
        cells.clear();
        settledPositions.clear();
        exitingCells.clear();
        surface.nodeLayer().getChildren().clear();
    }

    private void normalizeCellOrder() {
        List<StringCellView> ordered = cells.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
        for (int index = 0; index < ordered.size(); index++) {
            StringCellView cell = ordered.get(index);
            int current = surface.nodeLayer().getChildren().indexOf(cell);
            if (current != index) {
                surface.nodeLayer().getChildren().remove(cell);
                surface.nodeLayer().getChildren().add(index, cell);
            }
        }
    }

    private VisualDensity densityFor(int size) {
        if (size <= 24) {
            return VisualDensity.DETAIL;
        }
        if (size <= 48) {
            return VisualDensity.COMPACT;
        }
        return VisualDensity.DENSE;
    }

    private void invalidateLayout() {
        layoutVersion.incrementAndGet();
        lastLayoutInput = List.of();
    }

    private void play(List<Animation> transitions) {
        for (Animation transition : transitions) {
            if (transition == null) {
                continue;
            }
            activeAnimations.add(transition);
            transition.setOnFinished(event -> activeAnimations.remove(transition));
            transition.play();
        }
    }

    private void stopActiveAnimation() {
        for (Animation animation : List.copyOf(activeAnimations)) {
            animation.stop();
        }
        activeAnimations.clear();
        for (Map.Entry<StringCellView, Point2D> entry : settledPositions.entrySet()) {
            StringCellView cell = entry.getKey();
            if (cells.containsValue(cell)) {
                Point2D target = entry.getValue();
                snapTo(cell, target.getX(), target.getY());
                cell.setOpacity(1.0d);
                cell.setTranslateY(0.0d);
            }
        }
        for (StringCellView cell : List.copyOf(exitingCells)) {
            settledPositions.remove(cell);
            surface.nodeLayer().getChildren().remove(cell);
        }
        exitingCells.clear();
        if (!algorithmPattern.isEmpty() && patternTrack.getParent() != null) {
            patternTrack.relocate(settledPatternX, settledPatternY);
        }
    }

    private static void snapTo(StringCellView cell, double x, double y) {
        cell.relocate(x, y);
        cell.setTranslateX(0.0d);
        cell.setTranslateY(0.0d);
        cell.setOpacity(1.0d);
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
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
        surface.nodeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
        observationLabel.setText("");
        settledPatternX = 0.0d;
        settledPatternY = 0.0d;
        selectedIndex = -1;
        lastRenderedValue = "";
        firstRender = true;
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        stopActiveAnimation();
        invalidateLayout();
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
