package com.majortom.algorithms.telemetry.runtime;

import com.majortom.algorithms.telemetry.api.TelemetryCapabilities;
import com.majortom.algorithms.telemetry.api.TelemetryScopeId;

/**
 * 与算法和可视化解耦的遥测会话创建接口。
 *
 * <p>Structure、Algorithm、Practice 以 TelemetryScopeId 表示稳定业务范围，
 * 每次执行都获得独立会话采集执行资源事实。
 * 业务算法不应直接操作某个 JavaFX 图表或把统计逻辑写入数据结构。
 *
 * <p>支持能力由 {@link #capabilities()} 声明，避免假设所有平台
 * 都具备 JFR 或完整内存探针；呈现层只消费最终不可变 Profile。
 */
public interface TelemetryFramework {
  TelemetryCapabilities capabilities();

  TelemetrySession begin(TelemetryScopeId scope);
}
