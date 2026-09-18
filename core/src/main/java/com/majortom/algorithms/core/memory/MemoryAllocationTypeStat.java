package com.majortom.algorithms.core.memory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** One JFR allocation-sample aggregate grouped by allocated object class. */
public record MemoryAllocationTypeStat(String className, long estimatedBytes, long sampleCount)
    implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public MemoryAllocationTypeStat {
    className = Objects.requireNonNullElse(className, "unknown");
    estimatedBytes = Math.max(0L, estimatedBytes);
    sampleCount = Math.max(0L, sampleCount);
  }
}
