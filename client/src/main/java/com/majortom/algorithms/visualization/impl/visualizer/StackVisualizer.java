package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.RectangleGeometry;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralChange;
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

import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.beans.InvalidationListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Logical LIFO visualization: a vertical stack whose first value is TOP. */
public final class StackVisualizer extends BaseVisualizer<LinearStructureViewState>
        implements FxSurfaceAdapter<LinearStructureViewState> {
    private static final RenderSessionId SESSION_ID = RenderSessionId.of("STACK");
    private static final RectangleGeometry ITEM_GEOMETRY = new RectangleGeometry(108.0d, 48.0d);
    private static final Duration MOVE_DURATION = Duration.millis(240.0d);
    private static final Duration ENTER_DURATION = Duration.millis(190.0d);
    private static final Duration EXIT_DURATION = Duration.millis(150.0d);
    private static final double ENTRY_OFFSET = 62.0d;

    private final VisualizationSurface surface = new VisualizationSurface();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final DefaultRenderFramework renderFramework = RenderRuntime.shared();
    private final Map<Integer, NodeView> items = new LinkedHashMap<>();
    private final List<NodeView> exitingItems = new ArrayList<>();
    private final Text topLabel = new Text();
    private boolean measuringElements;
    private final InvalidationListener elementSizeListener =
            observable -> {
                if (!measuringElements) requestGeometryRefresh();
            };

    private volatile LinearStructureViewState lastSubmittedState;
    private PendingLayout pendingLayout = PendingLayout.empty();
    private Animation activeAnimation;
    private CompletableFuture<Void> activeAnimationFuture;
    private boolean hasAppliedLayout;
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
        renderFramework.registerSurface(SESSION_ID, this);
    }

    @Override
    protected synchronized void submitFrameworkRender(LinearStructureViewState state) {
        LinearStructureViewState previous = lastSubmittedState;
        boolean initial = previous == null;
        boolean structural = initial || !previous.values().equals(state.values());
        lastSubmittedState = state;
        if (structural) {
            renderFramework.submit(
                    new StructuralRenderIntent<>(
                            SESSION_ID,
                            state,
                            initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE,
                            initial));
        } else {
            renderFramework.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
    }

    private void requestGeometryRefresh() {
        LinearStructureViewState state = currentState();
        if (state == null || !isModuleAttached() || isDisposed()) return;
        renderFramework.submit(
                new StructuralRenderIntent<>(
                        SESSION_ID,
                        state,
                        CameraPolicy.ENSURE_VISIBLE,
                        false,
                        StructuralChange.GEOMETRY));
    }

    @Override
    public LayoutRequest captureLayout(
            LinearStructureViewState state, RenderCaptureContext context) {
        stopActiveAnimation();
        normalizeViews();
        List<Animation> transitions = new ArrayList<>();
        Set<Integer> newIndexes = Set.of();
        if (!animations.isScrubbing()) newIndexes = prepareIdentity(state.mutation(), transitions);
        applyPendingSelection(state.values().size());

        for (int index = 0; index < state.values().size(); index++) {
            NodeView item = items.get(index);
            if (item == null) {
                item = createItem(index, state.values().get(index).text());
                items.put(index, item);
                surface.nodeLayer().getChildren().add(item);
                newIndexes = withIndex(newIndexes, index);
            } else {
                item.setText(state.values().get(index).text());
                installSelectionHandler(item, index);
            }
            item.setHighlighted(
                    index == 0 && state.mutation().type() != LinearStructureViewState.Type.NONE);
        }
        syncSelection();

        List<Integer> stale =
                items.keySet().stream().filter(index -> index >= state.values().size()).toList();
        for (Integer index : stale) {
            NodeView item = items.remove(index);
            if (item != null) {
                item.layoutBoundsProperty().removeListener(elementSizeListener);
                if (!exitingItems.contains(item)) surface.nodeLayer().getChildren().remove(item);
            }
        }

        List<LayoutElement> measured = measureItems(state.values().size());
        pendingLayout = new PendingLayout(transitions, newIndexes);
        return new LayoutRequest(
                context.requestId(),
                context.sessionId(),
                context.modelRevision(),
                context.geometryRevision(),
                LinearLayoutEngine.ID,
                measured,
                Map.of("structure", "stack", "direction", "DOWN", "padding", "30", "spacing", "0"));
    }

    @Override
    public CompletionStage<Void> commitLayout(
            LinearStructureViewState state, LayoutPatch patch, RenderCommitContext context) {
        PendingLayout pending = pendingLayout;
        List<Animation> transitions = new ArrayList<>(pending.transitions());
        boolean snap = context.initialFrame() || animations.isScrubbing();
        for (Map.Entry<Integer, NodeView> entry : items.entrySet()) {
            ElementGeometry bounds = patch.elements().get(id(entry.getKey()));
            if (bounds == null) continue;
            Point2D target = center(bounds);
            NodeView item = entry.getValue();
            if (snap || !hasAppliedLayout) {
                item.setCenter(target.getX(), target.getY());
                item.setOpacity(1.0d);
            } else if (pending.newIndexes().contains(entry.getKey())) {
                item.setCenter(target.getX(), target.getY() - ENTRY_OFFSET);
                item.setOpacity(0.0d);
                transitions.add(
                        animations.together(
                                animations.move(item, target, MOVE_DURATION),
                                animations.fadeIn(item, ENTER_DURATION)));
            } else if (item.center().distance(target) > 0.01d) {
                transitions.add(animations.move(item, target, MOVE_DURATION));
            }
        }
        ElementGeometry top = patch.elements().get(id(0));
        if (top == null) topLabel.relocate(48.0d, 40.0d);
        else
            topLabel.relocate(
                    Math.max(2.0d, top.x() - 52.0d), top.y() + top.height() / 2.0d - 8.0d);
        hasAppliedLayout = true;
        pendingLayout = PendingLayout.empty();
        return snap ? finishSnapped(transitions) : playAsync(transitions);
    }

    @Override
    public CompletionStage<Void> commitPresentation(
            LinearStructureViewState state, RenderCommitContext context) {
        stopActiveAnimation();
        applyPendingSelection(state.values().size());
        for (int index = 0; index < state.values().size(); index++) {
            NodeView item = items.get(index);
            if (item == null) continue;
            item.setText(state.values().get(index).text());
            item.setHighlighted(
                    index == 0 && state.mutation().type() != LinearStructureViewState.Type.NONE);
        }
        syncSelection();
        return CompletableFuture.completedFuture(null);
    }

    private NodeView createItem(int index, String text) {
        NodeView item = new NodeView(ITEM_GEOMETRY, text);
        item.getStyleClass().add("stack-item");
        item.layoutBoundsProperty().addListener(elementSizeListener);
        installSelectionHandler(item, index);
        return item;
    }

    private void installSelectionHandler(NodeView item, int index) {
        item.setOnMouseClicked(
                event -> {
                    selectedIndex = index;
                    syncSelection();
                    selectionListener.accept(index);
                    event.consume();
                });
    }

    private Set<Integer> prepareIdentity(
            LinearStructureViewState.Mutation mutation, List<Animation> transitions) {
        if (mutation.type() == LinearStructureViewState.Type.PUSH) {
            Map<Integer, NodeView> shifted = new LinkedHashMap<>();
            items.entrySet().stream()
                    .sorted(Map.Entry.<Integer, NodeView>comparingByKey().reversed())
                    .forEach(entry -> shifted.put(entry.getKey() + 1, entry.getValue()));
            items.clear();
            shifted.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(
                            entry -> {
                                items.put(entry.getKey(), entry.getValue());
                                installSelectionHandler(entry.getValue(), entry.getKey());
                            });
            return Set.of(0);
        }
        if (mutation.type() == LinearStructureViewState.Type.POP && !items.isEmpty()) {
            NodeView removed = items.remove(0);
            if (removed != null) {
                removed.layoutBoundsProperty().removeListener(elementSizeListener);
                exitingItems.add(removed);
                Point2D target = removed.center().add(0.0d, -ENTRY_OFFSET);
                Animation exit =
                        animations.together(
                                animations.move(removed, target, EXIT_DURATION),
                                animations.fadeOut(removed, EXIT_DURATION));
                exit.setOnFinished(
                        event -> {
                            surface.nodeLayer().getChildren().remove(removed);
                            exitingItems.remove(removed);
                        });
                transitions.add(exit);
            }
            Map<Integer, NodeView> shifted = new LinkedHashMap<>();
            items.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> shifted.put(entry.getKey() - 1, entry.getValue()));
            items.clear();
            shifted.forEach(
                    (index, item) -> {
                        items.put(index, item);
                        installSelectionHandler(item, index);
                    });
        }
        return Set.of();
    }

    private List<LayoutElement> measureItems(int size) {
        measuringElements = true;
        try {
            List<LayoutElement> measured = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                NodeView item = items.get(index);
                item.applyCss();
                item.autosize();
                Bounds bounds = item.getLayoutBounds();
                measured.add(
                        new LayoutElement(
                                id(index),
                                positive(bounds.getWidth(), ITEM_GEOMETRY.width()),
                                positive(bounds.getHeight(), ITEM_GEOMETRY.height())));
            }
            return List.copyOf(measured);
        } finally {
            measuringElements = false;
        }
    }

    private CompletionStage<Void> finishSnapped(List<Animation> transitions) {
        transitions.forEach(Animation::stop);
        cleanupExitingItems();
        return CompletableFuture.completedFuture(null);
    }

    private CompletionStage<Void> playAsync(List<Animation> transitions) {
        if (transitions.isEmpty()) return CompletableFuture.completedFuture(null);
        CompletableFuture<Void> future = new CompletableFuture<>();
        ParallelTransition parallel = new ParallelTransition();
        parallel.getChildren().addAll(transitions);
        activeAnimation = parallel;
        activeAnimationFuture = future;
        parallel.setOnFinished(
                event -> {
                    if (activeAnimation == parallel) activeAnimation = null;
                    if (activeAnimationFuture == future) activeAnimationFuture = null;
                    future.complete(null);
                });
        parallel.play();
        return future;
    }

    private void stopActiveAnimation() {
        if (activeAnimation != null) {
            activeAnimation.stop();
            activeAnimation = null;
        }
        if (activeAnimationFuture != null) {
            activeAnimationFuture.complete(null);
            activeAnimationFuture = null;
        }
        normalizeViews();
        cleanupExitingItems();
    }

    private void cleanupExitingItems() {
        if (exitingItems.isEmpty()) return;
        surface.nodeLayer().getChildren().removeAll(List.copyOf(exitingItems));
        exitingItems.clear();
    }

    private void normalizeViews() {
        for (NodeView item : items.values()) {
            item.setOpacity(1.0d);
            item.setScaleX(1.0d);
            item.setScaleY(1.0d);
            item.setTranslateX(0.0d);
            item.setTranslateY(0.0d);
        }
    }

    public void setSelectionListener(IntConsumer listener) {
        selectionListener = listener == null ? ignored -> {} : listener;
    }

    public void clearSelection() {
        selectedIndex = -1;
        pendingSelectedIndex = -1;
        submitCurrentPresentation();
    }

    public void selectIndex(int index) {
        if (showSelection(index)) selectionListener.accept(index);
    }

    public boolean showSelection(int index) {
        if (index < 0) return false;
        LinearStructureViewState state = currentState();
        int currentSize = state == null ? items.size() : state.values().size();
        if (index >= currentSize) return false;
        selectedIndex = index;
        pendingSelectedIndex = items.containsKey(index) ? -1 : index;
        submitCurrentPresentation();
        return true;
    }

    private void submitCurrentPresentation() {
        LinearStructureViewState state = currentState();
        if (state != null && isModuleAttached() && !isDisposed()) {
            renderFramework.submit(new PresentationRenderIntent<>(SESSION_ID, state));
        }
    }

    private void applyPendingSelection(int size) {
        if (selectedIndex >= size) selectedIndex = -1;
        if (pendingSelectedIndex < 0) return;
        if (pendingSelectedIndex < size) selectedIndex = pendingSelectedIndex;
        pendingSelectedIndex = -1;
    }

    private void syncSelection() {
        for (Map.Entry<Integer, NodeView> entry : items.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == selectedIndex);
        }
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
        items.values()
                .forEach(item -> item.layoutBoundsProperty().removeListener(elementSizeListener));
        items.clear();
        exitingItems.clear();
        surface.nodeLayer().getChildren().clear();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().setAll(topLabel);
        selectedIndex = -1;
        pendingSelectedIndex = -1;
        lastSubmittedState = null;
        pendingLayout = PendingLayout.empty();
        hasAppliedLayout = false;
        surface.reset();
        surface.decorationLayer().getChildren().setAll(topLabel);
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        stopActiveAnimation();
        items.values()
                .forEach(item -> item.layoutBoundsProperty().removeListener(elementSizeListener));
        renderFramework.unregisterSurface(SESSION_ID);
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private static Set<Integer> withIndex(Set<Integer> indexes, int index) {
        if (indexes.contains(index)) return indexes;
        HashSet<Integer> copy = new HashSet<>(indexes);
        copy.add(index);
        return Set.copyOf(copy);
    }

    private static Point2D center(ElementGeometry bounds) {
        return new Point2D(bounds.x() + bounds.width() / 2.0d, bounds.y() + bounds.height() / 2.0d);
    }

    private static double positive(double value, double fallback) {
        return value > 0.0d ? value : fallback;
    }

    private static String id(int index) {
        return "stack:" + index;
    }

    private record PendingLayout(List<Animation> transitions, Set<Integer> newIndexes) {
        private PendingLayout {
            transitions = List.copyOf(transitions);
            newIndexes = Set.copyOf(newIndexes);
        }

        private static PendingLayout empty() {
            return new PendingLayout(List.of(), Set.of());
        }
    }
}
