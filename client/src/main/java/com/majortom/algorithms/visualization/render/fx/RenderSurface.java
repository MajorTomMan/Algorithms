package com.majortom.algorithms.visualization.render.fx;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;

/** FX surface with a stable RenderSession identity; lifecycle is owned by RenderSurfaceHost. */
public interface RenderSurface<S> extends FxSurfaceAdapter<S> {
    RenderSessionId sessionId();
}
