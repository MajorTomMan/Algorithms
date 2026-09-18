package com.majortom.algorithms.telemetry.memory.analysis;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** One JFR allocation-sample aggregate grouped by the first useful allocation stack frame. */
public record MemoryAllocationSiteStat(String site, long estimatedBytes, long sampleCount)
    implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  public MemoryAllocationSiteStat {
    site = Objects.requireNonNullElse(site, "unknown");
    estimatedBytes = Math.max(0L, estimatedBytes);
    sampleCount = Math.max(0L, sampleCount);
  }
}
