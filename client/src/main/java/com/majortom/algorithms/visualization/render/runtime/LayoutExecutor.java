package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.layout.LayoutEngine;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class LayoutExecutor implements AutoCloseable {
    private final ExecutorService executor;

    public LayoutExecutor(int threads) {
        int size = Math.max(1, threads);
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory factory =
                task -> {
                    Thread thread = new Thread(task, "render-layout-" + sequence.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                };
        executor = Executors.newFixedThreadPool(size, factory);
    }

    public CompletionStage<LayoutResult> submit(LayoutEngine engine, LayoutRequest request) {
        Objects.requireNonNull(engine, "engine");
        Objects.requireNonNull(request, "request");
        return CompletableFuture.supplyAsync(() -> engine.layout(request), executor);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
