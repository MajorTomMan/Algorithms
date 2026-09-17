package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;

/** Immutable JavaFX-neutral input visible to the structure capture phase. */
public record RenderCaptureContext(long transactionId, RenderSessionId sessionId, long generation,
    long modelRevision, long geometryRevision, LayoutRequestId requestId, StructuralChange change,
    ContentStyleSnapshot contentStyle, boolean initialFrame) {
  public RenderCaptureContext {
    Objects.requireNonNull(sessionId, "sessionId");
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(change, "change");
    Objects.requireNonNull(contentStyle, "contentStyle");
  }

  public boolean modelChange() {
    return change == StructuralChange.MODEL;
  }

  public boolean geometryRefresh() {
    return change == StructuralChange.GEOMETRY;
  }
}
