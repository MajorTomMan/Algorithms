package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.core.runtime.StatisticsReducer;
import java.util.Objects;
import java.util.Optional;

/**
 * 包在无状态 EventReducer 外面的有状态执行游标。
 *
 * <p>每消费一个 EventEnvelope，先验证 runId、operationId、sequence 连续性，
 * 再更新 ViewState 和 StatisticsReducer。这样直播与离线回放使用同一套
 * 顺序检查，不会因某个客户端入口漏掉验证而悄然展示错误帧。
 *
 * <p>Reducer 仍是纯函数；状态历史由 ReducedEventTimeline 负责索引。
 */
public final class ReductionCursor<S> {
  private final EventReducer<S> reducer;
  private S state;
  private String runId;
  private String operationId;
  private long nextSequence;
  private final StatisticsReducer statisticsReducer = new StatisticsReducer();
  private ExecutionStatistics statistics;

  public ReductionCursor(EventReducer<S> reducer) {
    this.reducer = Objects.requireNonNull(reducer, "reducer");
    reset();
  }

  /** Validates and reduces the next event in the execution stream. */
  public Reduction<S> accept(EventEnvelope event) {
    Objects.requireNonNull(event, "event");
    validateEnvelope(event);
    Reduction<S> reduction = Objects.requireNonNull(reducer.reduce(state, event), "reducer result");
    ExecutionStatistics nextStatistics = statisticsReducer.reduce(statistics, event);
    state = reduction.state();
    if (nextSequence == 0L) {
      runId = event.runId();
      operationId = event.operationId();
    }
    nextSequence++;
    statistics = nextStatistics;
    return reduction;
  }

  /** Restores this cursor to its reducer-defined state before sequence zero. */
  public void reset() {
    state = Objects.requireNonNull(reducer.initialState(), "reducer initial state");
    runId = null;
    operationId = null;
    nextSequence = 0L;
    statistics = statisticsReducer.initialState();
  }

  public S state() {
    return state;
  }

  public Optional<String> runId() {
    return Optional.ofNullable(runId);
  }

  public Optional<String> operationId() {
    return Optional.ofNullable(operationId);
  }

  public long eventCount() {
    return nextSequence;
  }

  public long nextSequence() {
    return nextSequence;
  }

  public ExecutionStatistics statistics() {
    return statistics;
  }

  private void validateEnvelope(EventEnvelope event) {
    if (event.sequence() != nextSequence) {
      throw new IllegalArgumentException("Expected execution event sequence " + nextSequence
          + " but received " + event.sequence());
    }
    if (nextSequence == 0L) {
      return;
    }
    if (!runId.equals(event.runId()) || !operationId.equals(event.operationId())) {
      throw new IllegalArgumentException("Cannot mix events from different executions");
    }
  }
}
