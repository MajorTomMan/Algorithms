package com.majortom.algorithms.visualization.render.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RenderScheduler implements AutoCloseable {
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(
                    task -> {
                        Thread thread = new Thread(task, "render-scheduler");
                        thread.setDaemon(true);
                        return thread;
                    });

    public void execute(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
