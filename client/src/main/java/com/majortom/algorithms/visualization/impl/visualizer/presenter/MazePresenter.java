package com.majortom.algorithms.visualization.impl.visualizer.presenter;

import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.api.StructurePresenter;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;

public final class MazePresenter implements StructurePresenter<MazeViewState> {
    @Override
    public RenderIntent present(RenderSessionId sessionId, MazeViewState previous, MazeViewState current) {
        boolean initial = previous == null;
        boolean structural = initial || previous.rows() != current.rows() || previous.columns() != current.columns();
        if (!structural) return new PresentationRenderIntent<>(sessionId, current);
        return new StructuralRenderIntent<>(sessionId, current,
                initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE, initial);
    }
}
