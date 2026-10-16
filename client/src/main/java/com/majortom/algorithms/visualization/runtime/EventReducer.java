package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.runtime.EventEnvelope;

/**
 * 权威事件流到不可变视图状态的纯归约协议。
 *
 * <p>对同一个 previousState 与 EventEnvelope，必须得到同一归约结果；
 * 不读取 UI、系统时间或外部可变状态。因而同一批事件既可以在后台
 * 增量直播，也能离线重放、倒退和随机 seek。
 *
 * <p>Reducer 不负责读取 JSON、维护领域图或创建 JavaFX Node。
 * GraphEventReducer 只是其中一种领域事件适配器；
 * 可见帧的挑选和序号连续性由 ReductionCursor/ReducedEventTimeline 负责。
 */
public interface EventReducer<S> {
  /** Creates the state used before sequence zero is reduced. */
  S initialState();

  /** Reduces one event without retaining execution-specific mutable state. */
  Reduction<S> reduce(S previousState, EventEnvelope event);
}
