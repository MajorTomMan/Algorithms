package com.majortom.algorithms.visualization.impl.visualizer.presenter;

import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.api.StructurePresenter;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import java.util.Objects;

public final class TreePresenter implements StructurePresenter<TreeViewState> {
    @Override
    public RenderIntent present(RenderSessionId sessionId, TreeViewState previous, TreeViewState current) {
        boolean initial = previous == null;
        boolean structural = initial || previous.kind() != current.kind()
                || !Objects.equals(previous.rootId(), current.rootId())
                || !previous.nodes().equals(current.nodes());
        if (!structural) return new PresentationRenderIntent<>(sessionId, current);
        return new StructuralRenderIntent<>(sessionId, current,
                initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE, initial);
    }
}
