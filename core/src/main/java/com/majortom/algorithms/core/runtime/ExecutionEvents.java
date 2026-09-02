package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.event.ExecutionEvent;

import java.util.Objects;

public final class ExecutionEvents {

    private static final ThreadLocal<RuntimeEventContext> CURRENT = new ThreadLocal<>();

    private ExecutionEvents() {
    }

    public static void emit(ExecutionEvent event) {
        Objects.requireNonNull(event);
        RuntimeEventContext c = CURRENT.get();
        if (c == null) return;
        domainCheckpoint(c);
        c.emit(event);
    }

    public static void checkpoint() {
        RuntimeEventContext c = CURRENT.get();
        if (c == null) return;
        try {
            c.checkpoint();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionCancellationException("Execution interrupted");
        }
    }

    private static void domainCheckpoint(RuntimeEventContext c) {
        try {
            c.domainCheckpoint();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionCancellationException("Execution interrupted");
        }
    }

    static Binding bind(RuntimeEventContext c) {
        Objects.requireNonNull(c);
        RuntimeEventContext p = CURRENT.get();
        CURRENT.set(c);
        return new Binding(p);
    }

    static final class Binding implements AutoCloseable {

        private final RuntimeEventContext previous;
        private boolean closed;

        private Binding(RuntimeEventContext p) {
            previous = p;
        }

        public void close() {
            if (closed) return;
            closed = true;
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
