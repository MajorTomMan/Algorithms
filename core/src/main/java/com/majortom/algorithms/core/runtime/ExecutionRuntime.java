package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.domain.execution.RunCancelledEvent;
import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.domain.execution.RunFailedEvent;
import com.majortom.algorithms.core.domain.execution.RunStartedEvent;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 一次执行的领域生命周期控制器，位于算法/结构与客户端之间。
 *
 * <p>创建 RuntimeEventContext → 发布 RunStarted → 等待运行许可 →
 * 在 ExecutionEvents 绑定的线程上下文中执行操作 → 发布 Completed/Failed/Cancelled。
 * 事实计算由算法/结构负责；事件被 EventSink 记录和分发。
 *
 * <p>Runtime 不创建线程也不触碰 JavaFX。GUI 的 LocalAlgorithmExecution
 * 使用 ExecutionScheduler 放到 worker 执行；暂停/单步/取消通过 RunControl
 * 协作检查点完成，而不是直接挂起工作线程。
 *
 * <p>业务异常转换为 ExecutionResult 和失败事件；事件投递失败单独分类，
 * 防止把一段不完整事件流误认作成功执行。
 */
public final class ExecutionRuntime {
  private final Clock clock;
  private final Supplier<String> runIdSupplier;

  public ExecutionRuntime() {
    this(Clock.systemUTC(), () -> UUID.randomUUID().toString());
  }

  public ExecutionRuntime(Clock clock, Supplier<String> runIdSupplier) {
    this.clock = Objects.requireNonNull(clock, "clock");
    this.runIdSupplier = Objects.requireNonNull(runIdSupplier, "runIdSupplier");
  }

  public ExecutionResult execute(
      String operationId, EventSink sink, ExecutionOperation<?> operation) {
    return execute(operationId, operationId, sink, RunControl.unrestricted(), operation);
  }

  public ExecutionResult execute(
      String operationId, EventSink sink, RunControl control, ExecutionOperation<?> operation) {
    return execute(operationId, operationId, sink, control, operation);
  }

  public ExecutionResult execute(String operationId, String source, EventSink sink,
      RunControl control, ExecutionOperation<?> operation) {
    Objects.requireNonNull(operation, "operation");
    RuntimeEventContext context =
        new RuntimeEventContext(runIdSupplier.get(), operationId, source, sink, control, clock);
    DefaultExecutionControl defaultControl;
    if (control instanceof DefaultExecutionControl value) {
      defaultControl = value;
    } else {
      defaultControl = null;
    }
    if (defaultControl != null) {
      defaultControl.bindLifecycle(context::emitLifecycle);
    }

    try {
      context.emitLifecycle(new RunStartedEvent());
      context.startCheckpoint();
      Object output;
      try (ExecutionEvents.Binding ignored = ExecutionEvents.bind(context)) {
        output = operation.execute();
      }
      context.completionCheckpoint();
      context.emitLifecycle(new RunCompletedEvent());
      return ExecutionResult.completed(output);
    } catch (ExecutionCancellationException exception) {
      return cancelled(context, exception.getMessage());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return cancelled(context, "Execution interrupted");
    } catch (EventDeliveryException exception) {
      return eventDeliveryFailure(exception);
    } catch (RuntimeException exception) {
      try {
        context.emitLifecycle(new RunFailedEvent("execution.operation.failed", message(exception)));
      } catch (EventDeliveryException deliveryFailure) {
        return eventDeliveryFailure(deliveryFailure);
      }
      return ExecutionResult.failed(new ExecutionFailure(
          "execution.operation.failed", message(exception), exception.getClass().getName()));
    } finally {
      if (defaultControl != null) {
        defaultControl.unbindLifecycle();
      }
    }
  }

  private ExecutionResult cancelled(RuntimeEventContext context, String reason) {
    try {
      if (reason == null) {
        context.emitLifecycle(new RunCancelledEvent("Execution cancelled"));
      } else {
        context.emitLifecycle(new RunCancelledEvent(reason));
      }
    } catch (EventDeliveryException deliveryFailure) {
      return eventDeliveryFailure(deliveryFailure);
    }
    return ExecutionResult.cancelled();
  }

  private static ExecutionResult eventDeliveryFailure(EventDeliveryException exception) {
    return ExecutionResult.failed(new ExecutionFailure(
        "execution.event.delivery.failed", message(exception), exception.getClass().getName()));
  }

  private static String message(Throwable throwable) {
    String message = throwable.getMessage();
    if (message == null || message.isBlank()) {
      return throwable.getClass().getSimpleName();
    } else {
      return message;
    }
  }
}
