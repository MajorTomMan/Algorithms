package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;

public record RenderResult(RenderStatus status, RenderSessionId sessionId, long revision,
    boolean layoutChanged, BoundsSnapshot contentBounds, Throwable error) {
  public RenderResult {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(sessionId, "sessionId");
    contentBounds = contentBounds == null ? BoundsSnapshot.empty() : contentBounds;
  }
  public static RenderResult superseded(RenderSessionId id) {
    return new RenderResult(RenderStatus.SUPERSEDED, id, -1L, false, BoundsSnapshot.empty(), null);
  }
  public static RenderResult cancelled(RenderSessionId id) {
    return new RenderResult(RenderStatus.CANCELLED, id, -1L, false, BoundsSnapshot.empty(), null);
  }
}
