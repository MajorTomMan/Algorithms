package com.majortom.algorithms.core.runtime;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Shared execution summary consumed by desktop, server, and other runtimes.
 *
 * <p>The event statistics are authoritative logical counters. Timing and host
 * resources are separate sections so a consumer cannot accidentally present
 * replay or resource time as algorithm time.</p>
 */
public record ExecutionSummary(
        ExecutionStatistics statistics,
        OptionalLong inputSize,
        ExecutionTiming timing,
        ResourceUsage resources) {

    public ExecutionSummary {
        statistics = Objects.requireNonNull(statistics, "statistics");
        inputSize = requireInputSize(inputSize);
        timing = Objects.requireNonNull(timing, "timing");
        if (!timing.eventSpan().equals(statistics.eventSpan())) {
            throw new IllegalArgumentException("Timing event span must match execution statistics");
        }
        resources = Objects.requireNonNull(resources, "resources");
    }

    /** Creates an empty summary before any event has been observed. */
    public static ExecutionSummary empty() {
        return new ExecutionSummary(
                ExecutionStatistics.empty(),
                OptionalLong.empty(),
                ExecutionTiming.eventOnly(java.time.Duration.ZERO),
                ResourceUsage.empty());
    }

    /** Creates the common summary available from an event-derived statistic. */
    public static ExecutionSummary from(ExecutionStatistics statistics) {
        Objects.requireNonNull(statistics, "statistics");
        return new ExecutionSummary(
                statistics,
                OptionalLong.empty(),
                ExecutionTiming.eventOnly(statistics.eventSpan()),
                ResourceUsage.empty());
    }

    /** Creates a summary with a supplied host-resource measurement. */
    public static ExecutionSummary from(
            ExecutionStatistics statistics,
            ResourceUsage usage) {
        return from(statistics).withResourceUsage(usage);
    }

    /** Returns the logical algorithm counters represented by the summary. */
    public AlgorithmStatistics algorithmStatistics() {
        AlgorithmStatistics logical = statistics.algorithmStatistics();
        if (inputSize.isPresent()) {
            return logical.withInputSize(inputSize.getAsLong());
        }
        return logical;
    }

    /** Returns the timestamp span represented by the execution event stream. */
    public Duration eventSpan() {
        return timing.eventSpan();
    }

    /** Returns host-measured total execution time when the environment supplied it. */
    public Optional<Duration> totalDuration() {
        return timing.totalDuration();
    }

    /** Returns active replay time when a replay-capable consumer supplied it. */
    public Optional<Duration> playbackDuration() {
        return timing.playbackDuration();
    }

    public ExecutionSummary withInputSize(long value) {
        return new ExecutionSummary(statistics, OptionalLong.of(value), timing, resources);
    }

    public ExecutionSummary withTiming(ExecutionTiming value) {
        return new ExecutionSummary(statistics, inputSize, value, resources);
    }

    public ExecutionSummary withResources(ResourceUsage value) {
        return new ExecutionSummary(statistics, inputSize, timing, value);
    }

    /** Returns the host-resource measurements supplied by the execution environment. */
    public ResourceUsage resourceUsage() {
        return resources;
    }

    /** Adds a host-resource measurement while retaining all logical statistics. */
    public ExecutionSummary withResourceUsage(ResourceUsage value) {
        Objects.requireNonNull(value, "resourceUsage");
        return withResources(value);
    }

    private static OptionalLong requireInputSize(OptionalLong value) {
        Objects.requireNonNull(value, "inputSize");
        if (value.isPresent() && value.getAsLong() < 0L) {
            throw new IllegalArgumentException("inputSize must not be negative");
        }
        return value;
    }
}
