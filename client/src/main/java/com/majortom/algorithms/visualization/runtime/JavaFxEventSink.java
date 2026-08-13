package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.runtime.EventSink;
import com.majortom.algorithms.core.runtime.ExecutionEvent;
import javafx.application.Platform;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Bounded, best-effort JavaFX observation channel for authoritative execution events.
 *
 * <p>Only one drain task is submitted at a time. Producers apply back pressure when the local
 * queue is full, so a fast algorithm cannot create an unbounded {@code Platform.runLater} queue.
 * Dispatcher and observer failures are deliberately isolated from algorithm execution: the
 * authoritative sink must be invoked before this observer, and remains the source for replay.</p>
 */
public final class JavaFxEventSink implements EventSink, AutoCloseable {

    static final int DEFAULT_CAPACITY = 256;
    static final int DEFAULT_BATCH_SIZE = 64;

    private final Consumer<Runnable> dispatcher;
    private final Consumer<ExecutionEvent> consumer;
    private final int capacity;
    private final int batchSize;
    private final Object lock = new Object();
    private final Deque<ExecutionEvent> pendingEvents = new ArrayDeque<>();

    private boolean drainScheduled;
    private boolean closed;
    private RuntimeException dispatcherFailure;
    private RuntimeException observerFailure;
    private long observerFailureCount;

    public JavaFxEventSink(Consumer<ExecutionEvent> consumer) {
        this(Platform::runLater, consumer, DEFAULT_CAPACITY, DEFAULT_BATCH_SIZE);
    }

    JavaFxEventSink(Consumer<Runnable> dispatcher, Consumer<ExecutionEvent> consumer) {
        this(dispatcher, consumer, DEFAULT_CAPACITY, DEFAULT_BATCH_SIZE);
    }

    JavaFxEventSink(
            Consumer<Runnable> dispatcher,
            Consumer<ExecutionEvent> consumer,
            int capacity,
            int batchSize) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.consumer = Objects.requireNonNull(consumer, "consumer");
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        this.capacity = capacity;
        this.batchSize = batchSize;
    }

    @Override
    public void accept(ExecutionEvent event) {
        Objects.requireNonNull(event, "event");
        boolean scheduleDrain = false;
        synchronized (lock) {
            boolean queueSpaceAvailable = awaitQueueSpace();
            if (!queueSpaceAvailable || closed || dispatcherFailure != null) {
                return;
            }
            pendingEvents.addLast(event);
            if (!drainScheduled) {
                drainScheduled = true;
                scheduleDrain = true;
            }
        }
        if (scheduleDrain) {
            dispatchDrain();
        }
    }

    public Optional<RuntimeException> dispatcherFailure() {
        synchronized (lock) {
            return Optional.ofNullable(dispatcherFailure);
        }
    }

    public Optional<RuntimeException> observerFailure() {
        synchronized (lock) {
            return Optional.ofNullable(observerFailure);
        }
    }

    public long observerFailureCount() {
        synchronized (lock) {
            return observerFailureCount;
        }
    }

    int pendingEventCount() {
        synchronized (lock) {
            return pendingEvents.size();
        }
    }

    /** Waits on the producer thread until all queued observer work is consumed or disabled. */
    void awaitDrained() {
        synchronized (lock) {
            while (!closed && dispatcherFailure == null
                    && (!pendingEvents.isEmpty() || drainScheduled)) {
                try {
                    lock.wait();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            closed = true;
            pendingEvents.clear();
            lock.notifyAll();
        }
    }

    private boolean awaitQueueSpace() {
        while (pendingEvents.size() >= capacity && !closed && dispatcherFailure == null) {
            try {
                lock.wait();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    private void dispatchDrain() {
        try {
            dispatcher.accept(this::drainBatch);
        } catch (RuntimeException exception) {
            disableDispatcher(exception);
        }
    }

    private void drainBatch() {
        int drained = 0;
        while (drained < batchSize) {
            ExecutionEvent event;
            synchronized (lock) {
                if (closed || dispatcherFailure != null) {
                    drainScheduled = false;
                    pendingEvents.clear();
                    lock.notifyAll();
                    return;
                }
                event = pendingEvents.pollFirst();
                lock.notifyAll();
                if (event == null) {
                    drainScheduled = false;
                    lock.notifyAll();
                    return;
                }
            }
            notifyObserver(event);
            drained++;
        }

        boolean scheduleAnotherDrain;
        synchronized (lock) {
            scheduleAnotherDrain = !closed
                    && dispatcherFailure == null
                    && !pendingEvents.isEmpty();
            if (!scheduleAnotherDrain) {
                drainScheduled = false;
                lock.notifyAll();
            }
        }
        if (scheduleAnotherDrain) {
            dispatchDrain();
        }
    }

    private void notifyObserver(ExecutionEvent event) {
        try {
            consumer.accept(event);
        } catch (RuntimeException exception) {
            synchronized (lock) {
                observerFailure = exception;
                observerFailureCount++;
            }
        }
    }

    private void disableDispatcher(RuntimeException exception) {
        synchronized (lock) {
            dispatcherFailure = exception;
            drainScheduled = false;
            pendingEvents.clear();
            lock.notifyAll();
        }
    }
}
