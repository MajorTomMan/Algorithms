package com.majortom.algorithms.visualization.render.fx;

import com.majortom.algorithms.visualization.render.api.PresentationRenderer;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** FX-only renderer registry for generic presentation surfaces. */
public final class FxPresentationSurfaceRegistry {
  private final Map<RenderSessionId, PresentationRenderer<?>> renderers = new HashMap<>();

  public <M> void register(RenderSessionId id, PresentationRenderer<M> renderer) {
    renderers.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(renderer, "renderer"));
  }

  public void unregister(RenderSessionId id) {
    renderers.remove(Objects.requireNonNull(id, "id"));
  }

  @SuppressWarnings("unchecked")
  public <M> PresentationRenderer<M> require(RenderSessionId id) {
    PresentationRenderer<?> renderer = renderers.get(id);
    if (renderer == null) {
      throw new IllegalStateException("No presentation renderer registered for " + id);
    }
    return (PresentationRenderer<M>) renderer;
  }
}
