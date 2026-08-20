package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvent;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Interruptible event pacing that never reads JavaFX properties from the worker thread. */
final class InterruptibleEventPacer {

    private final LongSupplier delayMillisSupplier;
    private final Object lock = new Object();

    private boolean paused;
    private boolean cancelled;
    private long pacingElapsedNanos;

    InterruptibleEventPacer(LongSupplier delayMillisSupplier) {
        this.delayMillisSupplier = Objects.requireNonNull(delayMillisSupplier, "delayMillisSupplier");
    }

    void awaitAfter(ExecutionEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.payload() instanceof ExecutionLifecycleEvent) {
            return;
        }
        long delayMillis = Math.max(0L, delayMillisSupplier.getAsLong());
        long remainingNanos = TimeUnit.MILLISECONDS.toNanos(delayMillis);
        long previousTick = System.nanoTime();
        synchronized (lock) {
            while (!cancelled) {
                while (paused && !cancelled) {
                    if (!waitFor(0L, 0)) {
                        return;
                    }
                    previousTick = System.nanoTime();
                }
                if (cancelled || remainingNanos <= 0L) {
                    return;
                }
                long millis = TimeUnit.NANOSECONDS.toMillis(remainingNanos);
                int nanos = (int) (remainingNanos - TimeUnit.MILLISECONDS.toNanos(millis));
                if (!waitFor(millis, nanos)) {
                    return;
                }
                long now = System.nanoTime();
                long elapsedNanos = Math.max(0L, now - previousTick);
                long consumedNanos = Math.min(elapsedNanos, remainingNanos);
                pacingElapsedNanos = Math.addExact(pacingElapsedNanos, consumedNanos);
                remainingNanos -= consumedNanos;
                previousTick = now;
            }
        }
    }

    /** Returns time spent waiting for configured event pacing, excluding pauses. */
    Duration pacingDuration() {
        synchronized (lock) {
            return Duration.ofNanos(pacingElapsedNanos);
        }
    }

    void pause() {
        synchronized (lock) {
            paused = true;
            lock.notifyAll();
        }
    }

    void resume() {
        synchronized (lock) {
            paused = false;
            lock.notifyAll();
        }
    }

    void cancel() {
        synchronized (lock) {
            cancelled = true;
            paused = false;
            lock.notifyAll();
        }
    }

    private boolean waitFor(long millis, int nanos) {
        try {
            lock.wait(millis, nanos);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
