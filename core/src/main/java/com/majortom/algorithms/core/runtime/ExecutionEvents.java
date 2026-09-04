package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.event.observation.ObservationEvent;
import com.majortom.algorithms.core.event.structure.StructureEvent;

import java.util.Objects;

public final class ExecutionEvents {
    private static final ThreadLocal<RuntimeEventContext> CURRENT = new ThreadLocal<>();

    private ExecutionEvents() {
    }

    /** Emits one factual non-lifecycle execution event through the bound Runtime context. */
    public static void emit(ExecutionEvent event) {
        Objects.requireNonNull(event, "event");
        RuntimeEventContext context = CURRENT.get();
        if (context == null) {
            return;
        }
        if (event instanceof StructureEvent || event instanceof ObservationEvent) {
            domainCheckpoint(context);
        }
        context.emit(event);
    }

    /** Convenience entry point that makes read-only observations explicit at call sites. */
    public static void observe(ObservationEvent event) {
        emit(Objects.requireNonNull(event, "event"));
    }

    /** Cooperative execution checkpoint. A paused run consumes one step permit to pass it. */
    public static void checkpoint() {
        RuntimeEventContext context = CURRENT.get();
        if (context == null) {
            return;
        }
        try {
            context.checkpoint();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExecutionCancellationException("Execution interrupted");
        }
    }

    private static void domainCheckpoint(RuntimeEventContext context) {
        try {
            context.domainCheckpoint();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExecutionCancellationException("Execution interrupted");
        }
    }

    static Binding bind(RuntimeEventContext context) {
        Objects.requireNonNull(context, "context");
        RuntimeEventContext previous = CURRENT.get();
        CURRENT.set(context);
        return new Binding(previous);
    }

    static final class Binding implements AutoCloseable {
        private final RuntimeEventContext previous;
        private boolean closed;

        private Binding(RuntimeEventContext previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
