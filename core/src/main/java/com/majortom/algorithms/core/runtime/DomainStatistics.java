package com.majortom.algorithms.core.runtime;

import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

public record DomainStatistics(long eventCount, OptionalLong inputSize, Map<String, Long> operationCounts) {
    public DomainStatistics {
        if (eventCount < 0L) throw new IllegalArgumentException("eventCount must not be negative");
        inputSize = Objects.requireNonNull(inputSize, "inputSize");
        operationCounts = Map.copyOf(Objects.requireNonNull(operationCounts, "operationCounts"));
    }

    public static DomainStatistics from(ExecutionStatistics statistics) {
        Objects.requireNonNull(statistics, "statistics");
        return new DomainStatistics(statistics.domainEventCount(), OptionalLong.empty(), statistics.metrics());
    }

    public DomainStatistics withInputSize(long value) {
        if (value < 0L) throw new IllegalArgumentException("inputSize must not be negative");
        return new DomainStatistics(eventCount, OptionalLong.of(value), operationCounts);
    }

    public long operationCount(String name) { return operationCounts.getOrDefault(name, 0L); }
}
