package com.majortom.algorithms.visualization.impl.visualizer.presenter;

import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.api.StructurePresenter;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;

public final class ArrayPresenter implements StructurePresenter<ArrayViewState> {
    @Override
    public RenderIntent present(RenderSessionId sessionId, ArrayViewState previous, ArrayViewState current) {
        boolean coldOrRevisit = previous == null;
        boolean replacement = sourceReplacement(previous, current);
        boolean initial = coldOrRevisit || replacement;
        boolean structural = initial || requiresStructuralLayout(previous, current);
        if (!structural) return new PresentationRenderIntent<>(sessionId, current);
        CameraPolicy cameraPolicy = replacement
                ? CameraPolicy.FIT_CONTENT
                : (coldOrRevisit ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE);
        return new StructuralRenderIntent<>(sessionId, current, cameraPolicy, initial);
    }

    private static boolean requiresStructuralLayout(ArrayViewState previous, ArrayViewState current) {
        if (previous == null) return true;
        if (current.values().size() != previous.values().size()) return true;
        return switch (current.mutation().type()) {
            case INSERTED, REMOVED, UPDATED -> true;
            case SWAPPED -> false;
            case NONE -> !current.values().equals(previous.values());
        };
    }

    private static boolean sourceReplacement(ArrayViewState previous, ArrayViewState current) {
        return previous != null
                && current.mutation().type() == ArrayViewState.Type.NONE
                && !current.completed()
                && !current.values().equals(previous.values());
    }
}
