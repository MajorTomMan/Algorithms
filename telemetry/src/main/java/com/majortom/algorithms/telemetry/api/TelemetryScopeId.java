package com.majortom.algorithms.telemetry.api;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Stable identity for one telemetry scope, independent of controllers and UI classes. */
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
