package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;

public record PresentationRenderIntent<S>(RenderSessionId sessionId, S snapshot, boolean coalescible)
    implements RenderIntent {
  public PresentationRenderIntent {
    Objects.requireNonNull(sessionId, "sessionId");
    Objects.requireNonNull(snapshot, "snapshot");
  }
  /** Ordered by default: event-driven animation frames must not be silently dropped. */
  public PresentationRenderIntent(RenderSessionId sessionId, S snapshot) {
    this(sessionId, snapshot, false);
  }

  @Override
  public RenderIntentKind kind() {
    return RenderIntentKind.PRESENTATIONAL;
  }
}
