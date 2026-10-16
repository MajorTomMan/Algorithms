package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.event.structure.StructureEvent;
import java.util.Objects;

/**
 * 算法与数据结构的事件发射门面：业务只产生事实，不依赖 JavaFX 或日志控件。
 *
 * <p>Runtime 将 RuntimeEventContext 绑定到当前线程的 ThreadLocal；
 * {@link #algorithm} / {@link #structure} 按类别发布事实，
 * 经过协作式许可检查后由上下文分配全局连续 sequence。
 * {@link #emit} 仅用于不属于算法/结构事实的辅助事件。
 *
 * <p>不在运行上下文时事件会被忽略，数据结构仍可独立使用。
 * 绑定不会自动传到新线程；Runtime 必须关闭 Binding，恢复先前上下文。
 * 客户端 EventReducer 才负责把事件解释为视图状态。
 */
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
