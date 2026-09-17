package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.semantic.LinearStructureVisualization;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.RectangleGeometry;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.fx.RenderCommitContext;
import javafx.geometry.Point2D;
import javafx.scene.text.Text;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Logical LIFO visualization: a vertical stack whose first value is TOP. */
public final class StackVisualizer extends BaseVisualizer<LinearStructureViewState> {
    private static final RenderSessionId SESSION_ID = RenderSessionId.of("STACK");
    private static final StructureVisualization<LinearStructureViewState> STRUCTURE_VISUALIZATION = new LinearStructureVisualization("stack", "DOWN", 108.0d, 48.0d, 30.0d, 30.0d);
    private static final double ITEM_MIN_WIDTH = 108.0d;
    private static final double ITEM_HEIGHT = 48.0d;
    private static final double ITEM_HORIZONTAL_PADDING = 30.0d;

    private final VisualizationSurface surface = new VisualizationSurface();
    private final Map<Integer, NodeView> items = new LinkedHashMap<>();
    private final Text topLabel = new Text();
    private int selectedIndex = -1;
    private int pendingSelectedIndex = -1;
    private IntConsumer selectionListener = ignored -> {};

    public StackVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        surface.setSafeInsets(new javafx.geometry.Insets(24.0d, 16.0d, 62.0d, 16.0d));
        surface.setFrameworkManagedCamera(true);
        topLabel.getStyleClass().addAll("linear-role-label", "stack-top-label");
        topLabel.textProperty().bind(I18N.createStringBinding("label.visual.stack.top"));
        surface.decorationLayer().getChildren().add(topLabel);
    }

    @Override
    public RenderSessionId sessionId() { return SESSION_ID; }



    @Override
    public CompletionStage<Void> commitLayout(
            LinearStructureViewState state, LayoutPatch patch, RenderCommitContext context) {
        if (context.modelChange()) {
            applyModelIdentity(state.mutation());
        }
        reconcileItems(state);
        applyPendingSelection(state.values().size());
        applyPresentation(state);
        for (Map.Entry<Integer, NodeView> entry : items.entrySet()) {
            ElementGeometry bounds = patch.elements().get(id(entry.getKey()));
            if (bounds == null) continue;
            NodeView item = entry.getValue();
            item.setGeometry(new RectangleGeometry(bounds.width(), bounds.height()));
            Point2D target = center(bounds);
            item.setCenter(target.getX(), target.getY());
            item.setOpacity(1.0d);
        }
        ElementGeometry top = patch.elements().get(id(0));
        if (top == null) topLabel.relocate(48.0d, 40.0d);
        else topLabel.relocate(
                Math.max(2.0d, top.x() - 52.0d), top.y() + top.height() / 2.0d - 8.0d);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> commitPresentation(
            LinearStructureViewState state, RenderCommitContext context) {
        applyPendingSelection(state.values().size());
        reconcileItems(state);
        applyPresentation(state);
        return CompletableFuture.completedFuture(null);
    }

    private void applyModelIdentity(LinearStructureViewState.Mutation mutation) {
        if (mutation == null) return;
        if (mutation.type() == LinearStructureViewState.Type.PUSH) {
            Map<Integer, NodeView> shifted = new LinkedHashMap<>();
            items.entrySet().stream()
                    .sorted(Map.Entry.<Integer, NodeView>comparingByKey().reversed())
                    .forEach(entry -> shifted.put(entry.getKey() + 1, entry.getValue()));
            items.clear();
            shifted.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> items.put(entry.getKey(), entry.getValue()));
        } else if (mutation.type() == LinearStructureViewState.Type.POP && !items.isEmpty()) {
            NodeView removed = items.remove(0);
            if (removed != null) surface.nodeLayer().getChildren().remove(removed);
            Map<Integer, NodeView> shifted = new LinkedHashMap<>();
            items.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> shifted.put(entry.getKey() - 1, entry.getValue()));
            items.clear();
            items.putAll(shifted);
        }
    }

    private void reconcileItems(LinearStructureViewState state) {
        int size = state.values().size();
        List<Integer> stale = items.keySet().stream().filter(index -> index >= size).toList();
        for (Integer index : stale) {
            NodeView removed = items.remove(index);
            if (removed != null) surface.nodeLayer().getChildren().remove(removed);
        }
        for (int index = 0; index < size; index++) {
            NodeView item = items.get(index);
            if (item == null) {
                item = createItem(index, state.values().get(index).text());
                items.put(index, item);
                surface.nodeLayer().getChildren().add(item);
            } else {
                item.setText(state.values().get(index).text());
                installSelectionHandler(item, index);
            }
        }
    }

    private NodeView createItem(int index, String text) {
        NodeView item = new NodeView(new RectangleGeometry(ITEM_MIN_WIDTH, ITEM_HEIGHT), text);
        item.getStyleClass().add("stack-item");
        installSelectionHandler(item, index);
        return item;
    }

    private void installSelectionHandler(NodeView item, int index) {
        item.setOnMouseClicked(event -> {
            selectedIndex = index;
                selectionListener.accept(index);
            event.consume();
        });
    }

    private void applyPresentation(LinearStructureViewState state) {
        for (int index = 0; index < state.values().size(); index++) {
            NodeView item = items.get(index);
            if (item == null) continue;
            item.setText(state.values().get(index).text());
            item.setHighlighted(
                    index == 0 && state.mutation().type() != LinearStructureViewState.Type.NONE);
            item.setSelected(index == selectedIndex);
        }
    }

    public void setSelectionListener(IntConsumer listener) {
        selectionListener = listener == null ? ignored -> {} : listener;
    }

    public void clearSelection() {
        selectedIndex = -1;
        pendingSelectedIndex = -1;
    }

    public void selectIndex(int index) {
        if (showSelection(index)) selectionListener.accept(index);
    }

    public boolean showSelection(int index) {
        if (index < 0) return false;
        selectedIndex = index;
        pendingSelectedIndex = items.containsKey(index) ? -1 : index;
        return true;
    }

    private void applyPendingSelection(int size) {
        if (selectedIndex >= size) selectedIndex = -1;
        if (pendingSelectedIndex < 0) return;
        if (pendingSelectedIndex < size) selectedIndex = pendingSelectedIndex;
        pendingSelectedIndex = -1;
    }

    @Override
    public StructureVisualization<LinearStructureViewState> structureVisualization() {
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
        items.clear();
        surface.nodeLayer().getChildren().clear();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().setAll(topLabel);
        selectedIndex = -1;
        pendingSelectedIndex = -1;
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private static Point2D center(ElementGeometry bounds) {
        return new Point2D(
                bounds.x() + bounds.width() / 2.0d,
                bounds.y() + bounds.height() / 2.0d);
    }

    private static String id(int index) {
        return "stack:" + index;
    }
}
