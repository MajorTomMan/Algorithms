package com.majortom.algorithms.visualization.render.fx;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Accessed only through FxExecutor. */
public final class FxSurfaceRegistry {
    private final Map<RenderSessionId, FxSurfaceAdapter<?>> adapters = new HashMap<>();

    public <S> void register(RenderSessionId id, FxSurfaceAdapter<S> adapter) {
        adapters.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(adapter, "adapter"));
    }

    public void unregister(RenderSessionId id) {
        FxSurfaceAdapter<?> adapter = adapters.remove(id);
        if (adapter != null) adapter.setViewportListener(null);
    }

    @SuppressWarnings("unchecked")
    public <S> FxSurfaceAdapter<S> require(RenderSessionId id) {
        FxSurfaceAdapter<?> adapter = adapters.get(id);
        if (adapter == null) throw new IllegalStateException("No FX surface registered for " + id);
        return (FxSurfaceAdapter<S>) adapter;
    }
}
