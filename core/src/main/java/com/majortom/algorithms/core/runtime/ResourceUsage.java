package com.majortom.algorithms.core.runtime;

import java.util.Objects;
import java.util.OptionalLong;

/**
 * Best-effort host-resource measurements for one execution.
 *
 * <p>The values are optional because a runtime may not expose a particular
 * counter.  The core model deliberately does not prescribe how a client or
 * server obtains these measurements.</p>
 */
public record ResourceUsage(
        OptionalLong cpuTimeNanos,
        OptionalLong peakMemoryBytes,
        OptionalLong outputBytes) {

    public ResourceUsage {
        cpuTimeNanos = requireNonNegative(cpuTimeNanos, "cpuTimeNanos");
        peakMemoryBytes = requireNonNegative(peakMemoryBytes, "peakMemoryBytes");
        outputBytes = requireNonNegative(outputBytes, "outputBytes");
    }

    /** Returns an empty measurement for an environment without sampling. */
    public static ResourceUsage empty() {
        return new ResourceUsage(
                OptionalLong.empty(), OptionalLong.empty(), OptionalLong.empty());
    }

    public static ResourceUsage of(
            OptionalLong cpuTimeNanos,
            OptionalLong peakMemoryBytes,
            OptionalLong outputBytes) {
        return new ResourceUsage(cpuTimeNanos, peakMemoryBytes, outputBytes);
    }

    private static OptionalLong requireNonNegative(OptionalLong value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isPresent() && value.getAsLong() < 0L) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }
}
