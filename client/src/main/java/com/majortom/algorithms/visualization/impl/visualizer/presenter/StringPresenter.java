package com.majortom.algorithms.visualization.impl.visualizer.presenter;

import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.api.StructurePresenter;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;

public final class StringPresenter implements StructurePresenter<StringViewState> {
    @Override
    public RenderIntent present(RenderSessionId sessionId, StringViewState previous, StringViewState current) {
        boolean coldOrRevisit = previous == null;
        boolean replacement = sourceReplacement(previous, current);
        boolean initial = coldOrRevisit || replacement;
        boolean structural = initial || !previous.value().equals(current.value());
        if (!structural) return new PresentationRenderIntent<>(sessionId, current);
        CameraPolicy cameraPolicy = replacement
                ? CameraPolicy.FIT_IF_READABLE
                : (coldOrRevisit ? CameraPolicy.RESTORE_OR_FIT_IF_READABLE
                        : CameraPolicy.ENSURE_VISIBLE_IF_READABLE);
        return new StructuralRenderIntent<>(sessionId, current, cameraPolicy, initial);
    }

    private static boolean sourceReplacement(StringViewState previous, StringViewState current) {
        return previous != null
                && current.mutation().type() == StringViewState.Type.NONE
                && !current.completed()
                && !current.value().equals(previous.value());
    }
}
