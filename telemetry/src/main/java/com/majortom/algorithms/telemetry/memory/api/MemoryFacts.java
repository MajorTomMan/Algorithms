package com.majortom.algorithms.telemetry.memory.api;

import com.majortom.algorithms.telemetry.api.TelemetryProfile;
import com.majortom.algorithms.telemetry.api.TelemetrySample;
import com.majortom.algorithms.telemetry.api.TelemetrySessionId;
import com.majortom.algorithms.telemetry.api.TelemetrySessionState;
import com.majortom.algorithms.telemetry.api.TelemetryValue;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Typed, immutable memory facts projected from a generic {@link TelemetryProfile}.
 * This class contains no presentation, replay, JavaFX, or renderer state.
 */
public record MemoryFacts(
    TelemetrySessionId sessionId,
    TelemetrySessionState state,
    boolean timingRepresentative,
    long durationNanos,
    Long allocatedBytes,
    Long averageAllocationRateBytesPerSecond,
    Long peakAllocationRateBytesPerSecond,
    Long heapDeltaBytes,
    Long gcCollectionCount,
    Long gcCollectionTimeMillis,
    List<MemoryAllocationSample> samples) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public MemoryFacts {
    sessionId = Objects.requireNonNull(sessionId, "sessionId");
    state = Objects.requireNonNull(state, "state");
    if (durationNanos < 0L) {
      throw new IllegalArgumentException("durationNanos must not be negative");
    }
    requireNonNegative(allocatedBytes, "allocatedBytes");
    requireNonNegative(averageAllocationRateBytesPerSecond, "averageAllocationRateBytesPerSecond");
    requireNonNegative(peakAllocationRateBytesPerSecond, "peakAllocationRateBytesPerSecond");
    requireNonNegative(gcCollectionCount, "gcCollectionCount");
    requireNonNegative(gcCollectionTimeMillis, "gcCollectionTimeMillis");
    samples = List.copyOf(Objects.requireNonNull(samples, "samples"));
  }

  public static MemoryFacts from(TelemetryProfile profile) {
    Objects.requireNonNull(profile, "profile");
    Long allocated = longSummary(profile, MemoryMetricIds.ALLOCATED_BYTES);
    Long heapDelta = longSummary(profile, MemoryMetricIds.HEAP_DELTA_BYTES);
    Long gcCount = longSummary(profile, MemoryMetricIds.GC_COLLECTION_COUNT);
    Long gcTime = longSummary(profile, MemoryMetricIds.GC_COLLECTION_TIME_MILLIS);
    List<MemoryAllocationSample> allocationSamples = allocationSamples(profile);
    Long averageRate = profile.timingRepresentative() ? rate(allocated, profile.durationNanos()) : null;
    Long peakRate = profile.timingRepresentative() ? peakRate(allocationSamples) : null;
    return new MemoryFacts(
        profile.sessionId(),
        profile.state(),
        profile.timingRepresentative(),
        profile.durationNanos(),
        allocated,
        averageRate,
        peakRate,
        heapDelta,
        gcCount,
        gcTime,
        profile.timingRepresentative() ? allocationSamples : List.of());
  }

  public boolean complete() {
    return state.executionEnded();
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

  public OptionalLong heapDeltaBytesValue() {
    return optional(heapDeltaBytes);
  }

  public OptionalLong gcCollectionCountValue() {
    return optional(gcCollectionCount);
  }

  public OptionalLong gcCollectionTimeMillisValue() {
    return optional(gcCollectionTimeMillis);
  }

  private static List<MemoryAllocationSample> allocationSamples(TelemetryProfile profile) {
    List<MemoryAllocationSample> result = new ArrayList<>();
    for (TelemetrySample sample : profile.samples()) {
      TelemetryValue value = sample.values().get(MemoryMetricIds.ALLOCATED_BYTES);
      if (value instanceof TelemetryValue.LongValue exact && exact.value() >= 0L) {
        result.add(new MemoryAllocationSample(sample.elapsedNanos(), exact.value()));
      }
    }
    return List.copyOf(result);
  }

  private static Long longSummary(TelemetryProfile profile, String metricId) {
    TelemetryValue value = profile.summary().get(metricId);
    if (value instanceof TelemetryValue.LongValue exact) {
      return exact.value();
    }
    if (value instanceof TelemetryValue.DoubleValue decimal) {
      double raw = decimal.value();
      if (raw >= Long.MAX_VALUE) return Long.MAX_VALUE;
      if (raw <= Long.MIN_VALUE) return Long.MIN_VALUE;
      return Math.round(raw);
    }
    return null;
  }

  private static Long rate(Long bytes, long elapsedNanos) {
    if (bytes == null || elapsedNanos <= 0L) return null;
    double value = bytes.doubleValue() * 1_000_000_000d / elapsedNanos;
    if (value >= Long.MAX_VALUE) return Long.MAX_VALUE;
    return Math.max(0L, Math.round(value));
  }

  private static Long peakRate(List<MemoryAllocationSample> samples) {
    if (samples.size() < 2) return null;
    long peak = 0L;
    boolean measured = false;
    for (int index = 1; index < samples.size(); index++) {
      MemoryAllocationSample previous = samples.get(index - 1);
      MemoryAllocationSample current = samples.get(index);
      long elapsed = current.elapsedNanos() - previous.elapsedNanos();
      long bytes = current.allocatedBytes() - previous.allocatedBytes();
      if (elapsed <= 0L || bytes < 0L) continue;
      Long currentRate = rate(bytes, elapsed);
      if (currentRate != null) {
        peak = Math.max(peak, currentRate);
        measured = true;
      }
    }
    return measured ? peak : null;
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
