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
    private final RenderSurfaceLifecyclePort lifecycle;
    private final Map<RenderSessionId, CompletionStage<Void>> lifecycleTails = new HashMap<>();

    public RenderSurfaceHost(RenderSurfaceLifecyclePort lifecycle) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
    }

    public CompletionStage<Void> attach(RenderSurface<?> surface) {
        Objects.requireNonNull(surface, "surface");
        return serialized(surface.sessionId(), () -> register(surface)
                .thenCompose(ignored -> lifecycle.activateSession(surface.sessionId())));
    }

    /** Strict order: deactivate -> unregister -> lifecycle-owned dispose. */
    public CompletionStage<Void> detach(RenderSurface<?> surface, Runnable dispose) {
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(dispose, "dispose");
        return serialized(surface.sessionId(), () -> lifecycle.deactivateSession(surface.sessionId())
                .thenCompose(ignored -> unregister(surface))
                .thenCompose(ignored -> lifecycle.disposeSurface(dispose)));
    }

    private CompletionStage<Void> register(RenderSurface<?> surface) {
        return lifecycle.registerSurface(surface);
    }

    private CompletionStage<Void> unregister(RenderSurface<?> surface) {
        return lifecycle.unregisterSurface(surface);
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
