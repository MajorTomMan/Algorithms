package com.majortom.algorithms.visualization.render.api;

import java.util.concurrent.CompletionStage;

/**
 * Lifecycle and invalidation boundary for non-structural presentation surfaces.
 *
 * <p>Callers publish factual models to their data source and invalidate a surface. They never mutate
 * JavaFX nodes directly. The RenderFramework owns capture, scheduling and FX commit.</p>
 */
public interface PresentationSurfacePort {
  <M> CompletionStage<Void> registerPresentationSurface(PresentationSurface<M> surface);

  CompletionStage<Void> unregisterPresentationSurface(RenderSessionId surfaceId);

  CompletionStage<Void> activatePresentationSurface(RenderSessionId surfaceId);

  CompletionStage<Void> deactivatePresentationSurface(RenderSessionId surfaceId);

  CompletionStage<RenderResult> invalidatePresentationSurface(RenderSessionId surfaceId);
}
