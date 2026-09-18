package com.majortom.algorithms.core.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;

/**
 * Opt-in JFR allocation sampler for deep diagnostics.
 *
 * <p>This deliberately does not replace {@link JdkMemoryProfiler}: ObjectAllocationSample.weight is
 * a sampled estimate. Exact per-execution cumulative allocation remains the ThreadMXBean metric.</p>
 */
public final class JfrAllocationAnalyzer {
  private static final String ALLOCATION_EVENT = "jdk.ObjectAllocationSample";
  private static final int TOP_LIMIT = 8;
  private static final ExecutorService ANALYSIS_EXECUTOR =
      Executors.newSingleThreadExecutor(new AnalyzerThreadFactory());

  private JfrAllocationAnalyzer() {}

  public static boolean isAvailable() {
    return ModuleLayer.boot().findModule("jdk.jfr").isPresent();
  }

  public static MemoryDeepProfileSession begin(MemoryDomain domain, String scopeId) {
    Objects.requireNonNull(domain, "domain");
    if (!isAvailable() || Thread.currentThread().isVirtual()) {
      return new UnsupportedSession(domain, scopeId);
    }
    try {
      return new Session(domain, scopeId, Thread.currentThread().threadId());
    } catch (RuntimeException exception) {
      return new UnsupportedSession(domain, scopeId);
    }
  }

  private static final class Session implements MemoryDeepProfileSession {
    private final MemoryDomain domain;
    private final String scopeId;
    private final long threadId;
    private final Recording recording;
    private Instant startedAt;
    private Instant endedAt;
    private boolean closed;
    private CompletionStage<MemoryDeepProfile> completion;

    private Session(MemoryDomain domain, String scopeId, long threadId) {
      this.domain = domain;
      this.scopeId = Objects.requireNonNullElse(scopeId, "");
      this.threadId = threadId;
      this.recording = new Recording();
      recording.setName("algorithms-memory-" + domain.name().toLowerCase());
      recording.enable(ALLOCATION_EVENT).withStackTrace();
      recording.start();
    }

    @Override
    public synchronized void markExecutionStart() {
      if (!closed && startedAt == null) {
        startedAt = Instant.now();
      }
    }

    @Override
    public synchronized void markExecutionEnd() {
      if (!closed && endedAt == null) {
        endedAt = Instant.now();
      }
    }

    @Override
    public synchronized void close() {
      if (closed) {
        return;
      }
      if (startedAt == null) {
        startedAt = Instant.now();
      }
      if (endedAt == null) {
        endedAt = Instant.now();
      }
      closed = true;
      Instant measurementStart = startedAt;
      Instant measurementEnd = endedAt;
      completion = CompletableFuture.supplyAsync(
          () -> finishRecording(recording, domain, scopeId, threadId, measurementStart, measurementEnd),
          ANALYSIS_EXECUTOR);
    }

    @Override
    public synchronized CompletionStage<MemoryDeepProfile> completion() {
      if (completion != null) {
        return completion;
      }
      return CompletableFuture.completedFuture(emptyProfile(domain, scopeId));
    }
  }

  private static final class UnsupportedSession implements MemoryDeepProfileSession {
    private final MemoryDeepProfile profile;

    private UnsupportedSession(MemoryDomain domain, String scopeId) {
      this.profile = emptyProfile(domain, scopeId);
    }

    @Override
    public void markExecutionStart() {}

    @Override
    public void markExecutionEnd() {}

    @Override
    public void close() {}

    @Override
    public CompletionStage<MemoryDeepProfile> completion() {
      return CompletableFuture.completedFuture(profile);
    }
  }

  private static MemoryDeepProfile finishRecording(
      Recording recording,
      MemoryDomain domain,
      String scopeId,
      long threadId,
      Instant startedAt,
      Instant endedAt) {
    Path file = null;
    try {
      recording.stop();
      file = Files.createTempFile("algorithms-memory-", ".jfr");
      recording.dump(file);
      return readProfile(file, domain, scopeId, threadId, startedAt, endedAt);
    } catch (IOException | RuntimeException exception) {
      return emptyProfile(domain, scopeId);
    } finally {
      recording.close();
      if (file != null) {
        try {
          Files.deleteIfExists(file);
        } catch (IOException ignored) {
          // Temporary diagnostic data is best-effort cleanup only.
        }
      }
    }
  }

  private static MemoryDeepProfile readProfile(
      Path file, MemoryDomain domain, String scopeId, long threadId, Instant startedAt, Instant endedAt)
      throws IOException {
    Map<String, MutableStat> types = new HashMap<>();
    Map<String, MutableStat> sites = new HashMap<>();
    long totalBytes = 0L;
    long totalSamples = 0L;
    for (RecordedEvent event : RecordingFile.readAllEvents(file)) {
      if (!ALLOCATION_EVENT.equals(event.getEventType().getName())) {
        continue;
      }
      RecordedThread thread = event.getThread();
      Instant eventTime = event.getStartTime();
      if (thread == null || thread.getJavaThreadId() != threadId
          || eventTime.isBefore(startedAt) || eventTime.isAfter(endedAt)) {
        continue;
      }
      long weight = Math.max(0L, event.getLong("weight"));
      totalBytes = saturatingAdd(totalBytes, weight);
      totalSamples++;

      RecordedClass allocatedClass = event.getClass("objectClass");
      String className = allocatedClass == null ? "unknown" : readableClassName(allocatedClass.getName());
      types.computeIfAbsent(className, ignored -> new MutableStat()).add(weight);

      String site = allocationSite(event.getStackTrace());
      sites.computeIfAbsent(site, ignored -> new MutableStat()).add(weight);
    }
    return new MemoryDeepProfile(
        domain,
        scopeId,
        totalBytes,
        totalSamples,
        topTypes(types),
        topSites(sites));
  }

  private static List<MemoryAllocationTypeStat> topTypes(Map<String, MutableStat> source) {
    return source.entrySet().stream()
        .sorted(statComparator())
        .limit(TOP_LIMIT)
        .map(entry -> new MemoryAllocationTypeStat(
            entry.getKey(), entry.getValue().bytes, entry.getValue().samples))
        .toList();
  }

  private static List<MemoryAllocationSiteStat> topSites(Map<String, MutableStat> source) {
    return source.entrySet().stream()
        .sorted(statComparator())
        .limit(TOP_LIMIT)
        .map(entry -> new MemoryAllocationSiteStat(
            entry.getKey(), entry.getValue().bytes, entry.getValue().samples))
        .toList();
  }

  private static Comparator<Map.Entry<String, MutableStat>> statComparator() {
    return Comparator.<Map.Entry<String, MutableStat>>comparingLong(entry -> entry.getValue().bytes)
        .reversed()
        .thenComparing(Map.Entry::getKey);
  }

  private static String allocationSite(RecordedStackTrace trace) {
    if (trace == null) {
      return "unknown";
    }
    List<RecordedFrame> frames = trace.getFrames();
    if (frames == null || frames.isEmpty()) {
      return "unknown";
    }
    for (RecordedFrame frame : frames) {
      RecordedMethod method = frame.getMethod();
      if (method == null || method.getType() == null) {
        continue;
      }
      String type = method.getType().getName();
      if (type != null && type.startsWith("com.majortom.algorithms.")) {
        return formatFrame(type, method.getName(), frame.getLineNumber());
      }
    }
    RecordedFrame frame = frames.getFirst();
    RecordedMethod method = frame.getMethod();
    if (method == null || method.getType() == null) {
      return "unknown";
    }
    return formatFrame(method.getType().getName(), method.getName(), frame.getLineNumber());
  }

  private static String formatFrame(String type, String method, int line) {
    String simpleType = type == null ? "unknown" : type.replace('/', '.');
    String value = simpleType + "." + Objects.requireNonNullElse(method, "?") + "()";
    return line > 0 ? value + ":" + line : value;
  }

  private static String readableClassName(String name) {
    if (name == null || name.isBlank()) {
      return "unknown";
    }
    if (!name.startsWith("[")) {
      return name.replace('/', '.');
    }
    int dimensions = 0;
    while (dimensions < name.length() && name.charAt(dimensions) == '[') {
      dimensions++;
    }
    if (dimensions >= name.length()) {
      return name;
    }
    String component = switch (name.charAt(dimensions)) {
      case 'Z' -> "boolean";
      case 'B' -> "byte";
      case 'C' -> "char";
      case 'S' -> "short";
      case 'I' -> "int";
      case 'J' -> "long";
      case 'F' -> "float";
      case 'D' -> "double";
      case 'L' -> {
        int end = name.endsWith(";") ? name.length() - 1 : name.length();
        yield name.substring(dimensions + 1, end).replace('/', '.');
      }
      default -> name.substring(dimensions).replace('/', '.');
    };
    return component + "[]".repeat(dimensions);
  }

  private static MemoryDeepProfile emptyProfile(MemoryDomain domain, String scopeId) {
    return new MemoryDeepProfile(domain, scopeId, 0L, 0L, List.of(), List.of());
  }

  private static long saturatingAdd(long left, long right) {
    if (right <= 0L) {
      return left;
    }
    if (Long.MAX_VALUE - left < right) {
      return Long.MAX_VALUE;
    }
    return left + right;
  }

  private static final class MutableStat {
    private long bytes;
    private long samples;

    private void add(long value) {
      bytes = saturatingAdd(bytes, value);
      samples++;
    }
  }
  private static final class AnalyzerThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable task) {
      Thread thread = new Thread(task, "jfr-memory-analysis");
      thread.setDaemon(true);
      return thread;
    }
  }

}
