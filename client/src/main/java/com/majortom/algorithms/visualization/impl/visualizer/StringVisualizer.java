package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.animation.api.AnimationControl;
import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.fx.AnimationSceneAdapter;
import com.majortom.algorithms.visualization.animation.runtime.StructureAnimationRuntime;
import com.majortom.algorithms.visualization.impl.visualizer.semantic.StringStructureVisualization;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.VisualDensityPolicy;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.impl.visualizer.string.KmpPatternCellView;
import com.majortom.algorithms.visualization.impl.visualizer.string.StringCellView;
import com.majortom.algorithms.visualization.impl.visualizer.string.StringVisualIds;
import com.majortom.algorithms.visualization.impl.visualizer.string.animation.StringAnimationIds;
import com.majortom.algorithms.visualization.impl.visualizer.string.animation.StringAnimationPlanner;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.IntConsumer;

/** String visualizer with a JavaFX-neutral capture and authoritative SceneGraph commit. */
public final class StringVisualizer extends BaseVisualizer<StringViewState> {
    private static final RenderSessionId SESSION_ID = RenderSessionId.of("STRING");
    private static final StructureVisualization<StringViewState> STRUCTURE_VISUALIZATION = new StringStructureVisualization();
    private static final double EMPTY_X = 36.0d;
    private static final double EMPTY_Y = 64.0d;

    private final VisualizationSurface surface = new VisualizationSurface();
    private final Map<Integer, StringCellView> cells = new LinkedHashMap<>();
    private final StructureAnimationRuntime<StringViewState> animationRuntime =
            new StructureAnimationRuntime<>(new StringAnimationPlanner());
    private final StringAnimationSceneAdapter animationScene = new StringAnimationSceneAdapter();
    private final Text emptyLabel = new Text();
    private final Text observationLabel = new Text();
    private final Label patternCaption = new Label();
    private final HBox patternTrack = new HBox(0.0d);
    private final List<KmpPatternCellView> patternCells = new ArrayList<>();
    private String lastRenderedValue = "";
    private int selectedIndex = -1;
    private int pendingSelectedIndex = -1;
    private String algorithmPattern = "";
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
    }

    @Override
    public RenderSessionId sessionId() { return SESSION_ID; }


    public void setOnIndexSelected(IntConsumer onIndexSelected) {
        this.onIndexSelected = onIndexSelected == null ? ignored -> {} : onIndexSelected;
    }

    /** Algorithm-only KMP overlay input. The logical String track remains unchanged. */
    public void setAlgorithmPattern(String pattern) {
        String next = pattern == null ? "" : pattern;
        if (next.equals(algorithmPattern)) return;
        algorithmPattern = next;
    }

    public void clearAlgorithmPattern() {
        if (algorithmPattern.isEmpty() && patternCells.isEmpty()) return;
        algorithmPattern = "";
    }

    public void clearSelection() {
        selectedIndex = -1;
        pendingSelectedIndex = -1;
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
        selectedIndex = index;
        pendingSelectedIndex = cells.containsKey(index) ? -1 : index;
        return true;
    }


    @Override
    public CompletionStage<Void> commitLayout(
            StringViewState state, LayoutPatch patch, RenderCommitContext context) {
        int previousCellCount = cells.size();
        boolean animate = context.modelChange() && !context.initialFrame();
        AnimationPlan plan = animationRuntime.beginTransition(state, patch, animate);
        animationScene.prepare(plan, state, patch, previousCellCount);

        if (context.modelChange()) {
            if (context.initialFrame() && sourceReplacement(lastRenderedValue, state)) {
                clearCells();
            } else {
                applyModelIdentity(state.mutation(), state.value().length(), previousCellCount);
            }
        }
        reconcileCells(state);
        applyPendingSelection(state.value().length());
        applyPresentation(state);

        for (Map.Entry<Integer, StringCellView> entry : cells.entrySet()) {
            ElementGeometry target = patch.elements().get(id(entry.getKey()));
            if (target == null) continue;
            StringCellView cell = entry.getValue();
            cell.setLayoutSize(target.width(), target.height());
            cell.relocate(target.x(), target.y());
        }
        updateDecorations(state);
        lastRenderedValue = state.value();
        animationRuntime.play(plan, animationScene, context.presentationProgress()::publish);
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

    private void applyModelIdentity(StringViewState.Mutation mutation, int newSize, int oldSize) {
        if (mutation == null || mutation.type() == StringViewState.Type.NONE) return;
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
                    // Exit visuals may already be detached by the animation adapter.
                    removeRange(index, Math.min(oldSize, index + length));
                    shiftIndexes(index + length, oldSize - 1, -length);
                }
            }
            case REPLACED -> {
                int delta = newSize - oldSize;
                int oldLength = length - delta;
                if (oldLength >= 0 && index >= 0 && index + oldLength <= oldSize) {
                    int common = Math.min(oldLength, length);
                    if (oldLength > common) {
                        removeRange(index + common, index + oldLength);
                    }
                    if (delta != 0) {
                        shiftIndexes(index + oldLength, oldSize - 1, delta);
                    }
                    // Common replacement cells retain identity and pulse their value change.
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
        VisualDensity density = VisualDensityPolicy.string(state.value().length());
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
        List<javafx.scene.Node> ordered = new ArrayList<>(cells.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .map(javafx.scene.Node.class::cast)
                .toList());
        ordered.addAll(animationScene.exitingCells());
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
        VisualDensity density = VisualDensityPolicy.string(state.value().length());
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

    private final class StringAnimationSceneAdapter implements AnimationSceneAdapter {
        private final Map<String, Point2D> capturedCenters = new LinkedHashMap<>();
        private final Map<String, StringCellView> exitingCells = new LinkedHashMap<>();
        private LayoutPatch targetPatch;

        void prepare(AnimationPlan plan, StringViewState state, LayoutPatch patch, int oldSize) {
            capturedCenters.clear();
            targetPatch = patch;
            captureCentersForMutation(state.mutation(), oldSize, state.value().length());
            for (var timed : plan.steps()) {
                if (timed.step() instanceof AnimationStep.NodeExit exit) detachExit(exit.targetId());
            }
        }

        private void captureCentersForMutation(
                StringViewState.Mutation mutation, int oldSize, int newSize) {
            if (mutation == null) {
                cells.forEach((index, cell) -> capturedCenters.put(
                        StringAnimationIds.node(index), visualCenter(cell)));
                return;
            }
            int index = mutation.index();
            int length = Math.max(0, mutation.length());
            int delta = newSize - oldSize;
            int oldReplacementLength = mutation.type() == StringViewState.Type.REPLACED
                    ? length - delta
                    : 0;
            int commonReplacement = Math.min(Math.max(0, oldReplacementLength), length);

            for (Map.Entry<Integer, StringCellView> entry : cells.entrySet()) {
                int source = entry.getKey();
                String logicalId = switch (mutation.type()) {
                    case INSERTED -> StringAnimationIds.node(source >= index ? source + length : source);
                    case REMOVED -> source >= index && source < index + length
                            ? StringAnimationIds.exit(source)
                            : StringAnimationIds.node(source >= index + length ? source - length : source);
                    case REPLACED -> {
                        if (source >= index + commonReplacement
                                && source < index + oldReplacementLength) {
                            yield StringAnimationIds.exit(source);
                        }
                        if (source >= index + oldReplacementLength) {
                            yield StringAnimationIds.node(source + delta);
                        }
                        yield StringAnimationIds.node(source);
                    }
                    case UPDATED -> StringAnimationIds.node(source);
                    case NONE -> source < newSize
                            ? StringAnimationIds.node(source)
                            : StringAnimationIds.exit(source);
                };
                capturedCenters.put(logicalId, visualCenter(entry.getValue()));
            }
        }

        private void detachExit(String logicalId) {
            int previousIndex = StringAnimationIds.exitIndex(logicalId);
            if (previousIndex < 0) return;
            StringCellView cell = cells.remove(previousIndex);
            if (cell != null) exitingCells.put(logicalId, cell);
        }

        Collection<javafx.scene.Node> exitingCells() {
            return List.copyOf(exitingCells.values());
        }

        @Override
        public Optional<NodeTarget> node(String logicalId) {
            int exitIndex = StringAnimationIds.exitIndex(logicalId);
            if (exitIndex >= 0) {
                StringCellView exiting = exitingCells.get(logicalId);
                Point2D center = capturedCenters.get(logicalId);
                return exiting == null || center == null
                        ? Optional.empty()
                        : Optional.of(new NodeTarget(logicalId, exiting, center, List.of()));
            }
            Integer index = activeIndex(logicalId);
            if (index == null) return Optional.empty();
            StringCellView cell = cells.get(index);
            ElementGeometry geometry = targetPatch == null ? null : targetPatch.elements().get(id(index));
            if (cell == null || geometry == null) return Optional.empty();
            return Optional.of(new NodeTarget(logicalId, cell, center(geometry), List.of()));
        }

        @Override
        public Optional<EdgeTarget> edge(String logicalId) {
            return Optional.empty();
        }

        @Override
        public Optional<Point2D> capturedNodeCenter(String logicalId) {
            return Optional.ofNullable(capturedCenters.get(logicalId));
        }

        @Override
        public Optional<List<Point2D>> capturedEdgeRoute(String logicalId) {
            return Optional.empty();
        }

        @Override
        public Collection<NodeTarget> activeNodes() {
            List<NodeTarget> result = new ArrayList<>(cells.size());
            for (Map.Entry<Integer, StringCellView> entry : cells.entrySet()) {
                ElementGeometry geometry = targetPatch == null ? null : targetPatch.elements().get(id(entry.getKey()));
                if (geometry != null) {
                    result.add(new NodeTarget(StringAnimationIds.node(entry.getKey()), entry.getValue(),
                            center(geometry), List.of()));
                }
            }
            return result;
        }

        @Override
        public Collection<EdgeTarget> activeEdges() {
            return List.of();
        }

        @Override
        public void discardExitedVisuals() {
            exitingCells.values().forEach(cell -> surface.nodeLayer().getChildren().remove(cell));
            exitingCells.clear();
        }

        @Override
        public void stabilize(AnimationPlan plan) {
            for (NodeTarget target : activeNodes()) {
                javafx.scene.Node node = target.node();
                node.setTranslateX(0.0d);
                node.setTranslateY(0.0d);
                node.setOpacity(1.0d);
                node.setScaleX(1.0d);
                node.setScaleY(1.0d);
            }
            discardExitedVisuals();
            capturedCenters.clear();
        }

        private Integer activeIndex(String logicalId) {
            int index = StringVisualIds.nodeIndex(logicalId);
            return index < 0 ? null : index;
        }
    }

    private static Point2D visualCenter(javafx.scene.Node node) {
        Bounds bounds = node.getBoundsInParent();
        return new Point2D(
                bounds.getMinX() + bounds.getWidth() / 2.0d,
                bounds.getMinY() + bounds.getHeight() / 2.0d);
    }

    private static Point2D center(ElementGeometry geometry) {
        return new Point2D(
                geometry.x() + geometry.width() / 2.0d,
                geometry.y() + geometry.height() / 2.0d);
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


    @Override
    protected AnimationControl animationControl() {
        return animationRuntime;
    }

    @Override
    public StructureVisualization<StringViewState> structureVisualization() {
        return STRUCTURE_VISUALIZATION;
    }

    @Override
    public FxSurfaceAdapter fxSurfaceAdapter() {
        return surface;
    }
    @Override public void setViewportObstructionInsets(javafx.geometry.Insets insets) { surface.setObstructionInsets(insets); }
@Override
    public void onVisualizationReset() {
        super.onVisualizationReset();
        clearCells();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
        patternCells.clear();
        patternTrack.getChildren().clear();
        observationLabel.setText("");
        selectedIndex = -1;
        pendingSelectedIndex = -1;
        lastRenderedValue = "";
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
        return StringVisualIds.node(index);
    }
}
