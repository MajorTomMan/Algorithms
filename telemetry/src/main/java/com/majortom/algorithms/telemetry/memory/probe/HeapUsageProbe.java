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
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Whole-JVM heap usage reference probe. Heap delta is not treated as execution-owned memory. */
public final class HeapUsageProbe implements TelemetryProbe {
  private final MemoryMXBean memoryBean;

  public HeapUsageProbe() {
    MemoryMXBean bean;
    try {
      bean = ManagementFactory.getMemoryMXBean();
    } catch (RuntimeException exception) {
      bean = null;
    }
    this.memoryBean = bean;
  }

  @Override
  public String id() {
    return "memory.heap";
  }

  @Override
  public TelemetryCapabilities capabilities() {
    return new TelemetryCapabilities(Map.of(MemoryCapabilityIds.HEAP_USAGE, memoryBean != null));
  }

  @Override
  public List<TelemetryMetricDescriptor> descriptors() {
    return List.of(
        new TelemetryMetricDescriptor(MemoryMetricIds.HEAP_USED_BYTES, "bytes", TelemetryMetricKind.GAUGE),
        new TelemetryMetricDescriptor(MemoryMetricIds.HEAP_DELTA_BYTES, "bytes", TelemetryMetricKind.GAUGE));
  }

  @Override
  public TelemetryProbeSession open(TelemetrySessionId sessionId) {
    return new Session();
  }

  private final class Session implements TelemetryProbeSession {
    private Long baseline = heapUsedBytes();

    @Override
    public synchronized void rebase() {
      baseline = heapUsedBytes();
    }

    @Override
    public synchronized Map<String, TelemetryValue> sample() {
      Long current = heapUsedBytes();
      if (current == null) return Map.of();
      Map<String, TelemetryValue> values = new LinkedHashMap<>();
      values.put(MemoryMetricIds.HEAP_USED_BYTES, TelemetryValue.of(current));
      if (baseline != null) values.put(MemoryMetricIds.HEAP_DELTA_BYTES, TelemetryValue.of(current - baseline));
      return Map.copyOf(values);
    }
  }

  private Long heapUsedBytes() {
    if (memoryBean == null) return null;
    try {
      long used = memoryBean.getHeapMemoryUsage().getUsed();
      return used < 0L ? null : used;
    } catch (RuntimeException ignored) {
      return null;
    }
  }
}
