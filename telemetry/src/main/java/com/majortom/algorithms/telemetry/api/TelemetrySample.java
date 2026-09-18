package com.majortom.algorithms.telemetry.api;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable multi-probe sample relative to the start of one telemetry session. */
public record TelemetrySample(
    long elapsedNanos,
    Map<String, TelemetryValue> values) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public TelemetrySample {
    if (elapsedNanos < 0L) {
      throw new IllegalArgumentException("elapsedNanos must not be negative");
    }
    Objects.requireNonNull(values, "values");
    Map<String, TelemetryValue> copy = new LinkedHashMap<>();
    for (Map.Entry<String, TelemetryValue> entry : values.entrySet()) {
      String key = Objects.requireNonNull(entry.getKey(), "metric id").trim();
      if (key.isEmpty()) {
        throw new IllegalArgumentException("metric id must not be blank");
      }
      copy.put(key, Objects.requireNonNull(entry.getValue(), "metric value"));
    }
    values = Map.copyOf(copy);
  }

  public Optional<TelemetryValue> value(String metricId) {
    return Optional.ofNullable(values.get(metricId));
  }
}
