package com.majortom.algorithms.telemetry.memory.api;

import java.io.Serial;
import java.io.Serializable;

/** Typed allocation sample derived from a generic telemetry sample. */
public record MemoryAllocationSample(long elapsedNanos, long allocatedBytes) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public MemoryAllocationSample {
    if (elapsedNanos < 0L) {
      throw new IllegalArgumentException("elapsedNanos must not be negative");
    }
    if (allocatedBytes < 0L) {
      throw new IllegalArgumentException("allocatedBytes must not be negative");
    }
  }
}
