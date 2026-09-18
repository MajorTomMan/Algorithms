package com.majortom.algorithms.core.event.structure;

import com.majortom.algorithms.core.statistics.MetricKeys;
import com.majortom.algorithms.core.statistics.StatisticsContribution;
import java.util.Map;

public sealed interface ArrayStructureEvent
    extends StructureEvent, StatisticsContribution permits ArrayStructureEvent.Inserted,
            ArrayStructureEvent.Removed, ArrayStructureEvent.Updated, ArrayStructureEvent.Swapped {
  record Inserted(int index, Object value) implements ArrayStructureEvent {
    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.INSERTIONS, 1L, MetricKeys.WRITES, 1L);
    }
  }
  record Removed(int index, Object value) implements ArrayStructureEvent {
    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.REMOVALS, 1L);
    }
  }
  record Updated(int index, Object previousValue, Object value) implements ArrayStructureEvent {
    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.UPDATES, 1L, MetricKeys.WRITES, 1L);
    }
  }
  record Swapped(int leftIndex, int rightIndex, Object leftValue, Object rightValue)
      implements ArrayStructureEvent {
    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.SWAPS, 1L, MetricKeys.WRITES, 2L);
    }
  }
}
