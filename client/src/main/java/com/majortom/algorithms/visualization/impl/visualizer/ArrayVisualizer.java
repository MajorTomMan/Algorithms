package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.impl.visualizer.array.ArrayCellView;
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
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Array reference implementation for RenderFramework.
 *
 * <p>The visualizer owns Array-specific visual semantics only. Threading, ELK scheduling,
 * stale-result rejection, revision/generation, FX commit ordering and Camera/Fit are owned by
 * RenderFramework.
 */
public final class ArrayVisualizer extends BaseVisualizer<ArrayViewState>
        implements FxSurfaceAdapter<ArrayViewState> {
    private static final RenderSessionId SESSION_ID = RenderSessionId.of("ARRAY");
    private static final double EMPTY_X = 36.0d;
    private static final double EMPTY_Y = 64.0d;
    private static final Duration MOVE_DURATION = Duration.millis(220.0d);
    private static final Duration APPEAR_DURATION = Duration.millis(180.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(150.0d);

    private final VisualizationSurface surface = new VisualizationSurface();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final DefaultRenderFramework renderFramework = RenderRuntime.shared();
    private final Map<Integer, ArrayCellView> cells = new LinkedHashMap<>();
    private final Map<ArrayCellView, Point2D> settledPositions = new LinkedHashMap<>();
    private final Map<Integer, Point2D> settledIndexPositions = new LinkedHashMap<>();
    private final Set<ArrayCellView> exitingCells = new LinkedHashSet<>();
    private final Set<Animation> activeAnimations = new LinkedHashSet<>();
    private final Text emptyLabel = new Text();

    private volatile ArrayViewState lastSubmittedState;
    private List<VisualValue> lastRenderedValues = List.of();
    private PendingLayout pendingLayout = PendingLayout.empty();
    private boolean firstRender = true;
    private int selectedIndex = -1;
    private int pendingSelectedIndex = -1;
    private IntConsumer onIndexSelected = ignored -> {};

    public ArrayVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        surface.setFrameworkManagedCamera(true);
        emptyLabel.getStyleClass().add("visual-empty-label");
        emptyLabel.textProperty().bind(I18N.createStringBinding("label.visual.array.empty"));
        renderFramework.registerSurface(SESSION_ID, this);
    }

    @Override
    protected synchronized void submitFrameworkRender(ArrayViewState state) {
        ArrayViewState previous = lastSubmittedState;
        boolean coldOrRevisit = previous == null;
        boolean replacement = sourceReplacement(previous, state);
        boolean initial = coldOrRevisit || replacement;
        boolean structural = initial || requiresStructuralLayout(previous, state);
        lastSubmittedState = state;
        if (structural) {
            // RESTORE falls back to FIT_CONTENT when no camera has ever been committed,
            // which gives cold start and Session revisit the same safe entry point.
            CameraPolicy cameraPolicy =
                    replacement
                            ? CameraPolicy.FIT_CONTENT
                            : (coldOrRevisit ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE);
            renderFramework.submit(
                    new StructuralRenderIntent<>(SESSION_ID, state, cameraPolicy, initial));
        } else {
            renderFramework.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
    }

    public void setOnIndexSelected(IntConsumer onIndexSelected) {
        this.onIndexSelected = onIndexSelected == null ? ignored -> {} : onIndexSelected;
    }

    public void clearSelection() {
        selectedIndex = -1;
        pendingSelectedIndex = -1;
        submitCurrentPresentation();
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public void selectIndex(int index) {
        if (!showSelection(index)) return;
        onIndexSelected.accept(index);
    }

    /**
     * Keeps a presentation selection on the same Array index without re-firing the user click
     * callback.
     */
    public boolean showSelection(int index) {
        if (index < 0) return false;
        ArrayViewState state = currentState();
        int currentSize = state == null ? lastRenderedValues.size() : state.values().size();
        if (index >= currentSize) return false;
        selectedIndex = index;
        pendingSelectedIndex = cells.containsKey(index) ? -1 : index;
        submitCurrentPresentation();
        return true;
    }

    private void submitCurrentPresentation() {
        ArrayViewState state = currentState();
        if (state != null && isModuleAttached() && !isDisposed()) {
            renderFramework.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
    }

    @Override
    public LayoutRequest captureLayout(ArrayViewState state, RenderCaptureContext context) {
        stopActiveAnimation();
        int size = state.values().size();
        boolean replace = !firstRender && sourceReplacement(lastRenderedValues, state);
        if (replace) {
            resetCellsForSourceReplacement();
            firstRender = true;
        }

        applyPendingSelection(size);
        List<Animation> immediateTransitions = new ArrayList<>();
        Set<ArrayCellView> enteringCells = new LinkedHashSet<>();
        Set<ArrayCellView> swappedCells = new LinkedHashSet<>();
        if (!animations.isScrubbing()) {
            prepareCellIdentityForMutation(
                    state.mutation(),
                    lastRenderedValues.size(),
                    size,
                    immediateTransitions,
                    swappedCells);
        }

        if (size == 0) {
            clearCells(immediateTransitions);
            if (!surface.decorationLayer().getChildren().contains(emptyLabel)) {
                emptyLabel.relocate(EMPTY_X, EMPTY_Y);
                surface.decorationLayer().getChildren().add(emptyLabel);
            }
            pendingLayout =
                    new PendingLayout(
                            List.copyOf(immediateTransitions),
                            Set.of(),
                            Set.of(),
                            context.initialFrame(),
                            state.values());
            return request(context, List.of());
        }

        surface.decorationLayer().getChildren().remove(emptyLabel);
        VisualDensity density = densityFor(size);
        for (int index = 0; index < size; index++) {
            ArrayCellView cell = cells.get(index);
            if (cell == null) {
                cell = createCell(index, state.values().get(index));
                cells.put(index, cell);
                surface.nodeLayer().getChildren().add(cell);
                if (!context.initialFrame()) {
                    cell.setOpacity(0.0d);
                    enteringCells.add(cell);
                }
            }
            applyCellPresentation(cell, state, index, density);
        }
        normalizeCellOrder();

        List<Integer> strayIndexes =
                cells.keySet().stream().filter(index -> index >= size).toList();
        for (Integer index : strayIndexes) {
            ArrayCellView stray = cells.remove(index);
            removeCell(stray, immediateTransitions);
        }

        List<LayoutElement> measured = measureElements(size);
        pendingLayout =
                new PendingLayout(
                        List.copyOf(immediateTransitions),
                        Set.copyOf(enteringCells),
                        Set.copyOf(swappedCells),
                        context.initialFrame(),
                        state.values());
        return request(context, measured);
    }

    @Override
    public CompletionStage<Void> commitLayout(
            ArrayViewState state, LayoutPatch patch, RenderCommitContext context) {
        PendingLayout pending = pendingLayout;
        List<Animation> transitions = new ArrayList<>(pending.immediateTransitions());
        boolean hiddenInitialLayout = context.initialFrame();
        settledIndexPositions.clear();

        for (Map.Entry<Integer, ArrayCellView> entry : cells.entrySet()) {
            ElementGeometry target = patch.elements().get(id(entry.getKey()));
            if (target == null) continue;
            ArrayCellView cell = entry.getValue();
            Point2D point = new Point2D(target.x(), target.y());
            settledIndexPositions.put(entry.getKey(), point);
            settledPositions.put(cell, point);
            if (hiddenInitialLayout || animations.isScrubbing()) {
                snapTo(cell, target.x(), target.y());
                cell.setOpacity(1.0d);
            } else if (pending.enteringCells().contains(cell)) {
                snapTo(cell, target.x(), target.y() + 20.0d);
                transitions.add(moveAndFade(cell, target.x(), target.y(), APPEAR_DURATION));
            } else if (pending.swappedCells().contains(cell)) {
                transitions.add(swapArc(cell, target.x(), target.y(), MOVE_DURATION));
            } else if (distance(cell.getLayoutX(), cell.getLayoutY(), target.x(), target.y())
                    > 0.5d) {
                transitions.add(move(cell, target.x(), target.y(), MOVE_DURATION));
            } else {
                snapTo(cell, target.x(), target.y());
            }
        }

        firstRender = false;
        lastRenderedValues = state.values();
        pendingLayout = PendingLayout.empty();
        return playAsync(transitions);
    }

    @Override
    public CompletionStage<Void> commitPresentation(
            ArrayViewState state, RenderCommitContext context) {
        stopActiveAnimation();
        int size = state.values().size();
        if (selectedIndex >= size) selectedIndex = -1;
        applyPendingSelection(size);

        List<Animation> transitions = new ArrayList<>();
        Set<ArrayCellView> swappedCells = new LinkedHashSet<>();
        if (!animations.isScrubbing() && state.mutation().type() == ArrayViewState.Type.SWAPPED) {
            prepareCellIdentityForMutation(
                    state.mutation(), lastRenderedValues.size(), size, transitions, swappedCells);
        }

        VisualDensity density = densityFor(size);
        for (int index = 0; index < size; index++) {
            ArrayCellView cell = cells.get(index);
            if (cell == null) continue;
            applyCellPresentation(cell, state, index, density);
            if (swappedCells.contains(cell)) {
                Point2D target = settledIndexPositions.get(index);
                if (target != null) {
                    settledPositions.put(cell, target);
                    transitions.add(swapArc(cell, target.getX(), target.getY(), MOVE_DURATION));
                }
            }
        }
        normalizeCellOrder();
        lastRenderedValues = state.values();
        return playAsync(transitions);
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
    public void onVisualizationReset() {
        stopActiveAnimation();
        cells.clear();
        settledPositions.clear();
        settledIndexPositions.clear();
        exitingCells.clear();
        selectedIndex = -1;
        pendingSelectedIndex = -1;
        lastRenderedValues = List.of();
        lastSubmittedState = null;
        pendingLayout = PendingLayout.empty();
        surface.nodeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
        firstRender = true;
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        stopActiveAnimation();
        settledPositions.clear();
        settledIndexPositions.clear();
        exitingCells.clear();
        renderFramework.unregisterSurface(SESSION_ID);
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private LayoutRequest request(RenderCaptureContext context, List<LayoutElement> elements) {
        return new LayoutRequest(
                context.requestId(),
                context.sessionId(),
                context.modelRevision(),
                context.geometryRevision(),
                LinearLayoutEngine.ID,
                elements,
                Map.of("structure", "array"));
    }

    private ArrayCellView createCell(int index, VisualValue value) {
        ArrayCellView cell = new ArrayCellView(index, value.text());
        cell.setSelectionHandler(this::selectIndex);
        return cell;
    }

    private void applyCellPresentation(
            ArrayCellView cell, ArrayViewState state, int index, VisualDensity density) {
        cell.setIndex(index);
        cell.setValue(state.values().get(index).text());
        cell.setStripPosition(index, state.values().size());
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

    private boolean prepareCellIdentityForMutation(
            ArrayViewState.Mutation mutation,
            int oldSize,
            int newSize,
            List<Animation> transitions,
            Set<ArrayCellView> swappedCells) {
        if (firstRender
                || mutation == null
                || mutation.type() == ArrayViewState.Type.NONE
                || cells.isEmpty()) return false;
        switch (mutation.type()) {
            case INSERTED -> {
                int index = mutation.index();
                if (newSize == oldSize + 1 && index >= 0 && index <= oldSize) {
                    for (int oldIndex = oldSize - 1; oldIndex >= index; oldIndex--) {
                        ArrayCellView cell = cells.remove(oldIndex);
                        if (cell != null) cells.put(oldIndex + 1, cell);
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
                        if (cell != null) cells.put(oldIndex - 1, cell);
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
            case UPDATED, NONE -> {
                return false;
            }
        }
        return false;
    }

    private Animation removalAnimation(ArrayCellView cell) {
        Duration duration = animations.effectiveDuration(DISAPPEAR_DURATION);
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            surface.nodeLayer().getChildren().remove(cell);
            exitingCells.remove(cell);
            settledPositions.remove(cell);
            return new javafx.animation.PauseTransition(Duration.ZERO);
        }
        FadeTransition fade = new FadeTransition(duration, cell);
        fade.setToValue(0.0d);
        Timeline lift =
                new Timeline(
                        new KeyFrame(duration, new KeyValue(cell.translateYProperty(), -18.0d)));
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
            ArrayCellView cell = cells.get(index);
            cell.applyCss();
            cell.autosize();
            Bounds bounds = cell.getLayoutBounds();
            double width = positive(bounds.getWidth(), cell.prefWidth(-1.0d));
            double height = positive(bounds.getHeight(), cell.prefHeight(-1.0d));
            measured.add(new LayoutElement(id(index), quantize(width), quantize(height)));
        }
        return List.copyOf(measured);
    }

    private Animation move(ArrayCellView cell, double x, double y, Duration baseDuration) {
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

    private Animation moveAndFade(ArrayCellView cell, double x, double y, Duration baseDuration) {
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
                new KeyFrame(
                        Duration.ZERO,
                        new KeyValue(cell.layoutXProperty(), startX),
                        new KeyValue(cell.layoutYProperty(), startY),
                        new KeyValue(cell.translateYProperty(), 0.0d)),
                new KeyFrame(
                        duration.multiply(0.5d),
                        new KeyValue(cell.layoutXProperty(), (startX + x) / 2.0d),
                        new KeyValue(cell.layoutYProperty(), (startY + y) / 2.0d),
                        new KeyValue(cell.translateYProperty(), arc)),
                new KeyFrame(
                        duration,
                        new KeyValue(cell.layoutXProperty(), x),
                        new KeyValue(cell.layoutYProperty(), y),
                        new KeyValue(cell.translateYProperty(), 0.0d)));
    }

    private void clearCells(List<Animation> transitions) {
        if (firstRender) {
            cells.clear();
            surface.nodeLayer().getChildren().clear();
            return;
        }
        for (ArrayCellView cell : List.copyOf(cells.values())) removeCell(cell, transitions);
        cells.clear();
        settledIndexPositions.clear();
    }

    private void removeCell(ArrayCellView cell, List<Animation> transitions) {
        if (cell == null) return;
        if (firstRender || animations.isScrubbing()) {
            settledPositions.remove(cell);
            surface.nodeLayer().getChildren().remove(cell);
            return;
        }
        Animation fade = animations.fadeOut(cell, DISAPPEAR_DURATION);
        exitingCells.add(cell);
        fade.setOnFinished(
                event -> {
                    exitingCells.remove(cell);
                    settledPositions.remove(cell);
                    surface.nodeLayer().getChildren().remove(cell);
                    cell.setOpacity(1.0d);
                });
        transitions.add(fade);
    }

    private void resetCellsForSourceReplacement() {
        stopActiveAnimation();
        cells.clear();
        settledPositions.clear();
        settledIndexPositions.clear();
        exitingCells.clear();
        surface.nodeLayer().getChildren().clear();
    }

    private void normalizeCellOrder() {
        List<Map.Entry<Integer, ArrayCellView>> entries = new ArrayList<>(cells.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        cells.clear();
        for (Map.Entry<Integer, ArrayCellView> entry : entries)
            cells.put(entry.getKey(), entry.getValue());
        List<ArrayCellView> visualOrder =
                cells.values().stream()
                        .sorted(Comparator.comparingInt(ArrayCellView::index))
                        .toList();
        surface.nodeLayer().getChildren().removeAll(visualOrder);
        surface.nodeLayer().getChildren().addAll(visualOrder);
    }

    private CompletionStage<Void> playAsync(List<Animation> transitions) {
        if (transitions.isEmpty()) return CompletableFuture.completedFuture(null);
        CompletableFuture<Void> future = new CompletableFuture<>();
        ParallelTransition parallel = new ParallelTransition();
        parallel.getChildren().addAll(transitions);
        activeAnimations.add(parallel);
        parallel.setOnFinished(
                event -> {
                    activeAnimations.remove(parallel);
                    future.complete(null);
                });
        parallel.play();
        return future;
    }

    private void stopActiveAnimation() {
        for (Animation animation : List.copyOf(activeAnimations)) animation.stop();
        activeAnimations.clear();
        for (ArrayCellView cell : cells.values()) {
            Point2D target = settledPositions.get(cell);
            if (target != null) snapTo(cell, target.getX(), target.getY());
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

    private static boolean requiresStructuralLayout(
            ArrayViewState previous, ArrayViewState current) {
        if (previous == null) return true;
        if (current.values().size() != previous.values().size()) return true;
        return switch (current.mutation().type()) {
            case INSERTED, REMOVED, UPDATED -> true;
            case SWAPPED -> false;
            case NONE -> !current.values().equals(previous.values());
        };
    }

    private static boolean sourceReplacement(ArrayViewState previous, ArrayViewState current) {
        return previous != null
                && current.mutation().type() == ArrayViewState.Type.NONE
                && !current.completed()
                && !current.values().equals(previous.values());
    }

    private static boolean sourceReplacement(
            List<VisualValue> previousValues, ArrayViewState current) {
        return !previousValues.isEmpty()
                && current.mutation().type() == ArrayViewState.Type.NONE
                && !current.completed()
                && !current.values().equals(previousValues);
    }

    private boolean isMutationIndex(ArrayViewState.Mutation mutation, int index) {
        if (mutation == null
                || mutation.type() == ArrayViewState.Type.NONE
                || mutation.type() == ArrayViewState.Type.REMOVED) return false;
        return index == mutation.index() || index == mutation.otherIndex();
    }

    private boolean isObservationIndex(ArrayViewState.Observation observation, int index) {
        return switch (observation.type()) {
            case COMPARED_INDEXES ->
                    observation.firstIndex() == index || observation.secondIndex() == index;
            case COMPARED_VALUE -> observation.firstIndex() == index;
            case NONE -> false;
        };
    }

    private static VisualDensity densityFor(int size) {
        if (size <= 16) return VisualDensity.DETAIL;
        if (size <= 40) return VisualDensity.COMPACT;
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
        if (actual > 0.0d) return actual;
        return fallback > 0.0d ? fallback : 1.0d;
    }

    private static double quantize(double value) {
        return Math.rint(value * 100.0d) / 100.0d;
    }

    private record PendingLayout(
            List<Animation> immediateTransitions,
            Set<ArrayCellView> enteringCells,
            Set<ArrayCellView> swappedCells,
            boolean initialFrame,
            List<VisualValue> values) {
        private PendingLayout {
            immediateTransitions = List.copyOf(immediateTransitions);
            enteringCells = Set.copyOf(enteringCells);
            swappedCells = Set.copyOf(swappedCells);
            values = List.copyOf(values);
        }

        static PendingLayout empty() {
            return new PendingLayout(List.of(), Set.of(), Set.of(), false, List.of());
        }
    }
}
