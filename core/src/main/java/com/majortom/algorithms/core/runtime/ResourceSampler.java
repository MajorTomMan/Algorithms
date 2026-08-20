package com.majortom.algorithms.core.runtime;

/**
 * Runtime-neutral boundary for optional execution-resource sampling.
 *
 * <p>Implementations may use JVM, operating-system, container, or application
 * counters.  Core only consumes the immutable {@link ResourceUsage} result.
 * Sampling failures should normally be handled by the implementation and
 * represented by an empty or partially populated result.</p>
 */
@FunctionalInterface
public interface ResourceSampler {

    /** Returns the best-effort usage measured for the current execution. */
    ResourceUsage sample();

    /** Marks the beginning of an execution interval. */
    default void start() {
    }

    /** Marks the end of an execution interval. */
    default void stop() {
    }

    /** Returns a sampler that intentionally reports no host measurements. */
    static ResourceSampler noop() {
        return ResourceUsage::empty;
    }
}
