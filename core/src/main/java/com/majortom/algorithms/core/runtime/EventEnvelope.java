package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.event.ExecutionEvent;
import java.time.Instant;
import java.util.Objects;

/**
 * 一条领域事实的运行信封：只包装语义事件，不反向改变事实本身。
 *
 * <p>runId 与 operationId 让客户端知道事件属于哪次运行；
 * 从零开始的 sequence 是同一次运行中唯一的权威顺序。
 * timestamp 是发生时间，不应使用它重新排序（系统时钟可能跳变）。
 *
 * <p>ExecutionRuntime/RuntimeEventContext 创建此信封；
 * EventSink 保存它，ReductionCursor 校验序号，Reducer 再读取 event
 * 产生可回放视图。日志、统计和导出也应引用同一事件序列。
 */
public record EventEnvelope(String runId, String operationId, long sequence, Instant timestamp,
    String source, ExecutionEvent event) {
  public EventEnvelope {
    runId = requireText(runId, "runId");
    operationId = requireText(operationId, "operationId");
    source = requireText(source, "source");
    if (sequence < 0) {
      throw new IllegalArgumentException("sequence must not be negative");
    }
    timestamp = Objects.requireNonNull(timestamp, "timestamp");
    event = Objects.requireNonNull(event, "event");
  }

  private static String requireText(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
