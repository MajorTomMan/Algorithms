package com.majortom.algorithms.telemetry.api;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable factual telemetry snapshot for one execution scope. */
public record TelemetryProfile(
    TelemetrySessionId sessionId,
    TelemetrySessionState state,
    TelemetryCapabilities capabilities,
    boolean timingRepresentative,
    long durationNanos,
    Map<String, TelemetryMetricDescriptor> descriptors,
    Map<String, TelemetryValue> summary,
    List<TelemetrySample> samples) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public TelemetryProfile {
    sessionId = Objects.requireNonNull(sessionId, "sessionId");
    state = Objects.requireNonNull(state, "state");
    capabilities = Objects.requireNonNull(capabilities, "capabilities");
    if (durationNanos < 0L) {
      throw new IllegalArgumentException("durationNanos must not be negative");
    }
    descriptors = immutableMap(descriptors, "descriptors");
    summary = immutableMap(summary, "summary");
    samples = List.copyOf(Objects.requireNonNull(samples, "samples"));
  }

  public Duration duration() {
    return Duration.ofNanos(durationNanos);
  }

  public Optional<TelemetryValue> summaryValue(String metricId) {
    return Optional.ofNullable(summary.get(metricId));
  }

  private static <V> Map<String, V> immutableMap(Map<String, V> source, String name) {
    Objects.requireNonNull(source, name);
    Map<String, V> copy = new LinkedHashMap<>();
    for (Map.Entry<String, V> entry : source.entrySet()) {
      String key = Objects.requireNonNull(entry.getKey(), name + " key").trim();
      if (key.isEmpty()) {
        throw new IllegalArgumentException(name + " key must not be blank");
      }
      copy.put(key, Objects.requireNonNull(entry.getValue(), name + " value"));
    }
    return Map.copyOf(copy);
  }
}
