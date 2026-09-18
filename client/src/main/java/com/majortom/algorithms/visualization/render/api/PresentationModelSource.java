package com.majortom.algorithms.visualization.render.api;

/**
 * JavaFX-neutral source for an immutable presentation model.
 *
 * <p>The RenderFramework invokes this on its control plane. Implementations must be fast,
 * thread-safe and must not touch JavaFX objects.</p>
 */
@FunctionalInterface
public interface PresentationModelSource<M> {
  M snapshot(PresentationSnapshotContext context);
}
