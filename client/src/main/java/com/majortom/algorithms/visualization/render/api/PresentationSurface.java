package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Binding for a non-structural presentation surface.
 *
 * <p>The optional cursor session lets timeline-driven views follow an existing execution/structure
 * cursor without coupling their data source to playback or animation classes.</p>
 */
public record PresentationSurface<M>(
    RenderSessionId sessionId,
    Optional<RenderSessionId> cursorSessionId,
    PresentationModelSource<M> source,
    PresentationRenderer<M> renderer) {
  public PresentationSurface {
    Objects.requireNonNull(sessionId, "sessionId");
    cursorSessionId = Objects.requireNonNull(cursorSessionId, "cursorSessionId");
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(renderer, "renderer");
  }

  public static <M> PresentationSurface<M> standalone(
      RenderSessionId sessionId,
      PresentationModelSource<M> source,
      PresentationRenderer<M> renderer) {
    return new PresentationSurface<>(sessionId, Optional.empty(), source, renderer);
  }

  public static <M> PresentationSurface<M> followingCursor(
      RenderSessionId sessionId,
      RenderSessionId cursorSessionId,
      PresentationModelSource<M> source,
      PresentationRenderer<M> renderer) {
    return new PresentationSurface<>(
        sessionId, Optional.of(Objects.requireNonNull(cursorSessionId, "cursorSessionId")), source, renderer);
  }
}
