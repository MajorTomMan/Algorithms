package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.fx.RenderSurface;
import java.util.concurrent.CompletionStage;

/** Small lifecycle boundary consumed by RenderSurfaceHost. */
public interface RenderSurfaceLifecycle {
  <S> CompletionStage<Void> register(RenderSurface<S> surface);
  CompletionStage<Void> unregister(RenderSurface<?> surface);
  CompletionStage<Void> activate(RenderSessionId sessionId);
  CompletionStage<Void> deactivate(RenderSessionId sessionId);
  CompletionStage<Void> disposeOnFx(Runnable dispose);
}
