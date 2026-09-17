package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.RenderPort;
import java.util.Objects;

/** Dependencies injected into one workbench module. */
public record RenderContext(RenderPort renderPort, RenderSurfaceHost surfaceHost) {
    public RenderContext {
        Objects.requireNonNull(renderPort, "renderPort");
        Objects.requireNonNull(surfaceHost, "surfaceHost");
    }
}
