package com.majortom.algorithms.core.memory;

import java.io.Serial;
import java.io.Serializable;

/** Runtime capability snapshot used to degrade memory profiling without failing execution. */
public record MemoryCapabilities(
    boolean threadAllocatedBytes,
    boolean heapUsage,
    boolean garbageCollection,
    boolean jfrAvailable) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;
}
