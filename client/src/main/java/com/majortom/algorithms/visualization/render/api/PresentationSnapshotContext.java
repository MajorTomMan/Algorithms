package com.majortom.algorithms.visualization.render.api;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import java.util.Objects;
import java.util.Optional;

/** Immutable control-plane context used while capturing one presentation model. */
public record PresentationSnapshotContext(
    RenderSessionId surfaceId,
    long generation,
    long presentationRevision,
    Optional<PresentationCursor> cursor,
    long cursorRevision) {
  public PresentationSnapshotContext {
    Objects.requireNonNull(surfaceId, "surfaceId");
    cursor = Objects.requireNonNull(cursor, "cursor");
    if (generation < 0L) throw new IllegalArgumentException("generation must be >= 0");
    if (presentationRevision < 0L)
      throw new IllegalArgumentException("presentationRevision must be >= 0");
    if (cursorRevision < 0L) throw new IllegalArgumentException("cursorRevision must be >= 0");
  }
}
