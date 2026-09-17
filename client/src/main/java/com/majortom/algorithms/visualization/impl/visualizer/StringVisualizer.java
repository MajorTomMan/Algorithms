package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
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
import com.majortom.algorithms.visualization.render.api.RenderPort;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.fx.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.fx.RenderCommitContext;
import com.majortom.algorithms.visualization.render.layout.DetachedMetrics;
import com.majortom.algorithms.visualization.render.layout.LinearLayoutEngine;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** String visualizer with a JavaFX-neutral capture and authoritative SceneGraph commit. */
public final class StringVisualizer extends BaseVisualizer<StringViewState> {
    private static final RenderSessionId SESSION_ID = RenderSessionId.of("STRING");
    private static final double EMPTY_X = 36.0d;
    private static final double EMPTY_Y = 64.0d;

    private final VisualizationSurface surface = new VisualizationSurface();
    private final RenderPort renderPort;
    private final Map<Integer, StringCellView> cells = new LinkedHashMap<>();
    private final Text emptyLabel = new Text();
    private final Text observationLabel = new Text();
    private final Label patternCaption = new Label();
    private final HBox patternTrack = new HBox(0.0d);
    private final List<KmpPatternCellView> patternCells = new ArrayList<>();

    private volatile StringViewState lastSubmittedState;
    private String lastRenderedValue = "";
    private int selectedIndex = -1;
    private int pendingSelectedIndex = -1;
    private String algorithmPattern = "";
    private IntConsumer onIndexSelected = ignored -> {};

    public StringVisualizer(RenderPort renderPort) {
        this.renderPort = java.util.Objects.requireNonNull(renderPort, "renderPort");
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
    }

    @Override
    public RenderSessionId sessionId() { return SESSION_ID; }

    @Override
    protected synchronized void submitFrameworkRender(StringViewState state) {
        StringViewState previous = lastSubmittedState;
        boolean coldOrRevisit = previous == null;
        boolean replacement = sourceReplacement(previous, state);
        boolean initial = coldOrRevisit || replacement;
        boolean structural = initial || !previous.value().equals(state.value());
        lastSubmittedState = state;
        if (structural) {
            CameraPolicy cameraPolicy = replacement
                    ? CameraPolicy.FIT_CONTENT
                    : (coldOrRevisit ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE);
            renderPort.submit(
                    new StructuralRenderIntent<>(SESSION_ID, state, cameraPolicy, initial));
        } else {
            renderPort.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
    }

    public void setOnIndexSelected(IntConsumer onIndexSelected) {
        this.onIndexSelected = onIndexSelected == null ? ignored -> {} : onIndexSelected;
    }

    /** Algorithm-only KMP overlay input. The logical String track remains unchanged. */
    public void setAlgorithmPattern(String pattern) {
        String next = pattern == null ? "" : pattern;
        if (next.equals(algorithmPattern)) return;
        algorithmPattern = next;
        submitCurrentPresentation();
    }

    public void clearAlgorithmPattern() {
        if (algorithmPattern.isEmpty() && patternCells.isEmpty()) return;
        algorithmPattern = "";
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

    public void selectIndex(int index) {
        if (!showSelection(index)) return;
        onIndexSelected.accept(index);
    }

    /** Keeps a presentation selection on the same character index without re-firing the click callback. */
    public boolean showSelection(int index) {
        if (index < 0) return false;
        StringViewState state = currentState();
        int currentSize = state == null ? lastRenderedValue.length() : state.value().length();
        if (index >= currentSize) return false;
        selectedIndex = index;
        pendingSelectedIndex = cells.containsKey(index) ? -1 : index;
        submitCurrentPresentation();
        return true;
    }

    private void submitCurrentPresentation() {
        StringViewState state = currentState();
        if (state != null && isModuleAttached() && !isDisposed()) {
            renderPort.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
    }

    @Override
    public LayoutRequest captureLayout(StringViewState state, RenderCaptureContext context) {
        int size = state.value().length();
        VisualDensity density = densityFor(size);
        double baseWidth = switch (density) {
            case DETAIL -> 52.0d;
            case COMPACT -> 40.0d;
            case DENSE -> 28.0d;
        };
        double height = Math.max(78.0d, 62.0d + context.contentStyle().fontSize());
        List<LayoutElement> elements = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            String value = Character.toString(state.value().charAt(index));
            double width = DetachedMetrics.boxWidth(value, context.contentStyle(), baseWidth, 16.0d);
            elements.add(new LayoutElement(id(index), width, height));
        }
        return new LayoutRequest(
                context.requestId(),
                context.sessionId(),
                context.modelRevision(),
                context.geometryRevision(),
                LinearLayoutEngine.ID,
                elements,
                Map.of(
                        "structure", "string",
                        "direction", "RIGHT",
                        "padding", "28",
                        "spacing", "0"));
    }

    @Override
    public CompletionStage<Void> commitLayout(
            StringViewState state, LayoutPatch patch, RenderCommitContext context) {
        if (context.modelChange()) {
            if (context.initialFrame() && sourceReplacement(lastRenderedValue, state)) {
                clearCells();
            } else {
                applyModelIdentity(state.mutation(), state.value().length());
            }
        }
        reconcileCells(state);
        applyPendingSelection(state.value().length());
        applyPresentation(state);

        for (Map.Entry<Integer, StringCellView> entry : cells.entrySet()) {
            ElementGeometry target = patch.elements().get(id(entry.getKey()));
            if (target == null) continue;
            StringCellView cell = entry.getValue();
            cell.setLayoutWidth(target.width());
            cell.relocate(target.x(), target.y());
            cell.setOpacity(1.0d);
            cell.setScaleX(1.0d);
            cell.setScaleY(1.0d);
        }
        updateDecorations(state);
        lastRenderedValue = state.value();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> commitPresentation(
            StringViewState state, RenderCommitContext context) {
        reconcileCells(state);
        applyPendingSelection(state.value().length());
        applyPresentation(state);
        updateDecorations(state);
        lastRenderedValue = state.value();
        return CompletableFuture.completedFuture(null);
    }

    private void applyModelIdentity(StringViewState.Mutation mutation, int newSize) {
        if (mutation == null || mutation.type() == StringViewState.Type.NONE || cells.isEmpty()) return;
        int oldSize = cells.size();
        int index = mutation.index();
        int length = Math.max(0, mutation.length());
        switch (mutation.type()) {
            case INSERTED -> {
                if (length > 0 && newSize == oldSize + length && index >= 0 && index <= oldSize) {
                    shiftIndexes(index, oldSize - 1, length);
                }
            }
            case REMOVED -> {
                if (length > 0 && newSize + length == oldSize && index >= 0 && index < oldSize) {
                    removeRange(index, Math.min(oldSize, index + length));
                    shiftIndexes(index + length, oldSize - 1, -length);
                }
            }
            case REPLACED -> {
                int delta = newSize - oldSize;
                int oldLength = length - delta;
                if (oldLength >= 0 && index >= 0 && index <= oldSize) {
                    if (oldLength != length) {
                        removeRange(index, Math.min(oldSize, index + oldLength));
                        shiftIndexes(index + oldLength, oldSize - 1, delta);
                    }
                    // Equal-length replacement keeps index identity; presentation updates values.
                }
            }
            case UPDATED, NONE -> {
                // Index identity remains stable.
            }
        }
    }

    private void shiftIndexes(int fromInclusive, int toInclusive, int delta) {
        if (delta == 0 || fromInclusive > toInclusive) return;
        if (delta > 0) {
            for (int oldIndex = toInclusive; oldIndex >= fromInclusive; oldIndex--) {
                StringCellView cell = cells.remove(oldIndex);
                if (cell != null) cells.put(oldIndex + delta, cell);
            }
        } else {
            for (int oldIndex = fromInclusive; oldIndex <= toInclusive; oldIndex++) {
                StringCellView cell = cells.remove(oldIndex);
                if (cell != null) cells.put(oldIndex + delta, cell);
            }
        }
    }

    private void removeRange(int fromInclusive, int toExclusive) {
        for (int index = fromInclusive; index < toExclusive; index++) {
            StringCellView removed = cells.remove(index);
            if (removed != null) surface.nodeLayer().getChildren().remove(removed);
        }
    }

    private void reconcileCells(StringViewState state) {
        int size = state.value().length();
        List<Integer> stale = cells.keySet().stream().filter(index -> index < 0 || index >= size).toList();
        for (Integer index : stale) {
            StringCellView removed = cells.remove(index);
            if (removed != null) surface.nodeLayer().getChildren().remove(removed);
        }
        for (int index = 0; index < size; index++) {
            if (cells.containsKey(index)) continue;
            StringCellView cell = new StringCellView(index, state.value().charAt(index));
            cell.setSelectionHandler(this::selectIndex);
            cells.put(index, cell);
            surface.nodeLayer().getChildren().add(cell);
        }
        normalizeCellOrder();
    }

    private void applyPresentation(StringViewState state) {
        VisualDensity density = densityFor(state.value().length());
        for (int index = 0; index < state.value().length(); index++) {
            StringCellView cell = cells.get(index);
            if (cell == null) continue;
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
        normalizeCellOrder();
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

    private void normalizeCellOrder() {
        List<javafx.scene.Node> ordered = cells.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .map(javafx.scene.Node.class::cast)
                .toList();
        surface.nodeLayer().getChildren().setAll(ordered);
    }

    private void updateDecorations(StringViewState state) {
        updateEmptyLabel(state.value().isEmpty());
        updateObservationLabel(state.observation());
        syncPatternCells();
        updatePatternOverlay(state);
    }

    private void updateEmptyLabel(boolean empty) {
        if (empty) {
            if (!surface.decorationLayer().getChildren().contains(emptyLabel)) {
                emptyLabel.relocate(EMPTY_X, EMPTY_Y);
                surface.decorationLayer().getChildren().add(emptyLabel);
            }
        } else {
            surface.decorationLayer().getChildren().remove(emptyLabel);
        }
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
        } else if (!surface.decorationLayer().getChildren().contains(observationLabel)) {
            surface.decorationLayer().getChildren().add(observationLabel);
        }
    }

    private void syncPatternCells() {
        if (algorithmPattern.length() == patternCells.size()) {
            boolean same = true;
            for (int index = 0; index < patternCells.size(); index++) {
                // Reusing cells is safe only when the text still matches.
                // KmpPatternCellView has no getter, so setValue below is cheap and deterministic.
                patternCells.get(index).setIndex(index);
                patternCells.get(index).setValue(algorithmPattern.charAt(index));
            }
            if (same) return;
        }
        patternCells.clear();
        patternTrack.getChildren().clear();
        for (int index = 0; index < algorithmPattern.length(); index++) {
            KmpPatternCellView cell = new KmpPatternCellView(index, algorithmPattern.charAt(index));
            patternCells.add(cell);
            patternTrack.getChildren().add(cell);
        }
    }

    private void updatePatternOverlay(StringViewState state) {
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
        if (first == null) return;
        double targetX = first.getLayoutX();
        StringCellView aligned = cells.get(state.patternStart());
        if (aligned != null) targetX = aligned.getLayoutX();
        double targetY = first.getLayoutY() + Math.max(1.0d, first.getHeight()) + 24.0d;
        patternCaption.applyCss();
        patternCaption.autosize();
        patternCaption.relocate(first.getLayoutX() + 32.0d, targetY - 14.0d);
        patternTrack.relocate(targetX, targetY);

        if (!observationLabel.getText().isEmpty()) {
            observationLabel.relocate(
                    first.getLayoutX(), targetY + Math.max(1.0d, patternTrack.getHeight()) + 10.0d);
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

    private void clearCells() {
        cells.clear();
        surface.nodeLayer().getChildren().clear();
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

    private static boolean sourceReplacement(StringViewState previous, StringViewState current) {
        return previous != null
                && current.mutation().type() == StringViewState.Type.NONE
                && !current.completed()
                && !current.value().equals(previous.value());
    }

    private static boolean sourceReplacement(String previousValue, StringViewState current) {
        return !previousValue.isEmpty()
                && current.mutation().type() == StringViewState.Type.NONE
                && !current.completed()
                && !current.value().equals(previousValue);
    }

    private static VisualDensity densityFor(int size) {
        if (size <= 24) return VisualDensity.DETAIL;
        if (size <= 48) return VisualDensity.COMPACT;
        return VisualDensity.DENSE;
    }

    @Override
    public FxSurfaceAdapter fxSurfaceAdapter() {
        return surface;
    }
    @Override public void setViewportObstructionInsets(javafx.geometry.Insets insets) { surface.setObstructionInsets(insets); }
@Override
    public void onVisualizationReset() {
        clearCells();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
        patternCells.clear();
        patternTrack.getChildren().clear();
        observationLabel.setText("");
        selectedIndex = -1;
        pendingSelectedIndex = -1;
        lastRenderedValue = "";
        lastSubmittedState = null;
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private static String id(int index) {
        return "string:" + index;
    }
}
