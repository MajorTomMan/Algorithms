package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.fx.RenderSurface;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Adapts the render framework to the small lifecycle boundary used by the surface host. */
public final class DefaultRenderSurfaceLifecycle implements RenderSurfaceLifecycle {
  private final DefaultRenderFramework framework;

  public DefaultRenderSurfaceLifecycle(DefaultRenderFramework framework) {
    this.framework = Objects.requireNonNull(framework, "framework");
  }

  @Override
  public <S> CompletionStage<Void> register(RenderSurface<S> surface) {
    return framework.registerSurface(surface);
  }

  @Override
  public CompletionStage<Void> unregister(RenderSurface<?> surface) {
    return framework.unregisterSurface(surface);
  }

  @Override
  public CompletionStage<Void> activate(RenderSessionId sessionId) {
    return framework.activateSession(sessionId);
  }

  @Override
  public CompletionStage<Void> deactivate(RenderSessionId sessionId) {
    return framework.deactivateSession(sessionId);
  }

  @Override
  public CompletionStage<Void> disposeOnFx(Runnable dispose) {
    return framework.fxExecutor().execute(Objects.requireNonNull(dispose, "dispose"));
  }
}
