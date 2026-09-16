package com.majortom.algorithms.visualization.render.diagnostics;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class RenderTrace {
  private final Map<RenderSessionId, AtomicLong> layoutCounts = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<Entry> entries = new CopyOnWriteArrayList<>();

  public void stage(RenderSessionId sessionId, long transactionId, String stage, long generation,
      long modelRevision) {
    entries.add(
        new Entry(System.nanoTime(), sessionId, transactionId, stage, generation, modelRevision));
  }

  public void layoutInvoked(RenderSessionId sessionId) {
    layoutCounts.computeIfAbsent(sessionId, ignored -> new AtomicLong()).incrementAndGet();
  }

  public long layoutInvocationCount(RenderSessionId sessionId) {
    AtomicLong count = layoutCounts.get(sessionId);
    return count == null ? 0L : count.get();
  }

  public List<Entry> entries(RenderSessionId sessionId) {
    List<Entry> result = new ArrayList<>();
    for (Entry entry : entries)
      if (entry.sessionId().equals(sessionId))
        result.add(entry);
    return List.copyOf(result);
  }

  public record Entry(long nanoTime, RenderSessionId sessionId, long transactionId, String stage,
      long generation, long modelRevision) {}
}
