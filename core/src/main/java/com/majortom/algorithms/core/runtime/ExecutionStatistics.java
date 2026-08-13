package com.majortom.algorithms.core.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Runtime-neutral execution and algorithm-domain statistics derived from events. */
public record ExecutionStatistics(
        long totalEventCount,
        long algorithmEventCount,
        long lifecycleEventCount,
        long visualFrameCount,
        Optional<Instant> startedAt,
        Optional<Instant> endedAt,
        Duration duration,
        Map<String, Long> metrics) {

    public ExecutionStatistics {
        if (totalEventCount < 0L) {
            throw new IllegalArgumentException("totalEventCount must not be negative");
        }
        if (algorithmEventCount < 0L || lifecycleEventCount < 0L) {
            throw new IllegalArgumentException("Event counts must not be negative");
        }
        if (algorithmEventCount + lifecycleEventCount != totalEventCount) {
            throw new IllegalArgumentException("Algorithm and lifecycle counts must equal totalEventCount");
        }
        if (visualFrameCount < 0L || visualFrameCount > totalEventCount) {
            throw new IllegalArgumentException("visualFrameCount must be between zero and totalEventCount");
        }
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
        endedAt = Objects.requireNonNull(endedAt, "endedAt");
        duration = Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        metrics = immutableMetrics(metrics);
    }

    /** Empty state used before the first event is reduced. */
    public static ExecutionStatistics empty() {
        return new ExecutionStatistics(
                0L,
                0L,
                0L,
                0L,
                Optional.empty(),
                Optional.empty(),
                Duration.ZERO,
                Map.of());
    }

    /** Returns an algorithm-specific counter, or zero when that counter was never contributed. */
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
            if (name.isBlank()) {
                throw new IllegalArgumentException("Metric names must not be blank");
            }
            if (value < 0L) {
                throw new IllegalArgumentException("Metric values must not be negative");
            }
            copy.put(name, value);
        });
        return Map.copyOf(copy);
    }
}
