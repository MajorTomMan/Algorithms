package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.animation.api.AnimationControl;
import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.fx.AnimationSceneAdapter;
import com.majortom.algorithms.visualization.animation.runtime.StructureAnimationRuntime;
import com.majortom.algorithms.visualization.impl.visualizer.semantic.ArrayStructureVisualization;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.impl.visualizer.array.ArrayCellView;
import com.majortom.algorithms.visualization.impl.visualizer.array.ArrayVisualIds;
import com.majortom.algorithms.visualization.impl.visualizer.array.animation.ArrayAnimationIds;
import com.majortom.algorithms.visualization.impl.visualizer.array.animation.ArrayAnimationPlanner;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
import javafx.geometry.Bounds;
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
    private final Map<Integer, ArrayCellView> cells = new LinkedHashMap<>();
    private final StructureAnimationRuntime<ArrayViewState> animationRuntime =
            new StructureAnimationRuntime<>(new ArrayAnimationPlanner());
    private final ArrayAnimationSceneAdapter animationScene = new ArrayAnimationSceneAdapter();
    private final Text emptyLabel = new Text();
    private List<VisualValue> lastRenderedValues = List.of();
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
    }

    @Override
    public RenderSessionId sessionId() { return SESSION_ID; }


    public void setOnIndexSelected(IntConsumer onIndexSelected) {
        this.onIndexSelected = onIndexSelected == null ? ignored -> {} : onIndexSelected;
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

    public boolean showSelection(int index) {
        if (index < 0) return false;
        selectedIndex = index;
        pendingSelectedIndex = cells.containsKey(index) ? -1 : index;
        return true;
    }


    @Override
    public CompletionStage<Void> commitLayout(
            ArrayViewState state, LayoutPatch patch, RenderCommitContext context) {
        int previousCellCount = cells.size();
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
        applyPendingSelection(state.values().size());
        applyPresentation(state);

        for (Map.Entry<Integer, ArrayCellView> entry : cells.entrySet()) {
            ElementGeometry target = patch.elements().get(id(entry.getKey()));
            if (target == null) continue;
            ArrayCellView cell = entry.getValue();
            cell.setLayoutWidth(target.width());
            cell.relocate(target.x(), target.y());
        }
        updateEmptyLabel(state.values().isEmpty());
        lastRenderedValues = state.values();
        animationRuntime.play(plan, animationScene, context.presentationProgress()::publish);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> commitPresentation(ArrayViewState state, RenderCommitContext context) {
        applyPendingSelection(state.values().size());
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
                    for (int oldIndex = oldSize - 1; oldIndex >= index; oldIndex--) {
                        ArrayCellView cell = cells.remove(oldIndex);
                        if (cell != null) cells.put(oldIndex + 1, cell);
                    }
                }
            }
            case REMOVED -> {
                int index = mutation.index();
                if (newSize + 1 == oldSize && index >= 0 && index < oldSize) {
                    // The animation adapter may already have detached the removed cell as an exit visual.
                    ArrayCellView removed = cells.remove(index);
                    if (removed != null) surface.nodeLayer().getChildren().remove(removed);
                    for (int oldIndex = index + 1; oldIndex < oldSize; oldIndex++) {
                        ArrayCellView cell = cells.remove(oldIndex);
                        if (cell != null) cells.put(oldIndex - 1, cell);
                    }
                }
            }
            case SWAPPED -> {
                int left = mutation.index();
                int right = mutation.otherIndex();
                if (oldSize == newSize && left >= 0 && left < oldSize && right >= 0 && right < oldSize
                        && left != right) {
                    ArrayCellView leftCell = cells.get(left);
                    ArrayCellView rightCell = cells.get(right);
                    if (leftCell != null && rightCell != null) {
                        cells.put(left, rightCell);
                        cells.put(right, leftCell);
                    }
                }
            }
            case UPDATED, NONE -> {
                // Index identity remains stable. Presentation applies the new factual values.
            }
        }
    }

    private void reconcileCells(ArrayViewState state) {
        int size = state.values().size();
        List<Integer> stale = cells.keySet().stream().filter(index -> index >= size).toList();
        for (Integer index : stale) {
            ArrayCellView removed = cells.remove(index);
            if (removed != null) surface.nodeLayer().getChildren().remove(removed);
        }
        for (int index = 0; index < size; index++) {
            ArrayCellView cell = cells.get(index);
            if (cell == null) {
                cell = createCell(index, state.values().get(index));
                cells.put(index, cell);
                surface.nodeLayer().getChildren().add(cell);
            }
        }
        normalizeCellOrder();
    }

    private ArrayCellView createCell(int index, VisualValue value) {
        ArrayCellView cell = new ArrayCellView(index, value.text());
        cell.setSelectionHandler(this::selectIndex);
        return cell;
    }

    private void applyPresentation(ArrayViewState state) {
        VisualDensity density = densityFor(state.values().size());
        for (int index = 0; index < state.values().size(); index++) {
            ArrayCellView cell = cells.get(index);
            if (cell == null) continue;
            applyCellPresentation(cell, state, index, density);
        }
        normalizeCellOrder();
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

    private void normalizeCellOrder() {
        List<javafx.scene.Node> ordered = new ArrayList<>(cells.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .map(javafx.scene.Node.class::cast)
                .toList());
        ordered.addAll(animationScene.exitingCells());
        surface.nodeLayer().getChildren().setAll(ordered);
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
        cells.clear();
        surface.nodeLayer().getChildren().clear();
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
    @Override public void setViewportObstructionInsets(javafx.geometry.Insets insets) { surface.setObstructionInsets(insets); }
@Override
    public void onVisualizationReset() {
        super.onVisualizationReset();
        clearCells();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().clear();
        selectedIndex = -1;
        pendingSelectedIndex = -1;
        lastRenderedValues = List.of();
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }


    private final class ArrayAnimationSceneAdapter implements AnimationSceneAdapter {
        private final Map<String, Point2D> capturedCenters = new LinkedHashMap<>();
        private final Map<String, ArrayCellView> exitingCells = new LinkedHashMap<>();
        private LayoutPatch targetPatch;

        void prepare(AnimationPlan plan, ArrayViewState state, LayoutPatch patch) {
            capturedCenters.clear();
            targetPatch = patch;
            captureCentersForMutation(state.mutation(), state.values().size());
            for (var timed : plan.steps()) {
                if (timed.step() instanceof AnimationStep.NodeExit exit) detachExit(exit.targetId());
            }
        }

        private void captureCentersForMutation(ArrayViewState.Mutation mutation, int newSize) {
            for (Map.Entry<Integer, ArrayCellView> entry : cells.entrySet()) {
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
                capturedCenters.put(logicalId, visualCenter(entry.getValue()));
            }
        }

        private void detachExit(String logicalId) {
            int previousIndex = ArrayAnimationIds.exitIndex(logicalId);
            if (previousIndex < 0) return;
            ArrayCellView cell = cells.remove(previousIndex);
            if (cell != null) exitingCells.put(logicalId, cell);
        }

        Collection<javafx.scene.Node> exitingCells() {
            return List.copyOf(exitingCells.values());
        }

        @Override
        public Optional<NodeTarget> node(String logicalId) {
            int exitIndex = ArrayAnimationIds.exitIndex(logicalId);
            if (exitIndex >= 0) {
                ArrayCellView exiting = exitingCells.get(logicalId);
                Point2D center = capturedCenters.get(logicalId);
                return exiting == null || center == null
                        ? Optional.empty()
                        : Optional.of(new NodeTarget(logicalId, exiting, center, List.of()));
            }
            Integer index = activeIndex(logicalId);
            if (index == null) return Optional.empty();
            ArrayCellView cell = cells.get(index);
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
            for (Map.Entry<Integer, ArrayCellView> entry : cells.entrySet()) {
                ElementGeometry geometry = targetPatch == null ? null : targetPatch.elements().get(id(entry.getKey()));
                if (geometry != null) {
                    result.add(new NodeTarget(ArrayAnimationIds.node(entry.getKey()), entry.getValue(),
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
            int index = ArrayVisualIds.nodeIndex(logicalId);
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

    private static VisualDensity densityFor(int size) {
        if (size <= 16) return VisualDensity.DETAIL;
        if (size <= 40) return VisualDensity.COMPACT;
        return VisualDensity.DENSE;
    }

    private static String id(int index) {
        return ArrayVisualIds.node(index);
    }
}
