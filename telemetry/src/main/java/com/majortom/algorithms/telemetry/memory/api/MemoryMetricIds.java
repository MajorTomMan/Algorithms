package com.majortom.algorithms.telemetry.memory.api;

/** Stable metric identifiers produced by the built-in memory telemetry probes. */
public final class MemoryMetricIds {
  public static final String ALLOCATED_BYTES = "memory.allocated.bytes";
  public static final String HEAP_USED_BYTES = "memory.heap.used.bytes";
  public static final String HEAP_DELTA_BYTES = "memory.heap.delta.bytes";
  public static final String GC_COLLECTION_COUNT = "memory.gc.collection.count";
  public static final String GC_COLLECTION_TIME_MILLIS = "memory.gc.collection.time.millis";

  private MemoryMetricIds() {}
}
