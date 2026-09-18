package com.majortom.algorithms.core.memory;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/** Immutable best-effort memory facts for one execution scope. */
public record MemoryProfile(
    MemorySessionId sessionId,
    MemoryCapabilities capabilities,
    boolean complete,
    boolean timingRepresentative,
    long durationNanos,
    Long allocatedBytes,
    Long averageAllocationRateBytesPerSecond,
    Long peakAllocationRateBytesPerSecond,
    Long heapDeltaBytes,
    Long gcCollectionCount,
    Long gcCollectionTimeMillis,
    List<MemorySample> samples) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public MemoryProfile {
    sessionId = Objects.requireNonNull(sessionId, "sessionId");
    capabilities = Objects.requireNonNull(capabilities, "capabilities");
    if (durationNanos < 0L) {
      throw new IllegalArgumentException("durationNanos must not be negative");
    }
    requireNonNegative(allocatedBytes, "allocatedBytes");
    requireNonNegative(averageAllocationRateBytesPerSecond,
        "averageAllocationRateBytesPerSecond");
    requireNonNegative(peakAllocationRateBytesPerSecond,
        "peakAllocationRateBytesPerSecond");
    requireNonNegative(gcCollectionCount, "gcCollectionCount");
    requireNonNegative(gcCollectionTimeMillis, "gcCollectionTimeMillis");
    samples = List.copyOf(Objects.requireNonNull(samples, "samples"));
  }


  /**
   * Returns the same allocation/GC totals while suppressing wall-time-derived data. This is used
   * for debugger-instrumented executions whose stepping overhead makes allocation rates misleading.
   */
  public MemoryProfile withoutRepresentativeTiming() {
    return new MemoryProfile(
        sessionId,
        capabilities,
        complete,
        false,
        durationNanos,
        allocatedBytes,
        null,
        null,
        heapDeltaBytes,
        gcCollectionCount,
        gcCollectionTimeMillis,
        List.of());
  }

  public Duration duration() {
    return Duration.ofNanos(durationNanos);
  }

  public OptionalLong allocatedBytesValue() {
    return optional(allocatedBytes);
  }

  public OptionalLong averageAllocationRateBytesPerSecondValue() {
    return optional(averageAllocationRateBytesPerSecond);
  }

  public OptionalLong peakAllocationRateBytesPerSecondValue() {
    return optional(peakAllocationRateBytesPerSecond);
  }

  /** Whole-JVM heap delta is a reference signal only; it may legitimately be negative after GC. */
  public OptionalLong heapDeltaBytesValue() {
    return optional(heapDeltaBytes);
  }

  public OptionalLong gcCollectionCountValue() {
    return optional(gcCollectionCount);
  }

  public OptionalLong gcCollectionTimeMillisValue() {
    return optional(gcCollectionTimeMillis);
  }

  private static OptionalLong optional(Long value) {
    return value == null ? OptionalLong.empty() : OptionalLong.of(value);
  }

  private static void requireNonNegative(Long value, String name) {
    if (value != null && value < 0L) {
      throw new IllegalArgumentException(name + " must not be negative");
    }
  }
}
