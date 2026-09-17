package com.majortom.algorithms.visualization.impl.visualizer.presenter;

import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.api.StructurePresenter;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;

public final class LinearPresenter implements StructurePresenter<LinearStructureViewState> {
    @Override
    public RenderIntent present(
            RenderSessionId sessionId, LinearStructureViewState previous, LinearStructureViewState current) {
        boolean initial = previous == null;
        boolean structural = initial || !previous.values().equals(current.values());
        if (!structural) return new PresentationRenderIntent<>(sessionId, current);
        return new StructuralRenderIntent<>(sessionId, current,
                initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE, initial);
    }
}
