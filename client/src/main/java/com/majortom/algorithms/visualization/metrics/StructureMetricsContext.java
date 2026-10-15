package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.event.structure.StructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Read-only summary of one structure-history snapshot.
 *
 * <p>Count once when events change; UI metric refreshes must not rescan the
 * complete history for every requested event subclass.</p>
 */
public final class StructureMetricsContext {
  private final String structureId;
  private final List<EventEnvelope> events;
  private final Map<Class<?>, Long> eventTypes;
  private final long eventCount;
  private final long operationCount;

  public StructureMetricsContext(String structureId, List<EventEnvelope> events) {
    this.structureId = Objects.requireNonNull(structureId, "structureId");
    this.events = List.copyOf(Objects.requireNonNull(events, "events"));

    Map<Class<?>, Long> types = new HashMap<>();
    Set<String> runs = new HashSet<>();
    long total = 0L;
    for (EventEnvelope envelope : this.events) {
      if (envelope.event() instanceof StructureEvent event) {
        total++;
        runs.add(envelope.runId());
        types.merge(event.getClass(), 1L, Long::sum);
      }
    }
    eventCount = total;
    operationCount = runs.size();
    eventTypes = Map.copyOf(types);
  }

  public String structureId() {
    return structureId;
  }

  public List<EventEnvelope> events() {
    return events;
  }

  public long eventCount() {
    return eventCount;
  }

  public long operationCount() {
    return operationCount;
  }

  /** Compatible with the original isInstance semantics, including event superclasses. */
  public long count(Class<? extends StructureEvent> eventType) {
    Objects.requireNonNull(eventType, "eventType");
    long total = 0L;
    for (var entry : eventTypes.entrySet()) {
      if (eventType.isAssignableFrom(entry.getKey())) total += entry.getValue();
    }
    return total;
  }
}
