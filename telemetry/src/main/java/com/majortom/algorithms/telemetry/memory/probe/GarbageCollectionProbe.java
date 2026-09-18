package com.majortom.algorithms.telemetry.memory.probe;

import com.majortom.algorithms.telemetry.api.TelemetryCapabilities;
import com.majortom.algorithms.telemetry.api.TelemetryMetricDescriptor;
import com.majortom.algorithms.telemetry.api.TelemetryMetricKind;
import com.majortom.algorithms.telemetry.api.TelemetrySessionId;
import com.majortom.algorithms.telemetry.api.TelemetryValue;
import com.majortom.algorithms.telemetry.memory.api.MemoryCapabilityIds;
import com.majortom.algorithms.telemetry.memory.api.MemoryMetricIds;
import com.majortom.algorithms.telemetry.probe.TelemetryProbe;
import com.majortom.algorithms.telemetry.probe.TelemetryProbeSession;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

/** Whole-JVM GC deltas sampled relative to the exact execution boundary. */
public final class GarbageCollectionProbe implements TelemetryProbe {
  private final List<GarbageCollectorMXBean> collectors =
      List.copyOf(ManagementFactory.getGarbageCollectorMXBeans());

  @Override
  public String id() {
    return "memory.gc";
  }

  @Override
  public TelemetryCapabilities capabilities() {
    return new TelemetryCapabilities(Map.of(MemoryCapabilityIds.GARBAGE_COLLECTION, !collectors.isEmpty()));
  }

  @Override
  public List<TelemetryMetricDescriptor> descriptors() {
    return List.of(
        new TelemetryMetricDescriptor(MemoryMetricIds.GC_COLLECTION_COUNT, "count", TelemetryMetricKind.COUNTER),
        new TelemetryMetricDescriptor(
            MemoryMetricIds.GC_COLLECTION_TIME_MILLIS, "ms", TelemetryMetricKind.CUMULATIVE));
  }

  @Override
  public TelemetryProbeSession open(TelemetrySessionId sessionId) {
    return new Session();
  }

  private final class Session implements TelemetryProbeSession {
    private GcSnapshot baseline = snapshot();

    @Override
    public synchronized void rebase() {
      baseline = snapshot();
    }

    @Override
    public synchronized Map<String, TelemetryValue> sample() {
      GcSnapshot current = snapshot();
      Long count = deltaNonNegative(baseline.collectionCount, current.collectionCount);
      Long time = deltaNonNegative(baseline.collectionTimeMillis, current.collectionTimeMillis);
      if (count == null && time == null) return Map.of();
      java.util.LinkedHashMap<String, TelemetryValue> values = new java.util.LinkedHashMap<>();
      if (count != null) values.put(MemoryMetricIds.GC_COLLECTION_COUNT, TelemetryValue.of(count));
      if (time != null) values.put(MemoryMetricIds.GC_COLLECTION_TIME_MILLIS, TelemetryValue.of(time));
      return Map.copyOf(values);
    }
  }

  private GcSnapshot snapshot() {
    long count = 0L;
    long time = 0L;
    boolean hasCount = false;
    boolean hasTime = false;
    for (GarbageCollectorMXBean collector : collectors) {
      try {
        long value = collector.getCollectionCount();
        if (value >= 0L) { count = saturatingAdd(count, value); hasCount = true; }
        value = collector.getCollectionTime();
        if (value >= 0L) { time = saturatingAdd(time, value); hasTime = true; }
      } catch (RuntimeException ignored) {
        // One collector can degrade without invalidating the others.
      }
    }
    return new GcSnapshot(hasCount ? count : null, hasTime ? time : null);
  }

  private static Long deltaNonNegative(Long before, Long after) {
    if (before == null || after == null || after < before) return null;
    return after - before;
  }

  private static long saturatingAdd(long left, long right) {
    if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
    return left + right;
  }

  private record GcSnapshot(Long collectionCount, Long collectionTimeMillis) {}
}
