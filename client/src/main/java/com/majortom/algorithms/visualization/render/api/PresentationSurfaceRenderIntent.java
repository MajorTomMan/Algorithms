package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;

/** Latest-wins invalidation for a generic presentation surface. */
public record PresentationSurfaceRenderIntent(RenderSessionId sessionId) implements RenderIntent {
  public PresentationSurfaceRenderIntent {
    Objects.requireNonNull(sessionId, "sessionId");
  }

  @Override
  public RenderIntentKind kind() {
    return RenderIntentKind.PRESENTATIONAL;
  }
}
