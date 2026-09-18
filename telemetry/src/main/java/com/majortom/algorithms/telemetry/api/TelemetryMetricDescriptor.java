package com.majortom.algorithms.telemetry.api;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Stable metadata for one numeric telemetry metric. */
public record TelemetryMetricDescriptor(
    String id,
    String unit,
    TelemetryMetricKind kind) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public TelemetryMetricDescriptor {
    id = requireText(id, "id");
    unit = Objects.requireNonNullElse(unit, "").trim();
    kind = Objects.requireNonNull(kind, "kind");
  }

  private static String requireText(String value, String name) {
    String normalized = Objects.requireNonNull(value, name).trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return normalized;
  }
}
