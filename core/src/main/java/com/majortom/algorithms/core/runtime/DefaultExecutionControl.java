package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.domain.execution.RunPausedEvent;
import com.majortom.algorithms.core.domain.execution.RunResumedEvent;

import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/** Default pause/step/cancel control for one run. */
public final class DefaultExecutionControl implements RunControl {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition stateChanged = lock.newCondition();
    private boolean paused;
    private int stepPermits;
    private volatile boolean cancelled;
    private Consumer<ExecutionLifecycleEvent> lifecycleSink;

    public void pause() {
        lock.lock();
        try {
            if (cancelled || paused) {
                return;
            }
            paused = true;
            stepPermits = 0;
            emitLifecycle(new RunPausedEvent());
        } finally {
            lock.unlock();
        }
    }

    public void resume() {
        lock.lock();
        try {
            if (cancelled || !paused) {
                return;
            }
            emitLifecycle(new RunResumedEvent());
            paused = false;
            stepPermits = 0;
            stateChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /** Allows exactly one subsequent domain event through while remaining paused. */
    public void step() {
        lock.lock();
        try {
            if (cancelled || !paused) {
                return;
            }
            stepPermits++;
            stateChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void cancel() {
        lock.lock();
        try {
            cancelled = true;
            paused = false;
            stepPermits = 0;
            stateChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isCancellationRequested() {
        return cancelled;
    }

    @Override
    public void awaitPermission(CancellationToken cancellationToken) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (paused && !cancelled && !cancellationToken.isCancellationRequested()) {
                stateChanged.await();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void awaitDomainEventPermission(CancellationToken cancellationToken) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (paused && stepPermits == 0 && !cancelled && !cancellationToken.isCancellationRequested()) {
                stateChanged.await();
            }
            if (paused && stepPermits > 0) {
                stepPermits--;
            }
        } finally {
            lock.unlock();
        }
    }

    void bindLifecycle(Consumer<ExecutionLifecycleEvent> lifecycleSink) {
        lock.lock();
        try {
            this.lifecycleSink = Objects.requireNonNull(lifecycleSink, "lifecycleSink");
        } finally {
            lock.unlock();
        }
    }

    void unbindLifecycle() {
        lock.lock();
        try {
            lifecycleSink = null;
        } finally {
            lock.unlock();
        }
    }

    private void emitLifecycle(ExecutionLifecycleEvent event) {
        Consumer<ExecutionLifecycleEvent> sink = lifecycleSink;
        if (sink != null) {
            sink.accept(event);
        }
    }
}
