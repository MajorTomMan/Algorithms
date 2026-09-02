package com.majortom.algorithms.core.runtime;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Small runtime-owned execution scheduler. Presentation schedulers remain outside core. */
public final class ExecutionScheduler implements AutoCloseable {

    private final ExecutorService executor;

    private ExecutionScheduler(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public static ExecutionScheduler single(String threadPrefix) {
        AtomicLong sequence = new AtomicLong();
        return new ExecutionScheduler(Executors.newSingleThreadExecutor(runnable -> daemonThread(runnable, threadPrefix + sequence.incrementAndGet())));
    }

    public static ExecutionScheduler bounded(String threadPrefix, int corePoolSize, int maximumPoolSize, int queueCapacity) {
        if (corePoolSize <= 0 || maximumPoolSize < corePoolSize || queueCapacity <= 0) {
            throw new IllegalArgumentException("Invalid scheduler bounds");
        }
        AtomicLong sequence = new AtomicLong();
        ExecutorService executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity), runnable -> daemonThread(runnable, threadPrefix + sequence.incrementAndGet()),
                new ThreadPoolExecutor.AbortPolicy());
        return new ExecutionScheduler(executor);
    }

    public Future<?> submit(Runnable task) {
        return executor.submit(Objects.requireNonNull(task, "task"));
    }

    public void execute(Runnable task) {
        executor.execute(Objects.requireNonNull(task, "task"));
    }

    public void shutdownNow() {
        executor.shutdownNow();
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private static Thread daemonThread(Runnable runnable, String name) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }
}
