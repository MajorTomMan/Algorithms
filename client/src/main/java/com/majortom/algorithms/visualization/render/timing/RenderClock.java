package com.majortom.algorithms.visualization.render.timing;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class RenderClock implements RenderTimer, AutoCloseable {
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(
                    task -> {
                        Thread thread = new Thread(task, "render-clock");
                        thread.setDaemon(true);
                        return thread;
                    });

    @Override
    public void schedule(Duration delay, Runnable task) {
        executor.schedule(task, Math.max(0L, delay.toMillis()), TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
