package com.majortom.algorithms.core.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime-neutral event statistics derived from one authoritative event stream.
 *
 * <p>{@link #duration()} and {@link #eventSpan()} are the timestamp span of the
 * execution event stream. Host total execution and active replay time belong to
 * {@link ExecutionTiming} and are intentionally not folded into this record.</p>
 */
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

    /** Returns the timestamp span between run start and the terminal event. */
    public Duration eventSpan() {
        return duration;
    }

    /** Returns the number of non-lifecycle algorithm events. */
    public long eventCount() {
        return algorithmEventCount;
    }

    /** Returns the number of visible frames supplied by the consuming reducer. */
    public long frameCount() {
        return visualFrameCount;
    }

    /** Returns a named logical operation count, or zero when it is absent. */
    public long operationCount(String name) {
        return metric(name);
    }

    /** Builds the logical algorithm statistics represented by this event reduction. */
    public AlgorithmStatistics algorithmStatistics() {
        return AlgorithmStatistics.from(this);
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
