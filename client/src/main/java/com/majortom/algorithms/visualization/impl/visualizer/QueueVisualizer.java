package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.animation.api.AnimationControl;
import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.runtime.StructureAnimationRuntime;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import com.majortom.algorithms.visualization.impl.visualizer.linear.LinearNodeSupport;
import com.majortom.algorithms.visualization.impl.visualizer.linear.LinearStructureLayoutSpecs;
import com.majortom.algorithms.visualization.impl.visualizer.linear.QueueDecorationIds;
import com.majortom.algorithms.visualization.impl.visualizer.linear.QueueDecorationLayout;
import com.majortom.algorithms.visualization.impl.visualizer.linear.animation.LinearAnimationSceneAdapter;
import com.majortom.algorithms.visualization.impl.visualizer.linear.animation.LinearStructureAnimationPlanner;
import com.majortom.algorithms.visualization.impl.visualizer.semantic.LinearStructureVisualization;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.DecorationGeometry;
import com.majortom.algorithms.visualization.render.api.DecorationSize;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.IntConsumer;
import javafx.scene.text.Text;

/** Logical FIFO visualization: a horizontal flow lane from FRONT to REAR. */
public final class QueueVisualizer extends BaseVisualizer<LinearStructureViewState> {
    private static final RenderSessionId SESSION_ID = RenderSessionId.of("QUEUE");
    private static final StructureVisualization<LinearStructureViewState> STRUCTURE_VISUALIZATION =
            new LinearStructureVisualization(LinearStructureLayoutSpecs.QUEUE);

    private final VisualizationSurface surface = new VisualizationSurface();
    private final LinearNodeSupport nodes =
            new LinearNodeSupport(surface, LinearStructureLayoutSpecs.QUEUE, "queue-item");
    private final StructureAnimationRuntime<LinearStructureViewState> animationRuntime =
            new StructureAnimationRuntime<>(new LinearStructureAnimationPlanner(StructureIds.QUEUE));
    private final LinearAnimationSceneAdapter animationScene =
            new LinearAnimationSceneAdapter(StructureIds.QUEUE, surface, nodes);
    private final Text frontLabel = new Text();
    private final Text rearLabel = new Text();
    private final Text dequeueLabel = new Text();
    private final Text enqueueLabel = new Text();
    private final QueueDecorationLayout decorationLayout = new QueueDecorationLayout();
    private Map<String, ElementGeometry> lastGeometry = Map.of();
    private final javafx.beans.InvalidationListener localeListener =
            observable -> positionLabels(lastGeometry);

    public QueueVisualizer() {
        installSurface(surface, new javafx.geometry.Insets(34.0d, 16.0d, 82.0d, 16.0d));
        frontLabel.getStyleClass().addAll("linear-role-label", "queue-front-label");
        rearLabel.getStyleClass().addAll("linear-role-label", "queue-rear-label");
        dequeueLabel.getStyleClass().addAll("linear-flow-label", "queue-dequeue-label");
        enqueueLabel.getStyleClass().addAll("linear-flow-label", "queue-enqueue-label");
        dequeueLabel.textProperty().bind(I18N.createStringBinding("label.visual.queue.dequeue"));
        enqueueLabel.textProperty().bind(I18N.createStringBinding("label.visual.queue.enqueue"));
        I18N.localeProperty().addListener(localeListener);
        surface.decorationLayer().getChildren()
                .addAll(frontLabel, rearLabel, dequeueLabel, enqueueLabel);
    }

    @Override
    public RenderSessionId sessionId() {
        return SESSION_ID;
    }

    @Override
    public CompletionStage<Void> commitLayout(
            LinearStructureViewState state, LayoutPatch patch, RenderCommitContext context) {
        boolean animate = context.modelChange() && !context.initialFrame();
        AnimationPlan plan = animationRuntime.beginTransition(state, patch, animate);
        animationScene.prepare(plan, state, patch);

        if (context.modelChange()) {
            applyModelIdentity(state.mutation());
        }

        nodes.reconcile(state.values());
        nodes.applyPendingSelection(state.values().size());
        applyPresentation(state);
        nodes.applyLayout(patch);

        lastGeometry = Map.copyOf(patch.elements());
        positionLabels(lastGeometry);
        animationRuntime.play(plan, animationScene, context.presentationProgress()::publish);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> commitPresentation(
            LinearStructureViewState state, RenderCommitContext context) {
        nodes.applyPendingSelection(state.values().size());
        nodes.reconcile(state.values());
        applyPresentation(state);
        positionLabels(lastGeometry);
        return CompletableFuture.completedFuture(null);
    }

    private void applyModelIdentity(LinearStructureViewState.Mutation mutation) {
        if (mutation == null
                || mutation.type() != LinearStructureViewState.Type.DEQUEUE
                || nodes.isEmpty()) {
            return;
        }

        if (!animationScene.exitDetached(0)) {
            nodes.remove(0);
        }
        nodes.shiftIndexes(1, nodes.size(), -1);
    }

    private void applyPresentation(LinearStructureViewState state) {
        for (int index = 0; index < state.values().size(); index++) {
            NodeView item = nodes.get(index);
            if (item == null) {
                continue;
            }
            item.setText(state.values().get(index).text());
            item.setHighlighted(false);
            item.setSelected(nodes.isSelected(index));
        }
    }

    private void positionLabels(Map<String, ElementGeometry> geometry) {
        ElementGeometry front = geometry.get(id(0));
        ElementGeometry rear = geometry.get(id(nodes.size() - 1));

        if (nodes.size() == 1) {
            frontLabel.setText(I18N.text("label.visual.queue.front_rear"));
            frontLabel.setVisible(true);
            rearLabel.setVisible(false);
        } else {
            frontLabel.setText(I18N.text("label.visual.queue.front"));
            rearLabel.setText(I18N.text("label.visual.queue.rear"));
            frontLabel.setVisible(true);
            rearLabel.setVisible(true);
        }

        var decorations = decorationLayout.layout(new QueueDecorationLayout.Input(
                front,
                rear,
                nodes.size(),
                measured(frontLabel),
                measured(rearLabel),
                measuredText(I18N.text("label.visual.queue.front_rear"), frontLabel),
                measured(dequeueLabel),
                measured(enqueueLabel)));

        applyDecoration(frontLabel, decorations.elements().get(QueueDecorationIds.FRONT));
        applyDecoration(rearLabel, decorations.elements().get(QueueDecorationIds.REAR));
        applyDecoration(dequeueLabel, decorations.elements().get(QueueDecorationIds.DEQUEUE));
        applyDecoration(enqueueLabel, decorations.elements().get(QueueDecorationIds.ENQUEUE));
    }

    private static DecorationSize measured(Text label) {
        label.applyCss();
        return new DecorationSize(
                Math.max(0.0d, label.getLayoutBounds().getWidth()),
                Math.max(0.0d, label.getLayoutBounds().getHeight()));
    }

    private static DecorationSize measuredText(String text, Text prototype) {
        String previous = prototype.getText();
        prototype.setText(text);
        DecorationSize size = measured(prototype);
        prototype.setText(previous);
        return size;
    }

    private static void applyDecoration(Text label, DecorationGeometry geometry) {
        if (geometry != null) {
            label.relocate(geometry.x(), geometry.y());
        }
    }

    public void setSelectionListener(IntConsumer listener) {
        nodes.setSelectionListener(listener);
    }

    public void clearSelection() {
        nodes.clearSelection();
    }

    public void selectIndex(int index) {
        nodes.selectIndex(index);
    }

    public boolean showSelection(int index) {
        return nodes.showSelection(index);
    }

    @Override
    protected AnimationControl animationControl() {
        return animationRuntime;
    }

    @Override
    public StructureVisualization<LinearStructureViewState> structureVisualization() {
        return STRUCTURE_VISUALIZATION;
    }

    @Override
    public FxSurfaceAdapter fxSurfaceAdapter() {
        return surface;
    }

    @Override
    public void onVisualizationReset() {
        super.onVisualizationReset();
        nodes.reset();
        surface.edgeLayer().getChildren().clear();
        lastGeometry = Map.of();
        surface.reset();
        surface.decorationLayer().getChildren()
                .setAll(frontLabel, rearLabel, dequeueLabel, enqueueLabel);
        positionLabels(Map.of());
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        I18N.localeProperty().removeListener(localeListener);
        super.dispose();
    }

    private static String id(int index) {
        return LinearStructureVisualization.elementId(StructureIds.QUEUE, index);
    }
}
