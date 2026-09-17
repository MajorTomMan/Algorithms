package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.fx.RenderSurface;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/** Owns surface registration and RenderSession activation/deactivation order. */
public final class RenderSurfaceHost {
    private final DefaultRenderFramework framework;
    private final Map<RenderSessionId, CompletionStage<Void>> lifecycleTails = new HashMap<>();

    public RenderSurfaceHost(DefaultRenderFramework framework) {
        this.framework = Objects.requireNonNull(framework, "framework");
    }

    public CompletionStage<Void> attach(RenderSurface<?> surface) {
        Objects.requireNonNull(surface, "surface");
        return serialized(surface.sessionId(), () -> register(surface)
                .thenCompose(ignored -> framework.activateSession(surface.sessionId())));
    }

    /** Strict order: deactivate -> unregister -> FX-thread dispose. */
    public CompletionStage<Void> detach(RenderSurface<?> surface, Runnable dispose) {
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(dispose, "dispose");
        return serialized(surface.sessionId(), () -> framework.deactivateSession(surface.sessionId())
                .thenCompose(ignored -> unregister(surface))
                .thenCompose(ignored -> framework.fxExecutor().execute(dispose)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private CompletionStage<Void> register(RenderSurface<?> surface) {
        return framework.registerSurface(surface.sessionId(), (RenderSurface) surface);
    }

    private CompletionStage<Void> unregister(RenderSurface<?> surface) {
        return framework.unregisterSurface(surface.sessionId(), surface);
    }

    private synchronized CompletionStage<Void> serialized(
            RenderSessionId id, Supplier<CompletionStage<Void>> action) {
        CompletionStage<Void> previous = lifecycleTails.getOrDefault(id, CompletableFuture.completedFuture(null));
        CompletionStage<Void> next = previous.handle((ignored, failure) -> null).thenCompose(ignored -> action.get());
        lifecycleTails.put(id, next);
        next.whenComplete((ignored, failure) -> clearTail(id, next));
        return next;
    }

    private synchronized void clearTail(RenderSessionId id, CompletionStage<Void> completed) {
        if (lifecycleTails.get(id) == completed) lifecycleTails.remove(id);
    }
}
