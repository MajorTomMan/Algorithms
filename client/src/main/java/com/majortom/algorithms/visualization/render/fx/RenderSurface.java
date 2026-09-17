package com.majortom.algorithms.visualization.render.fx;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import java.util.Objects;

/** Immutable binding between structure semantics, FX renderer and viewport adapter. */
public record RenderSurface<S>(
        RenderSessionId sessionId,
        StructureVisualization<S> visualization,
        FxStructureRenderer<S> renderer,
        FxSurfaceAdapter fxSurface) {
    public RenderSurface {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(visualization, "visualization");
        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(fxSurface, "fxSurface");
    }
}
