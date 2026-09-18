package com.majortom.algorithms.core.memory;

/** Creates execution-scoped memory measurements without exposing profiler implementation details. */
public interface MemoryProfiler {
  MemoryCapabilities capabilities();

  /** Starts a scope on the calling thread. Implementations must degrade rather than fail execution. */
  MemoryProfileSession begin(MemoryDomain domain, String scopeId);
}
