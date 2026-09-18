package com.majortom.algorithms.core.memory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cross-platform HotSpot/OpenJDK memory profiler built only on JDK management APIs.
 *
 * <p>Thread allocation is the authoritative execution-attribution signal. Heap delta is deliberately
 * retained only as a whole-JVM reference measurement because garbage collection and unrelated JVM
 * activity can make it negative or otherwise unrelated to the profiled scope.</p>
 */
public final class JdkMemoryProfiler implements MemoryProfiler {
  public static final long DEFAULT_SAMPLE_INTERVAL_MILLIS = 100L;

  private static final JdkMemoryProfiler SHARED = new JdkMemoryProfiler();

  private final com.sun.management.ThreadMXBean threadBean;
  private final MemoryMXBean memoryBean;
  private final List<GarbageCollectorMXBean> garbageCollectors;
  private final MemoryCapabilities capabilities;
  private final ScheduledExecutorService scheduler;
  private final AtomicLong sequence = new AtomicLong();
  private final long sampleIntervalMillis;

  public static JdkMemoryProfiler shared() {
    return SHARED;
  }

  public JdkMemoryProfiler() {
    this(DEFAULT_SAMPLE_INTERVAL_MILLIS);
  }

  JdkMemoryProfiler(long sampleIntervalMillis) {
    if (sampleIntervalMillis < 1L) {
      throw new IllegalArgumentException("sampleIntervalMillis must be positive");
    }
    this.sampleIntervalMillis = sampleIntervalMillis;
    this.threadBean = threadBean();
    this.memoryBean = memoryBean();
    this.garbageCollectors = List.copyOf(ManagementFactory.getGarbageCollectorMXBeans());
    boolean allocatedBytes = enableThreadAllocation(threadBean);
    this.capabilities = new MemoryCapabilities(
        allocatedBytes,
        memoryBean != null,
        !garbageCollectors.isEmpty(),
        ModuleLayer.boot().findModule("jdk.jfr").isPresent());
    this.scheduler = Executors.newSingleThreadScheduledExecutor(new SamplerThreadFactory());
  }

  @Override
  public MemoryCapabilities capabilities() {
    return capabilities;
  }

  @Override
  public MemoryProfileSession begin(MemoryDomain domain, String scopeId) {
    Objects.requireNonNull(domain, "domain");
    MemorySessionId id = new MemorySessionId(sequence.incrementAndGet(), domain, scopeId);
    Session session = new Session(id);
    session.start();
    return session;
  }

  private final class Session implements MemoryProfileSession {
    private final MemorySessionId id;
    private final Object lock = new Object();
    private final Map<Long, ThreadCounter> threadCounters = new LinkedHashMap<>();
    private final List<MemorySample> samples = new ArrayList<>();
    private final long startedAtNanos = System.nanoTime();
    private final Long heapBefore = heapUsedBytes();
    private final GcSnapshot gcBefore = gcSnapshot();

    private ScheduledFuture<?> samplingTask;
    private boolean closed;
    private MemoryProfile finalProfile;

    private Session(MemorySessionId id) {
      this.id = id;
    }

    private void start() {
      attachCurrentThread();
      if (capabilities.threadAllocatedBytes()) {
        samplingTask = scheduler.scheduleAtFixedRate(
            this::sampleSafely,
            sampleIntervalMillis,
            sampleIntervalMillis,
            TimeUnit.MILLISECONDS);
      }
      synchronized (lock) {
        rebaseAttachedThreadsLocked();
        samples.clear();
        if (!threadCounters.isEmpty()) {
          samples.add(new MemorySample(0L, 0L));
        }
      }
    }

    @Override
    public MemorySessionId id() {
      return id;
    }

    @Override
    public MemoryCapabilities capabilities() {
      return capabilities;
    }

    @Override
    public boolean attachCurrentThread() {
      if (!capabilities.threadAllocatedBytes() || Thread.currentThread().isVirtual()) {
        return false;
      }
      long threadId = Thread.currentThread().threadId();
      synchronized (lock) {
        if (closed) {
          return false;
        }
        ThreadCounter existing = threadCounters.get(threadId);
        if (existing != null) {
          return true;
        }
        ThreadCounter counter = new ThreadCounter(0L, 0L);
        threadCounters.put(threadId, counter);
        Long baseline = threadAllocatedBytes(threadId);
        if (baseline == null) {
          threadCounters.remove(threadId);
          return false;
        }
        counter.baselineBytes = baseline;
        counter.lastBytes = baseline;
        return true;
      }
    }

    @Override
    public MemoryProfile snapshot() {
      synchronized (lock) {
        if (finalProfile != null) {
          return finalProfile;
        }
        long elapsed = elapsedNanos();
        Long allocated = currentAllocatedBytesLocked();
        return buildProfileLocked(false, elapsed, allocated, false);
      }
    }

    @Override
    public void close() {
      synchronized (lock) {
        if (closed) {
          return;
        }
        long elapsed = elapsedNanos();
        Long allocated = currentAllocatedBytesLocked();
        closed = true;
        appendAllocationSampleLocked(elapsed, allocated);
        finalProfile = buildProfileLocked(true, elapsed, allocated, true);
      }
      ScheduledFuture<?> task = samplingTask;
      if (task != null) {
        task.cancel(false);
      }
    }

    private void sampleSafely() {
      try {
        synchronized (lock) {
          if (closed) {
            return;
          }
          long elapsed = elapsedNanos();
          appendAllocationSampleLocked(elapsed, currentAllocatedBytesLocked());
        }
      } catch (RuntimeException ignored) {
        // Profiling is best-effort and must never interfere with the execution being observed.
      }
    }

    private void appendAllocationSampleLocked(long elapsedNanos, Long allocated) {
      if (allocated == null) {
        return;
      }
      MemorySample next = new MemorySample(elapsedNanos, allocated);
      if (!samples.isEmpty()) {
        MemorySample previous = samples.getLast();
        if (previous.elapsedNanos() == elapsedNanos
            && previous.allocatedBytes() == allocated) {
          return;
        }
      }
      samples.add(next);
    }

    private void rebaseAttachedThreadsLocked() {
      for (Map.Entry<Long, ThreadCounter> entry : threadCounters.entrySet()) {
        Long current = threadAllocatedBytes(entry.getKey());
        if (current != null) {
          entry.getValue().baselineBytes = current;
          entry.getValue().lastBytes = current;
        }
      }
    }

    private Long currentAllocatedBytesLocked() {
      if (!capabilities.threadAllocatedBytes() || threadCounters.isEmpty()) {
        return null;
      }
      long total = 0L;
      boolean measured = false;
      for (Map.Entry<Long, ThreadCounter> entry : threadCounters.entrySet()) {
        ThreadCounter counter = entry.getValue();
        Long current = threadAllocatedBytes(entry.getKey());
        if (current != null && current >= counter.lastBytes) {
          counter.lastBytes = current;
        }
        if (counter.lastBytes >= counter.baselineBytes) {
          total = saturatingAdd(total, counter.lastBytes - counter.baselineBytes);
          measured = true;
        }
      }
      return measured ? total : null;
    }

    private MemoryProfile buildProfileLocked(
        boolean complete, long elapsed, Long allocated, boolean useRecordedFinalSample) {
      List<MemorySample> profileSamples = new ArrayList<>(samples);
      if (!useRecordedFinalSample && allocated != null) {
        MemorySample transientSample = new MemorySample(elapsed, allocated);
        if (profileSamples.isEmpty()
            || profileSamples.getLast().elapsedNanos() != transientSample.elapsedNanos()
            || profileSamples.getLast().allocatedBytes() != transientSample.allocatedBytes()) {
          profileSamples.add(transientSample);
        }
      }

      Long averageRate = rate(allocated, elapsed);
      Long peakRate = peakRate(profileSamples);
      Long heapDelta = delta(heapBefore, heapUsedBytes());
      GcSnapshot currentGc = gcSnapshot();
      Long gcCount = deltaNonNegative(gcBefore.collectionCount, currentGc.collectionCount);
      Long gcTime = deltaNonNegative(gcBefore.collectionTimeMillis, currentGc.collectionTimeMillis);

      return new MemoryProfile(
          id,
          capabilities,
          complete,
          true,
          elapsed,
          allocated,
          averageRate,
          peakRate,
          heapDelta,
          gcCount,
          gcTime,
          profileSamples);
    }

    private long elapsedNanos() {
      return Math.max(0L, System.nanoTime() - startedAtNanos);
    }
  }

  private Long threadAllocatedBytes(long threadId) {
    if (!capabilities.threadAllocatedBytes()) {
      return null;
    }
    try {
      long value = threadBean.getThreadAllocatedBytes(threadId);
      return value < 0L ? null : value;
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private Long heapUsedBytes() {
    if (memoryBean == null) {
      return null;
    }
    try {
      long used = memoryBean.getHeapMemoryUsage().getUsed();
      return used < 0L ? null : used;
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private GcSnapshot gcSnapshot() {
    long count = 0L;
    long time = 0L;
    boolean hasCount = false;
    boolean hasTime = false;
    for (GarbageCollectorMXBean collector : garbageCollectors) {
      try {
        long value = collector.getCollectionCount();
        if (value >= 0L) {
          count = saturatingAdd(count, value);
          hasCount = true;
        }
      } catch (RuntimeException ignored) {
        // Continue with any remaining collectors.
      }
      try {
        long value = collector.getCollectionTime();
        if (value >= 0L) {
          time = saturatingAdd(time, value);
          hasTime = true;
        }
      } catch (RuntimeException ignored) {
        // Continue with any remaining collectors.
      }
    }
    return new GcSnapshot(hasCount ? count : null, hasTime ? time : null);
  }

  private static com.sun.management.ThreadMXBean threadBean() {
    try {
      return ManagementFactory.getPlatformMXBean(com.sun.management.ThreadMXBean.class);
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static MemoryMXBean memoryBean() {
    try {
      return ManagementFactory.getMemoryMXBean();
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static boolean enableThreadAllocation(com.sun.management.ThreadMXBean bean) {
    if (bean == null || !bean.isThreadAllocatedMemorySupported()) {
      return false;
    }
    try {
      if (!bean.isThreadAllocatedMemoryEnabled()) {
        bean.setThreadAllocatedMemoryEnabled(true);
      }
      return bean.isThreadAllocatedMemoryEnabled();
    } catch (UnsupportedOperationException | SecurityException ignored) {
      return false;
    }
  }

  private static Long rate(Long bytes, long elapsedNanos) {
    if (bytes == null || elapsedNanos <= 0L) {
      return null;
    }
    double value = bytes.doubleValue() * 1_000_000_000d / elapsedNanos;
    if (value >= Long.MAX_VALUE) {
      return Long.MAX_VALUE;
    }
    return Math.max(0L, Math.round(value));
  }

  private static Long peakRate(List<MemorySample> samples) {
    if (samples.size() < 2) {
      return null;
    }
    long peak = 0L;
    boolean measured = false;
    for (int index = 1; index < samples.size(); index++) {
      MemorySample previous = samples.get(index - 1);
      MemorySample current = samples.get(index);
      long elapsed = current.elapsedNanos() - previous.elapsedNanos();
      long bytes = current.allocatedBytes() - previous.allocatedBytes();
      if (elapsed <= 0L || bytes < 0L) {
        continue;
      }
      Long currentRate = rate(bytes, elapsed);
      if (currentRate != null) {
        peak = Math.max(peak, currentRate);
        measured = true;
      }
    }
    return measured ? peak : null;
  }

  private static Long delta(Long before, Long after) {
    if (before == null || after == null) {
      return null;
    }
    return after - before;
  }

  private static Long deltaNonNegative(Long before, Long after) {
    if (before == null || after == null || after < before) {
      return null;
    }
    return after - before;
  }

  private static long saturatingAdd(long left, long right) {
    if (right > 0L && left > Long.MAX_VALUE - right) {
      return Long.MAX_VALUE;
    }
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

  private record GcSnapshot(Long collectionCount, Long collectionTimeMillis) {}

  private static final class SamplerThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable, "memory-profiler-sampler");
      thread.setDaemon(true);
      return thread;
    }
  }
}
