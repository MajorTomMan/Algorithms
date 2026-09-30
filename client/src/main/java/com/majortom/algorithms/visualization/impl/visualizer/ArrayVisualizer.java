package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.animation.api.AnimationControl;
import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.fx.AnimationSceneAdapter;
import com.majortom.algorithms.visualization.animation.runtime.StructureAnimationRuntime;
import com.majortom.algorithms.visualization.impl.visualizer.semantic.ArrayStructureVisualization;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.VisualDensityPolicy;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.impl.visualizer.array.ArrayCellView;
import com.majortom.algorithms.visualization.impl.visualizer.array.ArrayVisualIds;
import com.majortom.algorithms.visualization.impl.visualizer.array.animation.ArrayAnimationIds;
import com.majortom.algorithms.visualization.impl.visualizer.array.animation.ArrayAnimationPlanner;
import com.majortom.algorithms.visualization.impl.visualizer.indexed.IndexedStripAnimationSupport;
import com.majortom.algorithms.visualization.impl.visualizer.indexed.IndexedStripSupport;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
import javafx.geometry.Point2D;
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

/** Array reference implementation with pure capture and authoritative FX commit. */
public final class ArrayVisualizer extends BaseVisualizer<ArrayViewState> {
    private static final RenderSessionId SESSION_ID = RenderSessionId.of("ARRAY");
    private static final StructureVisualization<ArrayViewState> STRUCTURE_VISUALIZATION = new ArrayStructureVisualization();
    private static final double EMPTY_X = 36.0d;
    private static final double EMPTY_Y = 64.0d;

    private final VisualizationSurface surface = new VisualizationSurface();
    private final IndexedStripSupport<ArrayCellView> strip = new IndexedStripSupport<>(surface);
    private final StructureAnimationRuntime<ArrayViewState> animationRuntime =
            new StructureAnimationRuntime<>(new ArrayAnimationPlanner());
    private final ArrayAnimationSceneAdapter animationScene = new ArrayAnimationSceneAdapter();
    private final Text emptyLabel = new Text();
    private List<VisualValue> lastRenderedValues = List.of();

    public ArrayVisualizer() {
        installSurface(surface);
        emptyLabel.getStyleClass().add("visual-empty-label");
        emptyLabel.textProperty().bind(I18N.createStringBinding("label.visual.array.empty"));
    }

    @Override
    public RenderSessionId sessionId() { return SESSION_ID; }


    public void setOnIndexSelected(IntConsumer onIndexSelected) {
        strip.setSelectionListener(onIndexSelected);
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

    public boolean showSelection(int index) {
        return strip.showSelection(index);
    }


    @Override
    public CompletionStage<Void> commitLayout(
            ArrayViewState state, LayoutPatch patch, RenderCommitContext context) {
        int previousCellCount = strip.size();
        boolean animate = context.modelChange() && !context.initialFrame();
        AnimationPlan plan = animationRuntime.beginTransition(state, patch, animate);
        animationScene.prepare(plan, state, patch);

        if (context.modelChange()) {
            if (context.initialFrame() && sourceReplacement(lastRenderedValues, state)) {
                clearCells();
            } else {
                applyModelIdentity(state.mutation(), state.values().size(), previousCellCount);
            }
        }
        reconcileCells(state);
        strip.applyPendingSelection(state.values().size());
        applyPresentation(state);

        strip.applyLayout(patch, ArrayVisualizer::id);
        updateEmptyLabel(state.values().isEmpty());
        lastRenderedValues = state.values();
        animationRuntime.play(plan, animationScene, context.presentationProgress()::publish);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> commitPresentation(ArrayViewState state, RenderCommitContext context) {
        strip.applyPendingSelection(state.values().size());
        reconcileCells(state);
        applyPresentation(state);
        updateEmptyLabel(state.values().isEmpty());
        lastRenderedValues = state.values();
        return CompletableFuture.completedFuture(null);
    }

    private void applyModelIdentity(ArrayViewState.Mutation mutation, int newSize, int oldSize) {
        if (mutation == null) return;
        switch (mutation.type()) {
            case INSERTED -> {
                int index = mutation.index();
                if (newSize == oldSize + 1 && index >= 0 && index <= oldSize) {
                    strip.shiftIndexes(index, oldSize - 1, 1);
                }
            }
            case REMOVED -> {
                int index = mutation.index();
                if (newSize + 1 == oldSize && index >= 0 && index < oldSize) {
                    // The animation adapter may already have detached the removed cell as an exit visual.
                    strip.remove(index);
                    strip.shiftIndexes(index + 1, oldSize - 1, -1);
                }
            }
            case SWAPPED -> {
                int left = mutation.index();
                int right = mutation.otherIndex();
                if (oldSize == newSize && left >= 0 && left < oldSize && right >= 0 && right < oldSize
                        && left != right) {
                    strip.swap(left, right);
                }
            }
            case UPDATED, NONE -> {
                // Index identity remains stable. Presentation applies the new factual values.
            }
        }
    }

    private void reconcileCells(ArrayViewState state) {
        strip.reconcile(state.values().size(),
                index -> new ArrayCellView(index, state.values().get(index).text()));
        strip.normalizeOrder(animationScene.exitingCells());
    }

    private void applyPresentation(ArrayViewState state) {
        VisualDensity density = VisualDensityPolicy.array(state.values().size());
        for (int index = 0; index < state.values().size(); index++) {
            ArrayCellView cell = strip.get(index);
            if (cell == null) continue;
            applyCellPresentation(cell, state, index, density);
        }
        strip.normalizeOrder(animationScene.exitingCells());
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
        cell.setSelected(strip.isSelected(index));
        cell.setDensity(density, mutationIndex || observationIndex || strip.isSelected(index));
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

    private void clearCells() {
        strip.clearCells();
    }

    @Override
    protected AnimationControl animationControl() {
        return animationRuntime;
    }

    @Override
    public StructureVisualization<ArrayViewState> structureVisualization() {
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
        strip.resetSelection();
        lastRenderedValues = List.of();
        surface.reset();
        surface.markViewportPristine();
    }


    private final class ArrayAnimationSceneAdapter implements AnimationSceneAdapter {
        private final IndexedStripAnimationSupport<ArrayCellView> support =
                new IndexedStripAnimationSupport<>(
                        strip,
                        surface,
                        ArrayAnimationIds::node,
                        ArrayVisualizer::id,
                        ArrayVisualIds::nodeIndex,
                        ArrayAnimationIds::exitIndex);

        void prepare(AnimationPlan plan, ArrayViewState state, LayoutPatch patch) {
            support.begin(patch);
            captureCentersForMutation(state.mutation(), state.values().size());
            for (var timed : plan.steps()) {
                if (timed.step() instanceof AnimationStep.NodeExit exit) {
                    support.detachExit(exit.targetId());
                }
            }
        }

        private void captureCentersForMutation(ArrayViewState.Mutation mutation, int newSize) {
            for (Map.Entry<Integer, ArrayCellView> entry : strip.cells().entrySet()) {
                int source = entry.getKey();
                String logicalId = switch (mutation.type()) {
                    case INSERTED -> ArrayAnimationIds.node(
                            source >= mutation.index() ? source + 1 : source);
                    case REMOVED -> source == mutation.index()
                            ? ArrayAnimationIds.exit(source)
                            : ArrayAnimationIds.node(source > mutation.index() ? source - 1 : source);
                    case SWAPPED -> ArrayAnimationIds.node(source == mutation.index()
                            ? mutation.otherIndex()
                            : source == mutation.otherIndex() ? mutation.index() : source);
                    case UPDATED -> ArrayAnimationIds.node(source);
                    case NONE -> source < newSize
                            ? ArrayAnimationIds.node(source)
                            : ArrayAnimationIds.exit(source);
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

    private static boolean sourceReplacement(ArrayViewState previous, ArrayViewState current) {
        return previous != null
                && current.mutation().type() == ArrayViewState.Type.NONE
                && !current.completed()
                && !current.values().equals(previous.values());
    }

    private static boolean sourceReplacement(List<VisualValue> previousValues, ArrayViewState current) {
        return !previousValues.isEmpty()
                && current.mutation().type() == ArrayViewState.Type.NONE
                && !current.completed()
                && !current.values().equals(previousValues);
    }

    private boolean isMutationIndex(ArrayViewState.Mutation mutation, int index) {
        if (mutation == null
                || mutation.type() == ArrayViewState.Type.NONE
                || mutation.type() == ArrayViewState.Type.REMOVED) {
            return false;
        }
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


    private static String id(int index) {
        return ArrayVisualIds.node(index);
    }
}
