package com.majortom.algorithms.core.event.structure;

import com.majortom.algorithms.core.statistics.MetricKeys;
import com.majortom.algorithms.core.statistics.StatisticsContribution;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Factual hash-table mutations. Collision strategy remains an implementation detail. */
public sealed interface HashStructureEvent
    extends StructureEvent, StatisticsContribution
    permits HashStructureEvent.EntryInserted, HashStructureEvent.EntryUpdated,
            HashStructureEvent.EntryRemoved, HashStructureEvent.Rehashed {

  record EntryInserted(int bucketIndex, Object key, Object value) implements HashStructureEvent {
    public EntryInserted {
      requireBucket(bucketIndex);
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(value, "value");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.INSERTIONS, 1L, MetricKeys.WRITES, 1L);
    }
  }

  record EntryUpdated(int bucketIndex, Object key, Object previousValue, Object value)
      implements HashStructureEvent {
    public EntryUpdated {
      requireBucket(bucketIndex);
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(previousValue, "previousValue");
      Objects.requireNonNull(value, "value");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.UPDATES, 1L, MetricKeys.WRITES, 1L);
    }
  }

  record EntryRemoved(int bucketIndex, Object key, Object value) implements HashStructureEvent {
    public EntryRemoved {
      requireBucket(bucketIndex);
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(value, "value");
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.REMOVALS, 1L);
    }
  }

  /** Complete factual placement after a capacity change / rehash. */
  record Rehashed(int previousCapacity, int capacity, List<EntryPlacement> entries)
      implements HashStructureEvent {
    public Rehashed {
      if (previousCapacity < 1 || capacity < 1) {
        throw new IllegalArgumentException("capacity must be positive");
      }
      entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
      for (EntryPlacement entry : entries) {
        if (entry.bucketIndex() >= capacity) {
          throw new IllegalArgumentException("bucket index outside capacity: " + entry.bucketIndex());
        }
      }
    }

    @Override
    public Map<String, Long> metricDeltas() {
      return Map.of(MetricKeys.HASH_REHASHES, 1L);
    }
  }

  record EntryPlacement(int bucketIndex, Object key, Object value) {
    public EntryPlacement {
      requireBucket(bucketIndex);
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(value, "value");
    }
  }

  private static void requireBucket(int bucketIndex) {
    if (bucketIndex < 0) {
      throw new IllegalArgumentException("bucketIndex must not be negative");
    }
  }
}
