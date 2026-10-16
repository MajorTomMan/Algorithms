package com.majortom.algorithms.telemetry.api;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 遥测范围的稳定标识，与 UI 控件和具体执行线程身份无关。
 *
 * <p>三元组（domain、componentId、executionId）区分结构操作、
 * 算法及练习任务。TelemetrySessionId 在 scope 基础上区分每次实际执行。
 * 选择此设计是为了让多个控制器实例或多次打开工作区仍能查询同一类历史。
 */
public record TelemetryScopeId(
    TelemetryDomain domain,
    String componentId,
    String executionId) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public TelemetryScopeId {
    domain = Objects.requireNonNull(domain, "domain");
    componentId = requireText(componentId, "componentId");
    executionId = requireText(executionId, "executionId");
  }

  public static TelemetryScopeId structure(String structureId, String operationId) {
    return new TelemetryScopeId(TelemetryDomain.STRUCTURE, structureId, operationId);
  }

  public static TelemetryScopeId algorithm(String structureId, String algorithmId) {
    return new TelemetryScopeId(TelemetryDomain.ALGORITHM, structureId, algorithmId);
  }

  public static TelemetryScopeId practice(String practiceId) {
    return new TelemetryScopeId(TelemetryDomain.PRACTICE, "practice", practiceId);
  }

  public String stableId() {
    return domain.name().toLowerCase() + "/" + componentId + "/" + executionId;
  }

  private static String requireText(String value, String name) {
    String normalized = Objects.requireNonNull(value, name).trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return normalized;
  }
}
