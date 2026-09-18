package com.majortom.algorithms.telemetry.memory.api;

import com.majortom.algorithms.telemetry.api.TelemetryCapabilities;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Typed capability view used by memory consumers without exposing profiler implementation details. */
public record MemoryCapabilities(
    boolean threadAllocatedBytes,
    boolean heapUsage,
    boolean garbageCollection,
    boolean jfrAvailable) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public static MemoryCapabilities from(
      TelemetryCapabilities capabilities, boolean jfrAvailable) {
    Objects.requireNonNull(capabilities, "capabilities");
    return new MemoryCapabilities(
        capabilities.available(MemoryCapabilityIds.THREAD_ALLOCATED_BYTES),
        capabilities.available(MemoryCapabilityIds.HEAP_USAGE),
        capabilities.available(MemoryCapabilityIds.GARBAGE_COLLECTION),
        jfrAvailable);
  }
}
