package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.impl.visualizer.string.KmpPatternCellView;
import com.majortom.algorithms.visualization.impl.visualizer.string.StringCellView;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.fx.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.fx.RenderCommitContext;
import com.majortom.algorithms.visualization.render.layout.LinearLayoutEngine;
import com.majortom.algorithms.visualization.render.runtime.DefaultRenderFramework;
import com.majortom.algorithms.visualization.render.runtime.RenderRuntime;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.render.viewport.CameraState;
import com.majortom.algorithms.visualization.render.viewport.ViewportSnapshot;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Logical String visualizer: a continuous character track with presentation-only selection, density
 * and factual structure-mutation animation. KMP adds only a presentation overlay rail.
 */
public final class StringVisualizer extends BaseVisualizer<StringViewState>
        implements FxSurfaceAdapter<StringViewState> {
    private static final RenderSessionId SESSION_ID = RenderSessionId.of("STRING");
    private static final double EMPTY_X = 36.0d;
    private static final double EMPTY_Y = 64.0d;
    private static final double MINIMUM_AUTO_FIT_SCALE = 0.72d;
    private static final Duration MOVE_DURATION = Duration.millis(210.0d);
    private static final Duration APPEAR_DURATION = Duration.millis(170.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(140.0d);

    private final VisualizationSurface surface = new VisualizationSurface();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final DefaultRenderFramework renderFramework = RenderRuntime.shared();
    private final Map<Integer, StringCellView> cells = new LinkedHashMap<>();
    private final Map<StringCellView, Point2D> settledPositions = new LinkedHashMap<>();
    private final Set<StringCellView> exitingCells = new LinkedHashSet<>();
    private final Set<Animation> activeAnimations = new LinkedHashSet<>();
    private final Text emptyLabel = new Text();
    private final Text observationLabel = new Text();
    private final Label patternCaption = new Label();
    private final HBox patternTrack = new HBox(0.0d);
    private final List<KmpPatternCellView> patternCells = new ArrayList<>();

    private volatile StringViewState lastSubmittedState;
    private PendingLayout pendingLayout = PendingLayout.empty();
    private java.lang.String lastRenderedValue = "";
    private boolean firstRender = true;
    private int selectedIndex = -1;
    private int pendingSelectedIndex = -1;
    private java.lang.String algorithmPattern = "";
    private double settledPatternX;
    private double settledPatternY;
    private IntConsumer onIndexSelected = ignored -> {};

    public StringVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        surface.setFrameworkManagedCamera(true);
        emptyLabel.getStyleClass().add("visual-empty-label");
        emptyLabel.textProperty().bind(I18N.createStringBinding("label.visual.string.empty"));
        observationLabel.getStyleClass().add("string-observation-label");
        observationLabel.setMouseTransparent(true);
        patternCaption.getStyleClass().add("kmp-pattern-caption");
        patternCaption.textProperty().bind(I18N.createStringBinding("label.visual.string.pattern"));
        patternCaption.setMouseTransparent(true);
        patternTrack.getStyleClass().add("kmp-pattern-track");
        patternTrack.setMouseTransparent(true);
        renderFramework.registerSurface(SESSION_ID, this);
    }

    @Override
    protected synchronized void submitFrameworkRender(StringViewState state) {
        StringViewState previous = lastSubmittedState;
        boolean cold = previous == null;
        boolean replacement = sourceReplacement(previous, state);
        boolean initial = cold || replacement;
        boolean structural = initial || !previous.value().equals(state.value());
        lastSubmittedState = state;
        if (structural) {
            CameraPolicy camera =
                    replacement
                            ? CameraPolicy.FIT_CONTENT
                            : (cold ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE);
            renderFramework.submit(
                    new StructuralRenderIntent<>(SESSION_ID, state, camera, initial));
        } else {
            renderFramework.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
    }

    public void setOnIndexSelected(IntConsumer onIndexSelected) {
        if (onIndexSelected == null) {
            this.onIndexSelected = ignored -> {};
        } else {
            this.onIndexSelected = onIndexSelected;
        }
    }

    /** Algorithm-only KMP overlay input. The logical String track remains unchanged. */
    public void setAlgorithmPattern(java.lang.String pattern) {
        java.lang.String next;
        if (pattern == null) {
            next = "";
        } else {
            next = pattern;
        }
        if (next.equals(algorithmPattern)) {
            return;
        }
        algorithmPattern = next;
        rebuildPatternCells();
        submitCurrentPresentation();
    }

    public void clearAlgorithmPattern() {
        if (algorithmPattern.isEmpty() && patternTrack.getChildren().isEmpty()) {
            return;
        }
        algorithmPattern = "";
        patternCells.clear();
        patternTrack.getChildren().clear();
        surface.decorationLayer().getChildren().removeAll(patternCaption, patternTrack);
        submitCurrentPresentation();
    }

    public void clearSelection() {
        selectedIndex = -1;
        pendingSelectedIndex = -1;
        submitCurrentPresentation();
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    @Override
    public LayoutRequest captureLayout(StringViewState state, RenderCaptureContext context) {
        stopActiveAnimation();
        java.lang.String value = state.value();
        int size = value.length();
        boolean replacement = sourceReplacement(lastRenderedValue, state);
        if (replacement) {
            resetCellsForSourceReplacement();
            firstRender = true;
        }
        if (selectedIndex >= size) selectedIndex = -1;
        applyPendingSelection(size);

        List<Animation> immediateTransitions = new ArrayList<>();
        Set<StringCellView> enteringCells = new LinkedHashSet<>();
        if (!animations.isScrubbing()) {
            prepareCellIdentityForMutation(
                    state.mutation(), lastRenderedValue.length(), size, immediateTransitions);
        }

        if (size == 0) {
            clearCells(immediateTransitions);
            if (!surface.decorationLayer().getChildren().contains(emptyLabel)) {
                emptyLabel.relocate(EMPTY_X, EMPTY_Y);
                surface.decorationLayer().getChildren().add(emptyLabel);
            }
            pendingLayout = new PendingLayout(immediateTransitions, Set.of());
            return request(context, List.of());
        }
        surface.decorationLayer().getChildren().remove(emptyLabel);

        VisualDensity density = densityFor(size);
        for (int index = 0; index < size; index++) {
            StringCellView cell = cells.get(index);
            if (cell == null) {
                cell = createCell(index, value.charAt(index));
                cells.put(index, cell);
                surface.nodeLayer().getChildren().add(cell);
                if (!context.initialFrame()) enteringCells.add(cell);
            }
            applyCellPresentation(cell, state, index, density);
        }
        normalizeCellOrder();
        List<Integer> strayIndexes =
                cells.keySet().stream().filter(index -> index >= size).toList();
        for (Integer index : strayIndexes) removeCell(cells.remove(index), immediateTransitions);

        List<LayoutElement> measured = measureElements(size);
        pendingLayout = new PendingLayout(immediateTransitions, enteringCells);
        return request(context, measured);
    }

    @Override
    public CompletionStage<Void> commitLayout(
            StringViewState state, LayoutPatch patch, RenderCommitContext context) {
        PendingLayout pending = pendingLayout;
        List<Animation> transitions = new ArrayList<>(pending.immediateTransitions());
        boolean snap = context.initialFrame() || animations.isScrubbing();
        for (Map.Entry<Integer, StringCellView> entry : cells.entrySet()) {
            ElementGeometry target = patch.elements().get(id(entry.getKey()));
            if (target == null) continue;
            StringCellView cell = entry.getValue();
            settledPositions.put(cell, new Point2D(target.x(), target.y()));
            if (snap) {
                snapTo(cell, target.x(), target.y());
            } else if (pending.enteringCells().contains(cell)) {
                snapTo(cell, target.x(), target.y() + 16.0d);
                cell.setOpacity(0.0d);
                transitions.add(moveAndFade(cell, target.x(), target.y(), APPEAR_DURATION));
            } else if (distance(cell.getLayoutX(), cell.getLayoutY(), target.x(), target.y())
                    > 0.5d) {
                transitions.add(move(cell, target.x(), target.y(), MOVE_DURATION));
            } else {
                snapTo(cell, target.x(), target.y());
            }
        }
        pendingLayout = PendingLayout.empty();
        firstRender = false;
        lastRenderedValue = state.value();
        updateObservationLabel(state.observation());
        if (snap) {
            transitions.forEach(Animation::stop);
            cleanupExitingCells();
            updatePatternOverlay(state, false);
            return CompletableFuture.completedFuture(null);
        }
        return playAsync(transitions).thenRun(() -> updatePatternOverlay(state, false));
    }

    @Override
    public CompletionStage<Void> commitPresentation(
            StringViewState state, RenderCommitContext context) {
        stopActiveAnimation();
        int size = state.value().length();
        if (selectedIndex >= size) selectedIndex = -1;
        applyPendingSelection(size);
        VisualDensity density = densityFor(size);
        for (int index = 0; index < size; index++) {
            StringCellView cell = cells.get(index);
            if (cell != null) applyCellPresentation(cell, state, index, density);
        }
        updateObservationLabel(state.observation());
        updatePatternOverlay(state, true);
        lastRenderedValue = state.value();
        return CompletableFuture.completedFuture(null);
    }

    private LayoutRequest request(RenderCaptureContext context, List<LayoutElement> elements) {
        return new LayoutRequest(
                context.requestId(),
                context.sessionId(),
                context.modelRevision(),
                context.geometryRevision(),
                LinearLayoutEngine.ID,
                elements,
                Map.of(
                        "structure",
                        "string",
                        "direction",
                        "RIGHT",
                        "padding",
                        "28",
                        "spacing",
                        "0"));
    }

    private void applyCellPresentation(
            StringCellView cell, StringViewState state, int index, VisualDensity density) {
        cell.setIndex(index);
        cell.setValue(state.value().charAt(index));
        cell.setTrackPosition(index, state.value().length());
        boolean mutationIndex = isMutationIndex(state.mutation(), index);
        boolean observationIndex = isObservationIndex(state.observation(), index);
        cell.setObserved((mutationIndex || observationIndex) && !state.completed());
        cell.setCompleted(state.completed());
        cell.setSelected(index == selectedIndex);
        cell.setDensity(density, mutationIndex || observationIndex || index == selectedIndex);
    }

    private void applyPendingSelection(int size) {
        if (selectedIndex >= size) selectedIndex = -1;
        if (pendingSelectedIndex < 0) return;
        if (pendingSelectedIndex < size) {
            selectedIndex = pendingSelectedIndex;
            onIndexSelected.accept(selectedIndex);
        }
        pendingSelectedIndex = -1;
    }

    private StringCellView createCell(int index, char value) {
        StringCellView cell = new StringCellView(index, value);
        cell.setSelectionHandler(this::selectIndex);
        return cell;
    }

    public void selectIndex(int index) {
        if (!showSelection(index)) {
            return;
        }
        onIndexSelected.accept(index);
    }

    /**
     * Keeps a presentation selection on the same character index without re-firing the click
     * callback.
     */
    public boolean showSelection(int index) {
        if (index < 0) {
            return false;
        }
        StringViewState state = currentState();
        int currentSize;
        if (state == null) {
            currentSize = lastRenderedValue.length();
        } else {
            currentSize = state.value().length();
        }
        if (index >= currentSize) {
            return false;
        }
        selectedIndex = index;
        if (!cells.containsKey(index)) {
            pendingSelectedIndex = index;
            submitCurrentPresentation();
            return true;
        }
        pendingSelectedIndex = -1;
        applySelectionState();
        return true;
    }

    private void submitCurrentPresentation() {
        StringViewState state = currentState();
        if (state != null && isModuleAttached() && !isDisposed()) {
            renderFramework.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
    }

    private static boolean sourceReplacement(StringViewState previous, StringViewState current) {
        return previous != null
                && current.mutation().type() == StringViewState.Type.NONE
                && !current.completed()
                && !current.value().equals(previous.value());
    }

    private static boolean sourceReplacement(
            java.lang.String previousValue, StringViewState current) {
        return !previousValue.isEmpty()
                && current.mutation().type() == StringViewState.Type.NONE
                && !current.completed()
                && !current.value().equals(previousValue);
    }

    private void applySelectionState() {
        VisualDensity density = densityFor(Math.max(lastRenderedValue.length(), cells.size()));
        cells.forEach(
                (index, cell) -> {
                    cell.setSelected(index == selectedIndex);
                    cell.setDensity(density, index == selectedIndex);
                });
    }

    private boolean prepareCellIdentityForMutation(
            StringViewState.Mutation mutation,
            int oldSize,
            int newSize,
            List<Animation> transitions) {
        if (firstRender
                || mutation == null
                || mutation.type() == StringViewState.Type.NONE
                || cells.isEmpty()) {
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
                if (length > 0
                        && newSize + length == oldSize
                        && index >= 0
                        && index + length <= oldSize) {
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
                // Full/variable-length replace is a factual content replacement, not a stable slot
                // move.
                // Rebuild presentation cells while preserving the same logical String track
                // geometry.
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
        Timeline lift =
                new Timeline(
                        new KeyFrame(duration, new KeyValue(cell.translateYProperty(), -14.0d)));
        ParallelTransition transition = new ParallelTransition(fade, lift);
        transition.setOnFinished(
                event -> {
                    exitingCells.remove(cell);
                    settledPositions.remove(cell);
                    surface.nodeLayer().getChildren().remove(cell);
                    cell.setTranslateY(0.0d);
                    cell.setOpacity(1.0d);
                });
        return transition;
    }

    private List<LayoutElement> measureElements(int size) {
        List<LayoutElement> measured = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            StringCellView cell = cells.get(index);
            cell.applyCss();
            cell.autosize();
            Bounds bounds = cell.getLayoutBounds();
            double width = positive(bounds.getWidth(), cell.prefWidth(-1.0d));
            double height = positive(bounds.getHeight(), cell.prefHeight(-1.0d));
            measured.add(new LayoutElement(id(index), quantize(width), quantize(height)));
        }
        return List.copyOf(measured);
    }

    private Animation move(StringCellView cell, double x, double y, Duration baseDuration) {
        Duration duration = animations.effectiveDuration(baseDuration);
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            snapTo(cell, x, y);
            return new javafx.animation.PauseTransition(Duration.ZERO);
        }
        return new Timeline(
                new KeyFrame(
                        duration,
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
        return new Timeline(
                new KeyFrame(
                        duration,
                        new KeyValue(cell.layoutXProperty(), x),
                        new KeyValue(cell.layoutYProperty(), y),
                        new KeyValue(cell.opacityProperty(), 1.0d)));
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
            case MATCHED ->
                    index >= observation.firstIndex()
                            && index < observation.firstIndex() + observation.length();
            case FALLBACK, NONE -> false;
        };
    }

    private void updateObservationLabel(StringViewState.Observation observation) {
        java.lang.String text =
                switch (observation.type()) {
                    case COMPARED ->
                            "COMPARE target["
                                    + observation.firstIndex()
                                    + "] ↔ pattern["
                                    + observation.secondIndex()
                                    + "]";
                    case MATCHED ->
                            "MATCH @" + observation.firstIndex() + " ×" + observation.length();
                    case FALLBACK ->
                            "FALLBACK "
                                    + observation.firstIndex()
                                    + " → "
                                    + observation.secondIndex();
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
            boolean observed =
                    switch (state.observation().type()) {
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
        double slotWidth;
        if (first.getPrefWidth() > 0.0d) {
            slotWidth = first.getPrefWidth();
        } else {
            slotWidth = first.getLayoutBounds().getWidth();
        }
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
            Timeline shift =
                    new Timeline(
                            new KeyFrame(
                                    duration,
                                    new KeyValue(patternTrack.layoutXProperty(), targetX)));
            activeAnimations.add(shift);
            shift.setOnFinished(event -> activeAnimations.remove(shift));
            shift.play();
        } else {
            patternTrack.setLayoutX(targetX);
        }

        if (!observationLabel.getText().isEmpty()) {
            observationLabel.relocate(
                    first.getLayoutX(),
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
        cells.clear();
        settledPositions.clear();
        exitingCells.clear();
        surface.nodeLayer().getChildren().clear();
    }

    private void normalizeCellOrder() {
        List<StringCellView> ordered =
                cells.entrySet().stream()
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

    private CompletionStage<Void> playAsync(List<Animation> transitions) {
        List<Animation> actual = transitions.stream().filter(java.util.Objects::nonNull).toList();
        if (actual.isEmpty()) return CompletableFuture.completedFuture(null);
        CompletableFuture<Void> future = new CompletableFuture<>();
        ParallelTransition parallel = new ParallelTransition();
        parallel.getChildren().addAll(actual);
        activeAnimations.add(parallel);
        parallel.setOnFinished(
                event -> {
                    activeAnimations.remove(parallel);
                    future.complete(null);
                });
        parallel.play();
        return future;
    }

    private void cleanupExitingCells() {
        for (StringCellView cell : List.copyOf(exitingCells)) {
            settledPositions.remove(cell);
            surface.nodeLayer().getChildren().remove(cell);
            cell.setOpacity(1.0d);
            cell.setTranslateY(0.0d);
        }
        exitingCells.clear();
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
        cleanupExitingCells();
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
    public ViewportSnapshot viewportSnapshot() {
        return surface.viewportSnapshot();
    }

    @Override
    public CameraState cameraState() {
        return surface.cameraState();
    }

    @Override
    public void applyCameraState(CameraState cameraState) {
        surface.applyCameraState(cameraState);
    }

    @Override
    public boolean userControlledCamera() {
        return surface.isUserViewportChanged();
    }

    @Override
    public void prepareInitialFrame() {
        surface.markViewportPristine();
    }

    @Override
    public void revealFrame() {
        surface.setWorldVisible(true);
    }

    @Override
    public void setViewportListener(Consumer<ViewportSnapshot> listener) {
        surface.setViewportListener(listener);
    }

    @Override
    public void onModuleAttached(String moduleId) {
        renderFramework.activateSession(SESSION_ID);
        super.onModuleAttached(moduleId);
    }

    @Override
    public void onModuleDetached(String moduleId) {
        renderFramework.deactivateSession(SESSION_ID);
        super.onModuleDetached(moduleId);
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
        cells.clear();
        settledPositions.clear();
        exitingCells.clear();
        surface.nodeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
        observationLabel.setText("");
        settledPatternX = 0.0d;
        settledPatternY = 0.0d;
        selectedIndex = -1;
        pendingSelectedIndex = -1;
        lastRenderedValue = "";
        lastSubmittedState = null;
        pendingLayout = PendingLayout.empty();
        firstRender = true;
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        stopActiveAnimation();
        renderFramework.unregisterSurface(SESSION_ID, this);
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
        if (fallback > 0.0d) {
            return fallback;
        } else {
            return 1.0d;
        }
    }

    private static double quantize(double value) {
        return Math.rint(value * 100.0d) / 100.0d;
    }

    private record PendingLayout(
            List<Animation> immediateTransitions, Set<StringCellView> enteringCells) {
        private PendingLayout {
            immediateTransitions = List.copyOf(immediateTransitions);
            enteringCells = Set.copyOf(enteringCells);
        }

        private static PendingLayout empty() {
            return new PendingLayout(List.of(), Set.of());
        }
    }
}
