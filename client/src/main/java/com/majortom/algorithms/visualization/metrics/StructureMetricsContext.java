package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.event.structure.StructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Read-only structure-history context available to structure metric providers. */
public record StructureMetricsContext(String structureId, List<EventEnvelope> events) {
  public StructureMetricsContext {
    Objects.requireNonNull(structureId, "structureId");
    events = List.copyOf(Objects.requireNonNull(events, "events"));
  }

  public long eventCount() {
    return events.stream().filter(event -> event.event() instanceof StructureEvent).count();
  }

  public long operationCount() {
    Set<Object> runs = new HashSet<>();
    for (EventEnvelope event : events) {
      if (event.event() instanceof StructureEvent) runs.add(event.runId());
    }
    return runs.size();
  }

  public long count(Class<? extends StructureEvent> eventType) {
    Objects.requireNonNull(eventType, "eventType");
    return events.stream().filter(event -> eventType.isInstance(event.event())).count();
  }
}
