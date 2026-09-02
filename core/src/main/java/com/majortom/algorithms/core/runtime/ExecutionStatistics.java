package com.majortom.algorithms.core.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Runtime-neutral statistics derived only from the authoritative event stream. */
public record ExecutionStatistics(
        long totalEventCount,
        long domainEventCount,
        long lifecycleEventCount,
        Optional<Instant> startedAt,
        Optional<Instant> endedAt,
        Duration duration,
        Map<String, Long> metrics) {

    public ExecutionStatistics {
        if (totalEventCount < 0L) throw new IllegalArgumentException("totalEventCount must not be negative");
        if (domainEventCount < 0L || lifecycleEventCount < 0L) throw new IllegalArgumentException("Event counts must not be negative");
        if (domainEventCount + lifecycleEventCount != totalEventCount) throw new IllegalArgumentException("Domain and lifecycle counts must equal totalEventCount");
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
        endedAt = Objects.requireNonNull(endedAt, "endedAt");
        duration = Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) throw new IllegalArgumentException("duration must not be negative");
        metrics = immutableMetrics(metrics);
    }

    public Duration eventSpan() { return duration; }
    public long eventCount() { return domainEventCount; }
    public long operationCount(String name) { return metric(name); }
    public DomainStatistics domainStatistics() { return DomainStatistics.from(this); }

    public static ExecutionStatistics empty() {
        return new ExecutionStatistics(0L, 0L, 0L, Optional.empty(), Optional.empty(), Duration.ZERO, Map.of());
    }

    public long metric(String name) {
        Objects.requireNonNull(name, "name");
        return metrics.getOrDefault(name, 0L);
    }

    private static Map<String, Long> immutableMetrics(Map<String, Long> source) {
        Objects.requireNonNull(source, "metrics");
        Map<String, Long> copy = new LinkedHashMap<>();
        source.forEach((name, value) -> {
            Objects.requireNonNull(name, "metric name");
            Objects.requireNonNull(value, "metric value");
            if (name.isBlank()) throw new IllegalArgumentException("Metric names must not be blank");
            if (value < 0L) throw new IllegalArgumentException("Metric values must not be negative");
            copy.put(name, value);
        });
        return Map.copyOf(copy);
    }
}
