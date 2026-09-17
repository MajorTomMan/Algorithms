package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.impl.visualizer.array.ArrayCellView;
import com.majortom.algorithms.visualization.impl.visualizer.semantic.ArrayStructureVisualization;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.fx.RenderCommitContext;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
import javafx.scene.text.Text;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.IntConsumer;

/** Passive JavaFX array renderer with stable cell identity. */
public final class ArrayVisualizer extends BaseVisualizer<ArrayViewState> {
  private static final RenderSessionId SESSION_ID = RenderSessionId.of("ARRAY");
  private static final StructureVisualization<ArrayViewState> STRUCTURE_VISUALIZATION =
      new ArrayStructureVisualization();
  private static final double EMPTY_X = 36.0d;
  private static final double EMPTY_Y = 64.0d;

  private final VisualizationSurface surface = new VisualizationSurface();
  private final Map<Integer, ArrayCellView> cells = new LinkedHashMap<>();
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
    invalidatePresentation();
  }

  public int selectedIndex() { return selectedIndex; }

  public void selectIndex(int index) {
    if (!showSelection(index)) return;
    onIndexSelected.accept(index);
  }

  public boolean showSelection(int index) {
    if (index < 0) return false;
    selectedIndex = index;
    pendingSelectedIndex = cells.containsKey(index) ? -1 : index;
    invalidatePresentation();
    return true;
  }

  @Override
  public CompletionStage<Void> commitLayout(
      ArrayViewState state, LayoutPatch patch, RenderCommitContext context) {
    if (context.modelChange()) {
      if (context.initialFrame() && sourceReplacement(lastRenderedValues, state)) {
        clearCells();
      } else {
        applyModelIdentity(state.mutation(), state.values().size());
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
      cell.setOpacity(1.0d);
      cell.setScaleX(1.0d);
      cell.setScaleY(1.0d);
    }
    updateEmptyLabel(state.values().isEmpty());
    lastRenderedValues = state.values();
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

  private void applyModelIdentity(ArrayViewState.Mutation mutation, int newSize) {
    if (mutation == null) return;
    int oldSize = cells.size();
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
          ArrayCellView removed = cells.remove(index);
          if (removed != null) surface.nodeLayer().getChildren().remove(removed);
          for (int oldIndex = index + 1; oldIndex < oldSize; oldIndex++) {
            ArrayCellView cell = cells.remove(oldIndex);
            if (cell != null) cells.put(oldIndex - 1, cell);
          }
        }
      }
      case UPDATED, SWAPPED, NONE -> {
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
    List<javafx.scene.Node> ordered = cells.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(Map.Entry::getValue)
        .map(javafx.scene.Node.class::cast)
        .toList();
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
  public StructureVisualization<ArrayViewState> structureVisualization() { return STRUCTURE_VISUALIZATION; }

  @Override
  public FxSurfaceAdapter fxSurfaceAdapter() { return surface; }

  @Override
  public void setViewportObstructionInsets(javafx.geometry.Insets insets) {
    surface.setObstructionInsets(insets);
  }

  @Override
  public void onVisualizationReset() {
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

  private static boolean sourceReplacement(List<VisualValue> previousValues, ArrayViewState current) {
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
      case COMPARED_INDEXES -> observation.firstIndex() == index || observation.secondIndex() == index;
      case COMPARED_VALUE -> observation.firstIndex() == index;
      case NONE -> false;
    };
  }

  private static VisualDensity densityFor(int size) {
    if (size <= 16) return VisualDensity.DETAIL;
    if (size <= 40) return VisualDensity.COMPACT;
    return VisualDensity.DENSE;
  }

  private static String id(int index) { return "array:" + index; }
}
