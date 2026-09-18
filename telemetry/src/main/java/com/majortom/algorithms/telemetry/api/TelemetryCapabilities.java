package com.majortom.algorithms.telemetry.api;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Capability map used for graceful feature degradation across JVMs and platforms. */
public record TelemetryCapabilities(Map<String, Boolean> values) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public static final TelemetryCapabilities EMPTY = new TelemetryCapabilities(Map.of());

  public TelemetryCapabilities {
    Objects.requireNonNull(values, "values");
    Map<String, Boolean> copy = new LinkedHashMap<>();
    for (Map.Entry<String, Boolean> entry : values.entrySet()) {
      String id = Objects.requireNonNull(entry.getKey(), "capability id").trim();
      if (id.isEmpty()) {
        throw new IllegalArgumentException("capability id must not be blank");
      }
      copy.put(id, Boolean.TRUE.equals(entry.getValue()));
    }
    values = Map.copyOf(copy);
  }

  public boolean available(String capabilityId) {
    return Boolean.TRUE.equals(values.get(capabilityId));
  }

  public TelemetryCapabilities merge(TelemetryCapabilities other) {
    Objects.requireNonNull(other, "other");
    Map<String, Boolean> merged = new LinkedHashMap<>(values);
    other.values.forEach((id, available) -> merged.merge(id, available, (left, right) -> left || right));
    return new TelemetryCapabilities(merged);
  }
}
