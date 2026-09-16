package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;

public record PresentationRenderIntent<S>(RenderSessionId sessionId, S snapshot)
    implements RenderIntent {
  public PresentationRenderIntent {
    Objects.requireNonNull(sessionId, "sessionId");
    Objects.requireNonNull(snapshot, "snapshot");
  }
  @Override
  public RenderIntentKind kind() {
    return RenderIntentKind.PRESENTATIONAL;
  }
}
