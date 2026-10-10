package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.animation.api.AnimationControl;
import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.runtime.StructureAnimationRuntime;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.runtime.linked.LinearStructureViewState;
import com.majortom.algorithms.visualization.impl.visualizer.linear.LinearNodeSupport;
import com.majortom.algorithms.visualization.impl.visualizer.linear.LinearStructureLayoutSpecs;
import com.majortom.algorithms.visualization.impl.visualizer.linear.animation.LinearAnimationSceneAdapter;
import com.majortom.algorithms.visualization.impl.visualizer.linear.animation.LinearStructureAnimationPlanner;
import com.majortom.algorithms.visualization.impl.visualizer.semantic.LinearStructureVisualization;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.IntConsumer;
import javafx.scene.text.Text;

/** Logical LIFO visualization: a vertical stack whose first value is TOP. */
public final class StackVisualizer extends BaseVisualizer<LinearStructureViewState> {
    private static final RenderSessionId SESSION_ID = RenderSessionId.of("STACK");
    private static final StructureVisualization<LinearStructureViewState> STRUCTURE_VISUALIZATION =
            new LinearStructureVisualization(LinearStructureLayoutSpecs.STACK);

    private final VisualizationSurface surface = new VisualizationSurface();
    private final LinearNodeSupport nodes =
            new LinearNodeSupport(surface, LinearStructureLayoutSpecs.STACK, "stack-item");
    private final StructureAnimationRuntime<LinearStructureViewState> animationRuntime =
            new StructureAnimationRuntime<>(new LinearStructureAnimationPlanner(StructureIds.STACK));
    private final LinearAnimationSceneAdapter animationScene =
            new LinearAnimationSceneAdapter(StructureIds.STACK, surface, nodes);
    private final Text topLabel = new Text();

    public StackVisualizer() {
        installSurface(surface, new javafx.geometry.Insets(24.0d, 16.0d, 62.0d, 16.0d));
        topLabel.getStyleClass().addAll("linear-role-label", "stack-top-label");
        topLabel.textProperty().bind(I18N.createStringBinding("label.visual.stack.top"));
        surface.decorationLayer().getChildren().add(topLabel);
    }

    @Override
    public RenderSessionId sessionId() {
        return SESSION_ID;
    }

    @Override
    public CompletionStage<Void> commitLayout(
            LinearStructureViewState state, LayoutResult patch, RenderCommitContext context) {
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
        positionTopLabel(patch);

        animationRuntime.play(plan, animationScene, context.presentationProgress()::publish);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> commitPresentation(
            LinearStructureViewState state, RenderCommitContext context) {
        nodes.applyPendingSelection(state.values().size());
        nodes.reconcile(state.values());
        applyPresentation(state);
        return CompletableFuture.completedFuture(null);
    }

    private void applyModelIdentity(LinearStructureViewState.Mutation mutation) {
        if (mutation == null) {
            return;
        }

        if (mutation.type() == LinearStructureViewState.Type.PUSH) {
            nodes.shiftIndexes(0, nodes.size() - 1, 1);
        } else if (mutation.type() == LinearStructureViewState.Type.POP) {
            if (!animationScene.exitDetached(0)) {
                nodes.remove(0);
            }
            nodes.shiftIndexes(1, nodes.size(), -1);
        }
    }

    private void applyPresentation(LinearStructureViewState state) {
        for (int index = 0; index < state.values().size(); index++) {
            NodeView item = nodes.get(index);
            if (item == null) {
                continue;
            }
            item.setText(state.values().get(index).text());
            item.setHighlighted(
                    index == 0 && state.mutation().type() != LinearStructureViewState.Type.NONE);
            item.setSelected(nodes.isSelected(index));
        }
    }

    private void positionTopLabel(LayoutResult patch) {
        ElementGeometry top = patch.elements().get(id(0));
        if (top == null) {
            topLabel.relocate(48.0d, 40.0d);
        } else {
            topLabel.relocate(
                    Math.max(2.0d, top.x() - 52.0d),
                    top.y() + top.height() / 2.0d - 8.0d);
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
        surface.decorationLayer().getChildren().setAll(topLabel);
        surface.reset();
        surface.markViewportPristine();
    }

    private static String id(int index) {
        return LinearStructureVisualization.elementId(StructureIds.STACK, index);
    }
}
