package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.event.structure.StructureEvent;
import java.util.Objects;

public final class ExecutionEvents {
  private static final ThreadLocal<RuntimeEventContext> CURRENT = new ThreadLocal<>();

  private ExecutionEvents() {}

  /** Auxiliary events (logs and snapshots) only; domain events use typed channels. */
  public static void emit(ExecutionEvent event) {
    Objects.requireNonNull(event, "event");
    if (event instanceof AlgorithmEvent || event instanceof StructureEvent) {
      throw new IllegalArgumentException("Use algorithm() or structure() for domain events");
    }
    RuntimeEventContext context = CURRENT.get();
    if (context != null) context.emit(event);
  }

  /** One algorithm fact: one checkpoint and one globally ordered envelope. */
  public static void algorithm(AlgorithmEvent event) {
    Objects.requireNonNull(event, "event");
    if (event instanceof StructureEvent) {
      throw new IllegalArgumentException("Algorithm event cannot also be a StructureEvent");
    }
    emitDomain(event);
  }

  /** One canonical structure mutation: independent from algorithm presentation. */
  public static void structure(StructureEvent event) {
    Objects.requireNonNull(event, "event");
    if (event instanceof AlgorithmEvent) {
      throw new IllegalArgumentException("Structure event cannot also be an AlgorithmEvent");
    }
    emitDomain(event);
  }

  private static void emitDomain(ExecutionEvent event) {
    RuntimeEventContext context = CURRENT.get();
    if (context == null) return;
    domainCheckpoint(context);
    context.emit(event);
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
