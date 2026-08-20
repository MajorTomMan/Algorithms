package com.majortom.algorithms.core.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Logical, algorithm-domain counters independent of a host runtime.
 *
 * <p>Event and frame counts describe the event stream and the selected reducer
 * projection. Named operation counts are supplied by {@code StatisticsContribution}
 * events. Input size is optional because not every algorithm has one canonical
 * scalar size.</p>
 */
public record AlgorithmStatistics(
        long eventCount,
        long frameCount,
        OptionalLong inputSize,
        Map<String, Long> operationCounts) {

    public AlgorithmStatistics {
        if (eventCount < 0L) {
            throw new IllegalArgumentException("eventCount must not be negative");
        }
        if (frameCount < 0L) {
            throw new IllegalArgumentException("frameCount must not be negative");
        }
        inputSize = Objects.requireNonNull(inputSize, "inputSize");
        if (inputSize.isPresent() && inputSize.getAsLong() < 0L) {
            throw new IllegalArgumentException("inputSize must not be negative");
        }
        operationCounts = immutableOperationCounts(operationCounts);
    }

    /** Creates logical statistics from the event-derived execution statistics. */
    public static AlgorithmStatistics from(ExecutionStatistics statistics) {
        Objects.requireNonNull(statistics, "statistics");
        return new AlgorithmStatistics(
                statistics.algorithmEventCount(),
                statistics.visualFrameCount(),
                OptionalLong.empty(),
                statistics.metrics());
    }

    /** Returns a named operation count, or zero when it is absent. */
    public long operationCount(String name) {
        Objects.requireNonNull(name, "name");
        return operationCounts.getOrDefault(name, 0L);
    }

    /** Returns a copy with an explicitly supplied input size. */
    public AlgorithmStatistics withInputSize(long value) {
        return new AlgorithmStatistics(eventCount, frameCount, OptionalLong.of(value), operationCounts);
    }

    private static Map<String, Long> immutableOperationCounts(Map<String, Long> source) {
        Objects.requireNonNull(source, "operationCounts");
        Map<String, Long> copy = new LinkedHashMap<>();
        source.forEach((name, value) -> {
            Objects.requireNonNull(name, "operation name");
            Objects.requireNonNull(value, "operation count");
            if (name.isBlank()) {
                throw new IllegalArgumentException("Operation names must not be blank");
            }
            if (value < 0L) {
                throw new IllegalArgumentException("Operation counts must not be negative");
            }
            copy.put(name, value);
        });
        return Map.copyOf(copy);
    }
}
