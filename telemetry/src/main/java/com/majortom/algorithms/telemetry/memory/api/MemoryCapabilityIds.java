package com.majortom.algorithms.telemetry.memory.api;

/** Stable capability identifiers for memory telemetry and optional deep analysis. */
public final class MemoryCapabilityIds {
  public static final String THREAD_ALLOCATED_BYTES = "memory.threadAllocatedBytes";
  public static final String HEAP_USAGE = "memory.heapUsage";
  public static final String GARBAGE_COLLECTION = "memory.garbageCollection";
  public static final String JFR_ALLOCATION = "memory.jfrAllocation";
  public static final String JOL_FOOTPRINT = "memory.jolFootprint";

  private MemoryCapabilityIds() {}
}
