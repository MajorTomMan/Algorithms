package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.event.ExecutionEvent;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 一次运行的事件排序上下文，只有 ExecutionRuntime 可以创建并绑定。
 *
 * <p>每个事实封装为 EventEnvelope（runId、operationId、sequence、时间与来源），
 * 同步投递 EventSink 成功才递增 sequence，失败时抛 EventDeliveryException，
 * 不允许产生断序但表面成功的回放记录。
 *
 * <p>start/domain/completion 使用不同的 RunControl 许可点：
 * 单步对应一个领域执行单位，终止完成需要显式恢复或取消。
 * 领域代码不能通过 {@link #emit} 伪造生命周期事件。
 */
final class RuntimeEventContext {
  private final String runId;
  private final String operationId;
  private final String source;
  private final EventSink eventSink;
  private final RunControl runControl;
  private final Clock clock;
  private final AtomicLong sequence = new AtomicLong();

  RuntimeEventContext(String runId, String operationId, String source, EventSink eventSink,
      RunControl runControl, Clock clock) {
    this.runId = requireText(runId, "runId");
    this.operationId = requireText(operationId, "operationId");
    this.source = requireText(source, "source");
    this.eventSink = Objects.requireNonNull(eventSink, "eventSink");
    this.runControl = Objects.requireNonNull(runControl, "runControl");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  void emit(ExecutionEvent event) {
    Objects.requireNonNull(event, "event");
    if (event instanceof ExecutionLifecycleEvent) {
      throw new IllegalArgumentException("Domain code cannot emit runtime lifecycle events");
    }
    publish(event);
  }

  void emitLifecycle(ExecutionLifecycleEvent event) {
    publish(Objects.requireNonNull(event, "event"));
  }

  void startCheckpoint() throws InterruptedException {
    awaitPermission(Permission.START);
  }

  void checkpoint() throws InterruptedException {
    awaitPermission(Permission.DOMAIN);
  }

  void domainCheckpoint() throws InterruptedException {
    awaitPermission(Permission.DOMAIN);
  }

  void completionCheckpoint() throws InterruptedException {
    awaitPermission(Permission.COMPLETION);
  }

  private void awaitPermission(Permission permission) throws InterruptedException {
    requireNotCancelled();
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException("Execution interrupted");
    }
    switch (permission) {
      case START -> runControl.awaitStartPermission(runControl);
      case DOMAIN -> runControl.awaitDomainEventPermission(runControl);
      case COMPLETION -> runControl.awaitCompletionPermission(runControl);
    }
    requireNotCancelled();
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException("Execution interrupted");
    }
  }

  private enum Permission { START, DOMAIN, COMPLETION }

  private void requireNotCancelled() {
    if (runControl.isCancellationRequested()) {
      throw new ExecutionCancellationException("Execution cancelled");
    }
  }

  private synchronized void publish(ExecutionEvent event) {
    long nextSequence = sequence.get();
    EventEnvelope envelope =
        new EventEnvelope(runId, operationId, nextSequence, clock.instant(), source, event);
    try {
      eventSink.accept(envelope);
      sequence.incrementAndGet();
    } catch (RuntimeException exception) {
      throw new EventDeliveryException("Execution event delivery failed", exception);
    }
  }

  private static String requireText(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
