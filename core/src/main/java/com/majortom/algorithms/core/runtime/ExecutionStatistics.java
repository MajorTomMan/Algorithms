package com.majortom.algorithms.core.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Runtime-neutral statistics derived only from the authoritative event stream. */
public record ExecutionStatistics(long totalEventCount, long domainEventCount,
    long lifecycleEventCount, Optional<Instant> startedAt, Optional<Instant> endedAt,
    Duration duration, Map<String, Long> metrics) {
  private static final Map<String, Long> EMPTY_METRICS = new ValidatedMetrics(Map.of());

  public ExecutionStatistics {
    if (totalEventCount < 0L)
      throw new IllegalArgumentException("totalEventCount must not be negative");
    if (domainEventCount < 0L || lifecycleEventCount < 0L)
      throw new IllegalArgumentException("Event counts must not be negative");
    if (domainEventCount + lifecycleEventCount != totalEventCount)
      throw new IllegalArgumentException("Domain and lifecycle counts must equal totalEventCount");
    startedAt = Objects.requireNonNull(startedAt, "startedAt");
    endedAt = Objects.requireNonNull(endedAt, "endedAt");
    duration = Objects.requireNonNull(duration, "duration");
    if (duration.isNegative())
      throw new IllegalArgumentException("duration must not be negative");
    metrics = immutableMetrics(metrics);
  }

  public Duration eventSpan() {
    return duration;
  }
  public long eventCount() {
    return domainEventCount;
  }
  public long operationCount(String name) {
    return metric(name);
  }

  public static ExecutionStatistics empty() {
    return new ExecutionStatistics(
        0L, 0L, 0L, Optional.empty(), Optional.empty(), Duration.ZERO, Map.of());
  }

  public long metric(String name) {
    Objects.requireNonNull(name, "name");
    return metrics.getOrDefault(name, 0L);
  }

  private static Map<String, Long> immutableMetrics(Map<String, Long> source) {
    Objects.requireNonNull(source, "metrics");
    // Consecutive events usually keep the exact same metrics. This marker is
    // private, so only maps that have already been validated and frozen can
    // be reused. Public mutable inputs are still defensively copied.
    if (source instanceof ValidatedMetrics)
      return source;
    if (source.isEmpty())
      return EMPTY_METRICS;
    Map<String, Long> copy = new LinkedHashMap<>();
    source.forEach((name, value) -> {
      Objects.requireNonNull(name, "metric name");
      Objects.requireNonNull(value, "metric value");
      if (name.isBlank())
        throw new IllegalArgumentException("Metric names must not be blank");
      if (value < 0L)
        throw new IllegalArgumentException("Metric values must not be negative");
      copy.put(name, value);
    });
    return new ValidatedMetrics(Map.copyOf(copy));
  }

  /** A validated, immutable metrics map that can be shared across snapshots. */
  private static final class ValidatedMetrics extends AbstractMap<String, Long> {
    private final Map<String, Long> values;

    private ValidatedMetrics(Map<String, Long> values) {
      this.values = values;
    }

    @Override
    public Set<Entry<String, Long>> entrySet() {
      return values.entrySet();
    }

    @Override
    public Long get(Object key) {
      return values.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
      return values.containsKey(key);
    }

    @Override
    public int size() {
      return values.size();
    }
  }
}
