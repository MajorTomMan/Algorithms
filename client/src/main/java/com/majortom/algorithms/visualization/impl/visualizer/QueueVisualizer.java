package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.RectangleGeometry;
import com.majortom.algorithms.visualization.common.layout.ElementBounds;
import com.majortom.algorithms.visualization.common.layout.LayoutFailureReporter;
import com.majortom.algorithms.visualization.common.layout.LayoutResult;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import com.majortom.algorithms.visualization.impl.visualizer.linear.StackQueueElkLayout;
import com.majortom.algorithms.visualization.impl.visualizer.linear.StackQueueElkLayout.ElementSize;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;

/** Logical FIFO visualization: a horizontal flow lane from FRONT to REAR. */
public final class QueueVisualizer extends BaseVisualizer<LinearStructureViewState> {
    private static final RectangleGeometry ITEM_GEOMETRY = new RectangleGeometry(90.0d, 50.0d);
    private static final Duration MOVE_DURATION = Duration.millis(240.0d);
    private static final Duration ENTER_DURATION = Duration.millis(190.0d);
    private static final Duration EXIT_DURATION = Duration.millis(150.0d);
    private static final double ENTRY_OFFSET = 72.0d;

    private final VisualizationSurface surface = new VisualizationSurface();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final StackQueueElkLayout layout = new StackQueueElkLayout();
    private final ExecutorService layoutExecutor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "queue-elk-layout");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong layoutVersion = new AtomicLong();
    private final Map<Integer, NodeView> items = new LinkedHashMap<>();
    private final List<NodeView> exitingItems = new ArrayList<>();
    private final Text frontLabel = new Text("FRONT ↓");
    private final Text rearLabel = new Text("↓ REAR");
    private final Text dequeueLabel = new Text("← DEQUEUE");
    private final Text enqueueLabel = new Text("ENQUEUE ←");

    private List<ElementSize> lastLayoutInput = List.of();
    private List<Animation> pendingTransitions = List.of();
    private Set<Integer> pendingNewIndexes = Set.of();
    private long pendingVersion = -1L;
    private Animation activeAnimation;
    private boolean firstRender = true;
    private boolean hasAppliedLayout;
    private int selectedIndex = -1;
    private IntConsumer selectionListener = ignored -> { };

    public QueueVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        surface.setSafeInsets(new javafx.geometry.Insets(24.0d, 16.0d, 68.0d, 16.0d));
        frontLabel.getStyleClass().addAll("linear-role-label", "queue-front-label");
        rearLabel.getStyleClass().addAll("linear-role-label", "queue-rear-label");
        dequeueLabel.getStyleClass().addAll("linear-flow-label", "queue-dequeue-label");
        enqueueLabel.getStyleClass().addAll("linear-flow-label", "queue-enqueue-label");
        surface.decorationLayer().getChildren().addAll(frontLabel, rearLabel, dequeueLabel, enqueueLabel);
    }

    @Override
    protected void draw(LinearStructureViewState state) {
        stopActiveAnimation();
        normalizeViews();
        List<Animation> transitions = new ArrayList<>();
        Set<Integer> newIndexes = prepareIdentity(state.mutation(), transitions);

        if (firstRender) {
            surface.markViewportPristine();
        }

        for (int index = 0; index < state.values().size(); index++) {
            NodeView item = items.get(index);
            if (item == null) {
                item = new NodeView(ITEM_GEOMETRY, Integer.toString(state.values().get(index)));
                item.getStyleClass().add("queue-item");
                items.put(index, item);
                surface.nodeLayer().getChildren().add(item);
                newIndexes = withIndex(newIndexes, index);
            } else {
                item.setText(Integer.toString(state.values().get(index)));
            }
            int itemIndex = index;
            item.setOnMouseClicked(event -> {
                selectedIndex = itemIndex;
                syncSelection();
                selectionListener.accept(itemIndex);
                event.consume();
            });
            item.setHighlighted(false);
        }
        syncSelection();

        List<Integer> stale = items.keySet().stream().filter(index -> index >= state.values().size()).toList();
        for (Integer index : stale) {
            NodeView item = items.remove(index);
            if (item != null && !exitingItems.contains(item)) {
                surface.nodeLayer().getChildren().remove(item);
            }
        }

        if (state.values().isEmpty()) {
            invalidateLayout();
            positionEmptyLabels();
            play(transitions, null);
            firstRender = false;
            return;
        }

        List<ElementSize> measured = measureItems(state.values().size());
        if (!measured.equals(lastLayoutInput) || !newIndexes.isEmpty() || !transitions.isEmpty()) {
            lastLayoutInput = measured;
            scheduleLayout(measured, transitions, newIndexes);
        } else {
            play(transitions, null);
        }
        firstRender = false;
    }

    private Set<Integer> prepareIdentity(LinearStructureViewState.Mutation mutation, List<Animation> transitions) {
        if (mutation.type() == LinearStructureViewState.Type.ENQUEUE) {
            return Set.of(items.size());
        }
        if (mutation.type() == LinearStructureViewState.Type.DEQUEUE && !items.isEmpty()) {
            NodeView removed = items.remove(0);
            if (removed != null) {
                exitingItems.add(removed);
                Point2D target = removed.center().add(-ENTRY_OFFSET, 0.0d);
                Animation exit = animations.together(
                        animations.move(removed, target, EXIT_DURATION),
                        animations.fadeOut(removed, EXIT_DURATION));
                exit.setOnFinished(event -> {
                    surface.nodeLayer().getChildren().remove(removed);
                    exitingItems.remove(removed);
                });
                transitions.add(exit);
            }
            Map<Integer, NodeView> shifted = new LinkedHashMap<>();
            items.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> shifted.put(entry.getKey() - 1, entry.getValue()));
            items.clear();
            shifted.forEach(items::put);
        }
        return Set.of();
    }

    private List<ElementSize> measureItems(int size) {
        List<ElementSize> measured = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            NodeView item = items.get(index);
            item.applyCss();
            item.autosize();
            Bounds bounds = item.getLayoutBounds();
            measured.add(new ElementSize(id(index), positive(bounds.getWidth(), ITEM_GEOMETRY.width()),
                    positive(bounds.getHeight(), ITEM_GEOMETRY.height())));
        }
        return List.copyOf(measured);
    }

    private void scheduleLayout(List<ElementSize> input, List<Animation> transitions, Set<Integer> newIndexes) {
        long version = layoutVersion.incrementAndGet();
        pendingVersion = version;
        pendingTransitions = List.copyOf(transitions);
        pendingNewIndexes = Set.copyOf(newIndexes);
        layoutExecutor.execute(() -> {
            try {
                LayoutResult result = layout.layoutQueue(input);
                Platform.runLater(() -> applyLayout(version, result));
            } catch (Throwable failure) {
                Platform.runLater(() -> handleLayoutFailure(version, failure));
            }
        });
    }

    private void applyLayout(long version, LayoutResult result) {
        if (isDisposed() || version != layoutVersion.get()) {
            return;
        }
        List<Animation> transitions = new ArrayList<>();
        if (pendingVersion == version) {
            transitions.addAll(pendingTransitions);
        }
        Set<Integer> newIndexes = pendingVersion == version ? pendingNewIndexes : Set.of();

        for (Map.Entry<Integer, NodeView> entry : items.entrySet()) {
            ElementBounds bounds = result.elements().get(id(entry.getKey()));
            if (bounds == null) {
                continue;
            }
            Point2D target = center(bounds);
            NodeView item = entry.getValue();
            if (!hasAppliedLayout) {
                item.setCenter(target.getX(), target.getY());
            } else if (newIndexes.contains(entry.getKey())) {
                item.setCenter(target.getX() + ENTRY_OFFSET, target.getY());
                transitions.add(animations.together(
                        animations.move(item, target, MOVE_DURATION),
                        animations.fadeIn(item, ENTER_DURATION)));
            } else if (item.center().distance(target) > 0.01d) {
                transitions.add(animations.move(item, target, MOVE_DURATION));
            }
        }

        positionLabels(result);
        pendingVersion = -1L;
        pendingTransitions = List.of();
        pendingNewIndexes = Set.of();
        hasAppliedLayout = true;
        play(transitions, () -> surface.fitWithMinimumScale(0.78d));
    }

    private void positionLabels(LayoutResult result) {
        ElementBounds front = result.elements().get(id(0));
        ElementBounds rear = result.elements().get(id(items.size() - 1));
        if (front == null || rear == null) {
            positionEmptyLabels();
            return;
        }
        double top = Math.min(front.y(), rear.y());
        double bottom = Math.max(front.y() + front.height(), rear.y() + rear.height());
        frontLabel.relocate(front.x() + 4.0d, Math.max(2.0d, top - 28.0d));
        rearLabel.relocate(rear.x() + rear.width() - 44.0d, Math.max(2.0d, top - 28.0d));
        dequeueLabel.relocate(front.x(), bottom + 14.0d);
        enqueueLabel.relocate(rear.x() + rear.width() - 62.0d, bottom + 14.0d);
    }

    private void positionEmptyLabels() {
        frontLabel.relocate(40.0d, 30.0d);
        rearLabel.relocate(130.0d, 30.0d);
        dequeueLabel.relocate(40.0d, 86.0d);
        enqueueLabel.relocate(130.0d, 86.0d);
    }

    private void handleLayoutFailure(long version, Throwable failure) {
        if (isDisposed() || version != layoutVersion.get()) {
            return;
        }
        LayoutFailureReporter.report("Queue", failure);
        List<Animation> transitions = pendingVersion == version ? pendingTransitions : List.of();
        pendingVersion = -1L;
        pendingTransitions = List.of();
        pendingNewIndexes = Set.of();
        play(transitions, null);
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

    private void play(List<Animation> transitions, Runnable onFinished) {
        if (transitions.isEmpty()) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }
        ParallelTransition parallel = new ParallelTransition();
        parallel.getChildren().addAll(transitions);
        if (onFinished != null) {
            parallel.setOnFinished(event -> onFinished.run());
        }
        activeAnimation = parallel;
        parallel.play();
    }

    private void stopActiveAnimation() {
        if (activeAnimation != null) {
            activeAnimation.stop();
            activeAnimation = null;
        }
    }

    private void invalidateLayout() {
        layoutVersion.incrementAndGet();
        lastLayoutInput = List.of();
    }

    public void setSelectionListener(IntConsumer listener) {
        selectionListener = listener == null ? ignored -> { } : listener;
    }

    public void clearSelection() {
        selectedIndex = -1;
        syncSelection();
    }

    private void syncSelection() {
        for (Map.Entry<Integer, NodeView> entry : items.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == selectedIndex);
        }
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
        invalidateLayout();
        selectedIndex = -1;
        items.clear();
        exitingItems.clear();
        surface.nodeLayer().getChildren().clear();
        surface.edgeLayer().getChildren().clear();
        surface.decorationLayer().getChildren().setAll(frontLabel, rearLabel, dequeueLabel, enqueueLabel);
        pendingTransitions = List.of();
        pendingNewIndexes = Set.of();
        pendingVersion = -1L;
        hasAppliedLayout = false;
        firstRender = true;
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        stopActiveAnimation();
        invalidateLayout();
        layoutExecutor.shutdownNow();
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private static Set<Integer> withIndex(Set<Integer> indexes, int index) {
        if (indexes.contains(index)) {
            return indexes;
        }
        java.util.HashSet<Integer> copy = new java.util.HashSet<>(indexes);
        copy.add(index);
        return Set.copyOf(copy);
    }

    private static Point2D center(ElementBounds bounds) {
        return new Point2D(bounds.x() + bounds.width() / 2.0d, bounds.y() + bounds.height() / 2.0d);
    }

    private static double positive(double value, double fallback) {
        return value > 0.0d ? value : fallback;
    }

    private static String id(int index) {
        return "queue:" + index;
    }
}
