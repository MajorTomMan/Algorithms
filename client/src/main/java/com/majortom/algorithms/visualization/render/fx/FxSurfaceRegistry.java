package com.majortom.algorithms.visualization.render.fx;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Accessed only through FxExecutor. */
public final class FxSurfaceRegistry {
  private final Map<RenderSessionId, FxSurfaceAdapter<?>> adapters = new HashMap<>();

  public <S> void register(RenderSessionId id, FxSurfaceAdapter<S> adapter) {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(adapter, "adapter");
    FxSurfaceAdapter<?> previous = adapters.put(id, adapter);
    if (previous != null && previous != adapter)
      previous.setViewportListener(null);
  }

  public void unregister(RenderSessionId id, FxSurfaceAdapter<?> adapter) {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(adapter, "adapter");
    if (adapters.get(id) == adapter)
      adapters.remove(id);
    adapter.setViewportListener(null);
  }

  @SuppressWarnings("unchecked")
  public <S> FxSurfaceAdapter<S> require(RenderSessionId id) {
    FxSurfaceAdapter<?> adapter = adapters.get(id);
    if (adapter == null)
      throw new IllegalStateException("No FX surface registered for " + id);
    return (FxSurfaceAdapter<S>) adapter;
  }
}
