package com.majortom.algorithms.visualization.impl.visualizer.presenter;

import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.api.StructurePresenter;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;

public final class GraphPresenter implements StructurePresenter<GraphViewState> {
    @Override
    public RenderIntent present(RenderSessionId sessionId, GraphViewState previous, GraphViewState current) {
        boolean initial = previous == null;
        if (!initial && !requiresStructuralLayout(previous, current)) {
            return new PresentationRenderIntent<>(sessionId, current);
        }
        return new StructuralRenderIntent<>(sessionId, current,
                initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE, initial);
    }

    private static boolean requiresStructuralLayout(GraphViewState previous, GraphViewState current) {
        if (previous == null || previous.directed() != current.directed() || !previous.nodes().equals(current.nodes())) {
            return true;
        }
        if (previous.edges().size() != current.edges().size()) return true;
        for (int index = 0; index < previous.edges().size(); index++) {
            GraphViewState.Edge left = previous.edges().get(index);
            GraphViewState.Edge right = current.edges().get(index);
            if (left.id() != right.id() || left.fromId() != right.fromId() || left.toId() != right.toId()) return true;
        }
        return false;
    }
}
