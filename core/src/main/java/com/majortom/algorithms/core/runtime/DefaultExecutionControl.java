package com.majortom.algorithms.core.runtime;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/** Default pause/cancel control; cancellation signals the pause condition without polling. */
public final class DefaultExecutionControl implements RunControl {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition stateChanged = lock.newCondition();
    private boolean paused;
    private volatile boolean cancelled;

    public void pause() {
        lock.lock();
        try {
            paused = true;
        } finally {
            lock.unlock();
        }
    }

    public void resume() {
        lock.lock();
        try {
            paused = false;
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
}
