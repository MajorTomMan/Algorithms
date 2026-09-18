package com.majortom.algorithms.core.memory;

import java.io.Serial;
import java.io.Serializable;

/** One cumulative allocation sample relative to the beginning of a memory session. */
public record MemorySample(long elapsedNanos, long allocatedBytes) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public MemorySample {
    if (elapsedNanos < 0L) {
      throw new IllegalArgumentException("elapsedNanos must not be negative");
    }
    if (allocatedBytes < 0L) {
      throw new IllegalArgumentException("allocatedBytes must not be negative");
    }
  }
}
