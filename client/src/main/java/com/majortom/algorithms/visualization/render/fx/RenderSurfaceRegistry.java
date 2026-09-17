package com.majortom.algorithms.visualization.render.fx;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** RenderSurface bindings accessed only through FxExecutor. */
public final class RenderSurfaceRegistry {
    private final Map<RenderSessionId, RenderSurface<?>> surfaces = new HashMap<>();

    public <S> void register(RenderSurface<S> surface) {
        Objects.requireNonNull(surface, "surface");
        RenderSurface<?> previous = surfaces.put(surface.sessionId(), surface);
        if (previous != null && previous != surface) {
            previous.fxSurface().setViewportListener(null);
            previous.fxSurface().setCameraCommandListener(null);
        }
    }

    public void unregister(RenderSurface<?> surface) {
        Objects.requireNonNull(surface, "surface");
        if (surface.equals(surfaces.get(surface.sessionId()))) surfaces.remove(surface.sessionId());
        surface.fxSurface().setViewportListener(null);
        surface.fxSurface().setCameraCommandListener(null);
    }

    @SuppressWarnings("unchecked")
    public <S> RenderSurface<S> require(RenderSessionId id) {
        RenderSurface<?> surface = surfaces.get(id);
        if (surface == null) throw new IllegalStateException("No render surface registered for " + id);
        return (RenderSurface<S>) surface;
    }
}
