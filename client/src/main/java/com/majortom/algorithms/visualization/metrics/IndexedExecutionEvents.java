package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * UI-thread-confined event view with incremental class counters for live metrics.
 *
 * <p>It avoids a full List.copyOf of the authoritative event stream and lets
 * structure-specific metrics count events without rescanning the full history
 * on every 50 ms statistics refresh. Public history APIs still return snapshots.</p>
 */
public final class IndexedExecutionEvents extends AbstractList<EventEnvelope>
    implements RandomAccess {
  private final List<EventEnvelope> events = new ArrayList<>();
  private final Map<Class<?>, Long> counts = new HashMap<>();

  public void appendAll(List<EventEnvelope> batch) {
    Objects.requireNonNull(batch, "batch");
    for (EventEnvelope envelope : batch) {
      EventEnvelope event = Objects.requireNonNull(envelope, "event");
      events.add(event);
      counts.merge(event.event().getClass(), 1L, Long::sum);
    }
    modCount++;
  }

  public void replaceWith(List<EventEnvelope> authoritativeEvents) {
    clear();
    appendAll(authoritativeEvents);
  }

  @Override
  public void clear() {
    events.clear();
    counts.clear();
    modCount++;
  }

  /** Counts assignable event types, retaining the original isInstance semantics. */
  public long count(Class<?> eventType) {
    Objects.requireNonNull(eventType, "eventType");
    long total = 0L;
    for (Map.Entry<Class<?>, Long> entry : counts.entrySet()) {
      if (eventType.isAssignableFrom(entry.getKey()))
        total += entry.getValue();
    }
    return total;
  }

  @Override
  public EventEnvelope get(int index) {
    return events.get(index);
  }

  @Override
  public int size() {
    return events.size();
  }
}
