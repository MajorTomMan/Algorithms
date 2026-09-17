package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.fx.RenderSurface;
import java.util.concurrent.CompletionStage;

/** Small lifecycle boundary consumed by RenderSurfaceHost. */
public interface RenderSurfaceLifecyclePort {
    <S> CompletionStage<Void> registerSurface(RenderSurface<S> surface);
    CompletionStage<Void> unregisterSurface(RenderSurface<?> surface);
    CompletionStage<Void> activateSession(RenderSessionId id);
    CompletionStage<Void> deactivateSession(RenderSessionId id);
    CompletionStage<Void> disposeSurface(Runnable dispose);
}
