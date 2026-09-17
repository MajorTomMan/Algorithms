package com.majortom.algorithms.visualization.impl.visualizer.presenter;

import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.api.StructurePresenter;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;

public final class LinkedListPresenter implements StructurePresenter<LinkedListViewState> {
    @Override
    public RenderIntent present(
            RenderSessionId sessionId, LinkedListViewState previous, LinkedListViewState current) {
        boolean initial = previous == null;
        boolean structural = initial || !previous.nodes().equals(current.nodes());
        if (!structural) return new PresentationRenderIntent<>(sessionId, current);
        return new StructuralRenderIntent<>(sessionId, current,
                initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE, initial);
    }
}
