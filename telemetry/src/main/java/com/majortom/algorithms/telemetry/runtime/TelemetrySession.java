package com.majortom.algorithms.telemetry.runtime;

import com.majortom.algorithms.telemetry.api.TelemetryProfile;
import com.majortom.algorithms.telemetry.api.TelemetrySessionId;
import com.majortom.algorithms.telemetry.api.TelemetrySessionState;

/**
 * 一次遥测采样的可变运行边界，结果通过不可变 TelemetryProfile 对外发布。
 *
 * <p>attachCurrentThread 绑定当前业务执行线程，rebase 在实际开始时重置
 * 采样基线，sampleNow 强制采集关键执行点。complete/fail/cancel 明确终止状态，
 * 不能把中途快照当成已完成的测量结果。
 *
 * <p>遥测的采样时钟与 RenderClock 无关：动画快慢不得改变执行资源事实。
 */
public interface TelemetrySession extends AutoCloseable {
  TelemetrySessionId id();

  TelemetrySessionState state();

  boolean attachCurrentThread();

  /**
   * Establishes the exact execution boundary after optional profiler/analyzer setup.
   * Existing samples are discarded and probe baselines are reset before a new zero-point sample.
   */
  void rebase();

  /** Forces one factual sample now; useful at important execution/event boundaries. */
  void sampleNow();

  TelemetryProfile snapshot();

  void complete();

  void fail();

  void cancel();

  @Override
  default void close() {
    complete();
  }
}
