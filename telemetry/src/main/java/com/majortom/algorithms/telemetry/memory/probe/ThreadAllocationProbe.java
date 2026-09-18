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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exact execution-thread allocation attribution backed by HotSpot/OpenJDK ThreadMXBean. */
public final class ThreadAllocationProbe implements TelemetryProbe {
  private final com.sun.management.ThreadMXBean threadBean;
  private final boolean available;

  public ThreadAllocationProbe() {
    this.threadBean = threadBean();
    this.available = enableThreadAllocation(threadBean);
  }

  @Override
  public String id() {
    return "memory.thread-allocation";
  }

  @Override
  public TelemetryCapabilities capabilities() {
    return new TelemetryCapabilities(Map.of(MemoryCapabilityIds.THREAD_ALLOCATED_BYTES, available));
  }

  @Override
  public List<TelemetryMetricDescriptor> descriptors() {
    return List.of(new TelemetryMetricDescriptor(
        MemoryMetricIds.ALLOCATED_BYTES, "bytes", TelemetryMetricKind.CUMULATIVE));
  }

  @Override
  public TelemetryProbeSession open(TelemetrySessionId sessionId) {
    return new Session();
  }

  private final class Session implements TelemetryProbeSession {
    private final Map<Long, ThreadCounter> counters = new LinkedHashMap<>();

    @Override
    public synchronized boolean attachCurrentThread() {
      if (!available || Thread.currentThread().isVirtual()) return false;
      long threadId = Thread.currentThread().threadId();
      if (counters.containsKey(threadId)) return true;
      Long current = allocatedBytes(threadId);
      if (current == null) return false;
      counters.put(threadId, new ThreadCounter(current, current));
      return true;
    }

    @Override
    public synchronized void rebase() {
      counters.entrySet().removeIf(entry -> {
        Long current = allocatedBytes(entry.getKey());
        if (current == null) return true;
        entry.getValue().baselineBytes = current;
        entry.getValue().lastBytes = current;
        return false;
      });
    }

    @Override
    public synchronized Map<String, TelemetryValue> sample() {
      Long value = currentAllocatedBytes();
      return value == null ? Map.of() : Map.of(MemoryMetricIds.ALLOCATED_BYTES, TelemetryValue.of(value));
    }

    private Long currentAllocatedBytes() {
      if (!available || counters.isEmpty()) return null;
      long total = 0L;
      boolean measured = false;
      for (Map.Entry<Long, ThreadCounter> entry : counters.entrySet()) {
        ThreadCounter counter = entry.getValue();
        Long current = allocatedBytes(entry.getKey());
        if (current != null && current >= counter.lastBytes) counter.lastBytes = current;
        if (counter.lastBytes >= counter.baselineBytes) {
          total = saturatingAdd(total, counter.lastBytes - counter.baselineBytes);
          measured = true;
        }
      }
      return measured ? total : null;
    }
  }

  private Long allocatedBytes(long threadId) {
    if (!available) return null;
    try {
      long value = threadBean.getThreadAllocatedBytes(threadId);
      return value < 0L ? null : value;
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static com.sun.management.ThreadMXBean threadBean() {
    java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    return bean instanceof com.sun.management.ThreadMXBean extended ? extended : null;
  }

  private static boolean enableThreadAllocation(com.sun.management.ThreadMXBean bean) {
    if (bean == null || !bean.isThreadAllocatedMemorySupported()) return false;
    try {
      if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
      return bean.isThreadAllocatedMemoryEnabled();
    } catch (UnsupportedOperationException | SecurityException ignored) {
      return false;
    }
  }

  private static long saturatingAdd(long left, long right) {
    if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
    return left + right;
  }

  private static final class ThreadCounter {
    private long baselineBytes;
    private long lastBytes;

    private ThreadCounter(long baselineBytes, long lastBytes) {
      this.baselineBytes = baselineBytes;
      this.lastBytes = lastBytes;
    }
  }
}
