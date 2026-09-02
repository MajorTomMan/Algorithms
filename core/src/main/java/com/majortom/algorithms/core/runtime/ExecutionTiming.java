package com.majortom.algorithms.core.runtime;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Runtime/headless timing for one execution event stream. */
public record ExecutionTiming(Duration eventSpan, Optional<Duration> totalDuration) {

    public ExecutionTiming {
        eventSpan = requireDuration(eventSpan, "eventSpan");
        totalDuration = requireOptionalDuration(totalDuration, "totalDuration");
    }

    public static ExecutionTiming eventOnly(Duration eventSpan) {
        return new ExecutionTiming(eventSpan, Optional.empty());
    }

    public static ExecutionTiming of(Duration eventSpan, Optional<Duration> totalDuration) {
        return new ExecutionTiming(eventSpan, totalDuration);
    }

    public static ExecutionTiming of(Duration eventSpan, Duration totalDuration) {
        return new ExecutionTiming(eventSpan, Optional.of(requireDuration(totalDuration, "totalDuration")));
    }

    public Optional<Duration> totalExecutionDuration() {
        return totalDuration;
    }

    public ExecutionTiming withTotalDuration(Duration value) {
        return new ExecutionTiming(eventSpan, Optional.of(requireDuration(value, "totalDuration")));
    }

    private static Duration requireDuration(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isNegative()) throw new IllegalArgumentException(name + " must not be negative");
        return value;
    }

    private static Optional<Duration> requireOptionalDuration(Optional<Duration> value, String name) {
        Objects.requireNonNull(value, name);
        value.ifPresent(duration -> requireDuration(duration, name));
        return value;
    }
}
