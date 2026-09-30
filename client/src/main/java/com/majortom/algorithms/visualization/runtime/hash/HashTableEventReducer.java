package com.majortom.algorithms.visualization.runtime.hash;

import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.event.structure.HashStructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.snapshot.HashTableSnapshot;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Reduces factual hash-table mutations into an immutable bucket model. */
public final class HashTableEventReducer implements EventReducer<HashTableViewState> {
  private final HashTableViewState initial;

  public HashTableEventReducer(int initialCapacity) {
    this.initial = HashTableViewState.empty(initialCapacity);
  }

  public HashTableEventReducer(HashTableSnapshot<?, ?> snapshot) {
    this.initial = HashTableViewState.fromSnapshot(snapshot);
  }

  public HashTableEventReducer(HashTableViewState initial) {
    this.initial = Objects.requireNonNull(initial, "initial");
  }

  @Override
  public HashTableViewState initialState() {
    return initial;
  }

  @Override
  public Reduction<HashTableViewState> reduce(
      HashTableViewState previous, EventEnvelope envelope) {
    Object event = envelope.event();

    if (event instanceof HashStructureEvent.EntryInserted inserted) {
      Located existing = find(previous, inserted.key());
      long id = existing == null ? nextId(previous) : existing.entry().id();
      List<HashTableViewState.Bucket> buckets = mutableBuckets(previous);
      if (existing != null) {
        removeEntry(buckets, existing.bucketIndex(), existing.entryIndex());
      }
      addEntry(buckets, inserted.bucketIndex(), new HashTableViewState.Entry(
          id, VisualValue.of(inserted.key()), VisualValue.of(inserted.value())));
      return changed(state(previous.capacity(), buckets,
          HashTableViewState.Mutation.inserted(id, inserted.bucketIndex())));
    }

    if (event instanceof HashStructureEvent.EntryUpdated updated) {
      Located located = find(previous, updated.key());
      if (located == null) {
        long id = nextId(previous);
        List<HashTableViewState.Bucket> buckets = mutableBuckets(previous);
        addEntry(buckets, updated.bucketIndex(), new HashTableViewState.Entry(
            id, VisualValue.of(updated.key()), VisualValue.of(updated.value())));
        return changed(state(previous.capacity(), buckets,
            HashTableViewState.Mutation.inserted(id, updated.bucketIndex())));
      }
      List<HashTableViewState.Bucket> buckets = mutableBuckets(previous);
      removeEntry(buckets, located.bucketIndex(), located.entryIndex());
      addEntry(buckets, updated.bucketIndex(), new HashTableViewState.Entry(
          located.entry().id(), VisualValue.of(updated.key()), VisualValue.of(updated.value())));
      return changed(state(previous.capacity(), buckets,
          HashTableViewState.Mutation.updated(
              located.entry().id(), updated.bucketIndex(), located.bucketIndex())));
    }

    if (event instanceof HashStructureEvent.EntryRemoved removed) {
      Located located = find(previous, removed.key());
      if (located == null) {
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
      }
      List<HashTableViewState.Bucket> buckets = mutableBuckets(previous);
      removeEntry(buckets, located.bucketIndex(), located.entryIndex());
      return changed(state(previous.capacity(), buckets,
          HashTableViewState.Mutation.removed(located.entry().id(), located.bucketIndex())));
    }

    if (event instanceof HashStructureEvent.Rehashed rehashed) {
      List<List<HashTableViewState.Entry>> entries = new ArrayList<>(rehashed.capacity());
      for (int index = 0; index < rehashed.capacity(); index++) {
        entries.add(new ArrayList<>());
      }
      long next = nextId(previous);
      for (HashStructureEvent.EntryPlacement placement : rehashed.entries()) {
        Located existing = find(previous, placement.key());
        long id = existing == null ? next++ : existing.entry().id();
        entries.get(placement.bucketIndex()).add(new HashTableViewState.Entry(
            id, VisualValue.of(placement.key()), VisualValue.of(placement.value())));
      }
      List<HashTableViewState.Bucket> buckets = new ArrayList<>(rehashed.capacity());
      for (int index = 0; index < rehashed.capacity(); index++) {
        buckets.add(new HashTableViewState.Bucket(index, entries.get(index)));
      }
      return changed(state(
          rehashed.capacity(), buckets, HashTableViewState.Mutation.rehashed()));
    }

    if (event instanceof RunCompletedEvent) {
      return Reduction.changed(new HashTableViewState(
              previous.capacity(), previous.buckets(), HashTableViewState.Mutation.none(), true),
          EventImportance.TERMINAL, true);
    }

    return Reduction.unchanged(previous, EventImportance.TRANSIENT);
  }

  private static HashTableViewState state(
      int capacity,
      List<HashTableViewState.Bucket> buckets,
      HashTableViewState.Mutation mutation) {
    return new HashTableViewState(capacity, buckets, mutation, false);
  }

  private static List<HashTableViewState.Bucket> mutableBuckets(HashTableViewState state) {
    List<HashTableViewState.Bucket> result = new ArrayList<>(state.capacity());
    for (HashTableViewState.Bucket bucket : state.buckets()) {
      result.add(new HashTableViewState.Bucket(bucket.index(), new ArrayList<>(bucket.entries())));
    }
    return result;
  }

  private static void addEntry(
      List<HashTableViewState.Bucket> buckets,
      int bucketIndex,
      HashTableViewState.Entry entry) {
    List<HashTableViewState.Entry> entries = copyEntries(buckets, bucketIndex);
    entries.add(entry);
    buckets.set(bucketIndex, new HashTableViewState.Bucket(bucketIndex, entries));
  }

  private static void removeEntry(
      List<HashTableViewState.Bucket> buckets, int bucketIndex, int entryIndex) {
    List<HashTableViewState.Entry> entries = copyEntries(buckets, bucketIndex);
    entries.remove(entryIndex);
    buckets.set(bucketIndex, new HashTableViewState.Bucket(bucketIndex, entries));
  }

  private static List<HashTableViewState.Entry> copyEntries(
      List<HashTableViewState.Bucket> buckets, int bucketIndex) {
    if (bucketIndex < 0 || bucketIndex >= buckets.size()) {
      throw new IllegalArgumentException("bucket index outside capacity: " + bucketIndex);
    }
    return new ArrayList<>(buckets.get(bucketIndex).entries());
  }

  private static Located find(HashTableViewState state, Object key) {
    for (HashTableViewState.Bucket bucket : state.buckets()) {
      for (int index = 0; index < bucket.entries().size(); index++) {
        HashTableViewState.Entry entry = bucket.entries().get(index);
        if (Objects.equals(entry.key().value(), key)) {
          return new Located(bucket.index(), index, entry);
        }
      }
    }
    return null;
  }

  private static long nextId(HashTableViewState state) {
    return state.entries().stream().mapToLong(HashTableViewState.Entry::id).max().orElse(0L) + 1L;
  }

  private static Reduction<HashTableViewState> changed(HashTableViewState state) {
    return Reduction.changed(state, EventImportance.STATE_CHANGE, true);
  }

  private record Located(
      int bucketIndex, int entryIndex, HashTableViewState.Entry entry) {}
}
