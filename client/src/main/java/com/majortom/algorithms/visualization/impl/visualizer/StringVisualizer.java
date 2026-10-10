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
import com.majortom.algorithms.visualization.impl.visualizer.string.StringDecorationIds;
import com.majortom.algorithms.visualization.impl.visualizer.string.StringDecorationLayout;
import com.majortom.algorithms.visualization.impl.visualizer.string.StringVisualIds;
import com.majortom.algorithms.visualization.impl.visualizer.string.animation.StringAnimationIds;
import com.majortom.algorithms.visualization.impl.visualizer.string.animation.StringAnimationPlanner;
import com.majortom.algorithms.visualization.impl.visualizer.indexed.IndexedStripAnimationSupport;
import com.majortom.algorithms.visualization.impl.visualizer.indexed.IndexedStripSupport;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.DecorationGeometry;
import com.majortom.algorithms.visualization.render.api.DecorationSize;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;

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
    private final IndexedStripSupport<StringCellView> strip = new IndexedStripSupport<>(surface);
    private final StructureAnimationRuntime<StringViewState> animationRuntime =
            new StructureAnimationRuntime<>(new StringAnimationPlanner());
    private final StringAnimationSceneAdapter animationScene = new StringAnimationSceneAdapter();
    private final Text emptyLabel = new Text();
    private final Text observationLabel = new Text();
    private final Label patternCaption = new Label();
    private final HBox patternTrack = new HBox(0.0d);
    private final List<KmpPatternCellView> patternCells = new ArrayList<>();
    private final StringDecorationLayout decorationLayout = new StringDecorationLayout();
    private LayoutResult lastPatch;
    private String lastRenderedValue = "";
    private String algorithmPattern = "";

    public StringVisualizer() {
        installSurface(surface);

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
        strip.setSelectionListener(onIndexSelected);
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
        strip.clearSelection();
    }

    public int selectedIndex() {
        return strip.selectedIndex();
    }

    public void selectIndex(int index) {
        strip.selectIndex(index);
    }

    /** Keeps a presentation selection on the same character index without re-firing the click callback. */
    public boolean showSelection(int index) {
        return strip.showSelection(index);
    }


    @Override
    public CompletionStage<Void> commitLayout(
            StringViewState state, LayoutResult patch, RenderCommitContext context) {
        int previousCellCount = strip.size();
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
        strip.applyPendingSelection(state.value().length());
        applyPresentation(state);

        strip.applyLayout(patch, StringVisualizer::id);
        lastPatch = patch;
        updateDecorations(state, patch);
        lastRenderedValue = state.value();
        animationRuntime.play(plan, animationScene, context.presentationProgress()::publish);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> commitPresentation(
            StringViewState state, RenderCommitContext context) {
        reconcileCells(state);
        strip.applyPendingSelection(state.value().length());
        applyPresentation(state);
        updateDecorations(state, lastPatch);
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
                    strip.shiftIndexes(index, oldSize - 1, length);
                }
            }
            case REMOVED -> {
                if (length > 0 && newSize + length == oldSize && index >= 0 && index < oldSize) {
                    // Exit visuals may already be detached by the animation adapter.
                    strip.removeRange(index, Math.min(oldSize, index + length));
                    strip.shiftIndexes(index + length, oldSize - 1, -length);
                }
            }
            case REPLACED -> {
                int delta = newSize - oldSize;
                int oldLength = length - delta;
                if (oldLength >= 0 && index >= 0 && index + oldLength <= oldSize) {
                    int common = Math.min(oldLength, length);
                    if (oldLength > common) {
                        strip.removeRange(index + common, index + oldLength);
                    }
                    if (delta != 0) {
                        strip.shiftIndexes(index + oldLength, oldSize - 1, delta);
                    }
                    // Common replacement cells retain identity and pulse their value change.
                }
            }
            case UPDATED, NONE -> {
                // Index identity remains stable.
            }
        }
    }

    private void reconcileCells(StringViewState state) {
        strip.reconcile(state.value().length(),
                index -> new StringCellView(index, state.value().charAt(index)));
        strip.normalizeOrder(animationScene.exitingCells());
    }

    private void applyPresentation(StringViewState state) {
        VisualDensity density = VisualDensityPolicy.string(state.value().length());
        for (int index = 0; index < state.value().length(); index++) {
            StringCellView cell = strip.get(index);
            if (cell == null) continue;
            cell.setIndex(index);
            cell.setValue(state.value().charAt(index));
            cell.setTrackPosition(index, state.value().length());
            boolean mutationIndex = isMutationIndex(state.mutation(), index);
            boolean observationIndex = isObservationIndex(state.observation(), index);
            cell.setObserved((mutationIndex || observationIndex) && !state.completed());
            cell.setCompleted(state.completed());
            cell.setSelected(strip.isSelected(index));
            cell.setDensity(density, mutationIndex || observationIndex || strip.isSelected(index));
        }
        strip.normalizeOrder(animationScene.exitingCells());
    }

    private void updateDecorations(StringViewState state, LayoutResult patch) {
        updateEmptyLabel(state.value().isEmpty());
        updateObservationLabel(state.observation());
        syncPatternCells();
        updatePatternOverlay(state, patch);
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
            for (int index = 0; index < patternCells.size(); index++) {
                patternCells.get(index).setIndex(index);
                patternCells.get(index).setValue(algorithmPattern.charAt(index));
            }
            return;
        }
        patternCells.clear();
        patternTrack.getChildren().clear();
        for (int index = 0; index < algorithmPattern.length(); index++) {
            KmpPatternCellView cell = new KmpPatternCellView(index, algorithmPattern.charAt(index));
            patternCells.add(cell);
            patternTrack.getChildren().add(cell);
        }
    }

    private void updatePatternOverlay(StringViewState state, LayoutResult patch) {
        if (state == null || patch == null || state.completed()
                || algorithmPattern.isEmpty() || strip.size() == 0) {
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
        patternCaption.applyCss();
        patternCaption.autosize();
        observationLabel.applyCss();

        ElementGeometry first = patch.elements().get(id(0));
        ElementGeometry aligned = patch.elements().get(id(state.patternStart()));
        if (first == null) {
            detachPatternOverlay();
            return;
        }

        var decorations = decorationLayout.layout(new StringDecorationLayout.Input(
                first,
                aligned,
                new DecorationSize(
                        Math.max(0.0d, patternTrack.getWidth()),
                        Math.max(0.0d, patternTrack.getHeight())),
                new DecorationSize(
                        Math.max(0.0d, patternCaption.getWidth()),
                        Math.max(0.0d, patternCaption.getHeight())),
                new DecorationSize(
                        Math.max(0.0d, observationLabel.getLayoutBounds().getWidth()),
                        Math.max(0.0d, observationLabel.getLayoutBounds().getHeight())),
                !observationLabel.getText().isEmpty()));

        applyDecoration(patternTrack,
                decorations.elements().get(StringDecorationIds.PATTERN));
        applyDecoration(patternCaption,
                decorations.elements().get(StringDecorationIds.PATTERN_CAPTION));
        applyDecoration(observationLabel,
                decorations.elements().get(StringDecorationIds.OBSERVATION));
    }

    private static void applyDecoration(javafx.scene.Node node, DecorationGeometry geometry) {
        if (geometry != null) {
            node.relocate(geometry.x(), geometry.y());
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
        strip.clearCells();
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
        private final IndexedStripAnimationSupport<StringCellView> support =
                new IndexedStripAnimationSupport<>(
                        strip,
                        surface,
                        StringAnimationIds::node,
                        StringVisualizer::id,
                        StringVisualIds::nodeIndex,
                        StringAnimationIds::exitIndex);

        void prepare(AnimationPlan plan, StringViewState state, LayoutResult patch, int oldSize) {
            support.begin(patch);
            captureCentersForMutation(state.mutation(), oldSize, state.value().length());
            for (var timed : plan.steps()) {
                if (timed.step() instanceof AnimationStep.NodeExit exit) {
                    support.detachExit(exit.targetId());
                }
            }
        }

        private void captureCentersForMutation(
                StringViewState.Mutation mutation, int oldSize, int newSize) {
            if (mutation == null) {
                strip.cells().forEach((index, cell) ->
                        support.capture(StringAnimationIds.node(index), cell));
                return;
            }

            int index = mutation.index();
            int length = Math.max(0, mutation.length());
            int delta = newSize - oldSize;
            int oldReplacementLength = mutation.type() == StringViewState.Type.REPLACED
                    ? length - delta
                    : 0;
            int commonReplacement = Math.min(Math.max(0, oldReplacementLength), length);

            for (Map.Entry<Integer, StringCellView> entry : strip.cells().entrySet()) {
                int source = entry.getKey();
                String logicalId = switch (mutation.type()) {
                    case INSERTED -> StringAnimationIds.node(
                            source >= index ? source + length : source);
                    case REMOVED -> source >= index && source < index + length
                            ? StringAnimationIds.exit(source)
                            : StringAnimationIds.node(
                                    source >= index + length ? source - length : source);
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
                support.capture(logicalId, entry.getValue());
            }
        }

        Collection<javafx.scene.Node> exitingCells() {
            return support.exitingNodes();
        }

        @Override
        public Optional<NodeTarget> node(String logicalId) {
            return support.node(logicalId);
        }

        @Override
        public Optional<EdgeTarget> edge(String logicalId) {
            return Optional.empty();
        }

        @Override
        public Optional<Point2D> capturedNodeCenter(String logicalId) {
            return support.capturedNodeCenter(logicalId);
        }

        @Override
        public Optional<List<Point2D>> capturedEdgeRoute(String logicalId) {
            return Optional.empty();
        }

        @Override
        public Collection<NodeTarget> activeNodes() {
            return support.activeNodes();
        }

        @Override
        public Collection<EdgeTarget> activeEdges() {
            return List.of();
        }

        @Override
        public void discardExitedVisuals() {
            support.discardExitedVisuals();
        }

        @Override
        public void stabilize(AnimationPlan plan) {
            support.stabilize();
        }
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
@Override
    public void onVisualizationReset() {
        super.onVisualizationReset();
        clearCells();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
        patternCells.clear();
        patternTrack.getChildren().clear();
        observationLabel.setText("");
        strip.resetSelection();
        lastPatch = null;
        lastRenderedValue = "";
        surface.reset();
        surface.markViewportPristine();
    }

    private static String id(int index) {
        return StringVisualIds.node(index);
    }
}
