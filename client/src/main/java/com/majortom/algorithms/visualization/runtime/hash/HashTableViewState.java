package com.majortom.algorithms.visualization.runtime.hash;

import com.majortom.algorithms.core.snapshot.HashTableSnapshot;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Immutable JavaFX-neutral hash-table facts grouped by factual bucket index. */
public record HashTableViewState(
    int capacity,
    List<Bucket> buckets,
    Mutation mutation,
    boolean completed) {

  public HashTableViewState {
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity must be positive");
    }
    buckets = normalizeBuckets(capacity, buckets);
    mutation = Objects.requireNonNull(mutation, "mutation");
  }

  public static HashTableViewState empty(int capacity) {
    return new HashTableViewState(capacity, emptyBuckets(capacity), Mutation.none(), false);
  }

  public static HashTableViewState fromSnapshot(HashTableSnapshot<?, ?> snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    List<List<Entry>> entries = new ArrayList<>(snapshot.capacity());
    for (int index = 0; index < snapshot.capacity(); index++) {
      entries.add(new ArrayList<>());
    }
    long id = 1L;
    for (HashTableSnapshot.Entry<?, ?> entry : snapshot.entries()) {
      entries.get(entry.bucketIndex()).add(
          new Entry(id++, VisualValue.of(entry.key()), VisualValue.of(entry.value())));
    }
    List<Bucket> buckets = new ArrayList<>(snapshot.capacity());
    for (int index = 0; index < snapshot.capacity(); index++) {
      buckets.add(new Bucket(index, entries.get(index)));
    }
    return new HashTableViewState(snapshot.capacity(), buckets, Mutation.none(), false);
  }

  public int size() {
    return buckets.stream().mapToInt(bucket -> bucket.entries().size()).sum();
  }

  public List<Entry> entries() {
    return buckets.stream().flatMap(bucket -> bucket.entries().stream()).toList();
  }

  public record Bucket(int index, List<Entry> entries) {
    public Bucket {
      if (index < 0) {
        throw new IllegalArgumentException("bucket index must not be negative");
      }
      entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }
  }

  public record Entry(long id, VisualValue key, VisualValue value) {
    public Entry {
      if (id <= 0L) {
        throw new IllegalArgumentException("entry id must be positive");
      }
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(value, "value");
    }
  }

  public record Mutation(
      Type type,
      long entryId,
      int bucketIndex,
      int previousBucketIndex) {
    public Mutation {
      Objects.requireNonNull(type, "type");
    }

    public static Mutation none() {
      return new Mutation(Type.NONE, -1L, -1, -1);
    }

    public static Mutation inserted(long entryId, int bucketIndex) {
      return new Mutation(Type.INSERTED, entryId, bucketIndex, -1);
    }

    public static Mutation updated(long entryId, int bucketIndex, int previousBucketIndex) {
      return new Mutation(Type.UPDATED, entryId, bucketIndex, previousBucketIndex);
    }

    public static Mutation removed(long entryId, int bucketIndex) {
      return new Mutation(Type.REMOVED, entryId, bucketIndex, bucketIndex);
    }

    public static Mutation rehashed() {
      return new Mutation(Type.REHASHED, -1L, -1, -1);
    }
  }

  public enum Type {
    NONE,
    INSERTED,
    UPDATED,
    REMOVED,
    REHASHED
  }

  private static List<Bucket> normalizeBuckets(int capacity, List<Bucket> input) {
    Objects.requireNonNull(input, "buckets");
    List<Bucket> normalized = emptyBuckets(capacity);
    for (Bucket bucket : input) {
      if (bucket.index() >= capacity) {
        throw new IllegalArgumentException("bucket index outside capacity: " + bucket.index());
      }
      normalized.set(bucket.index(), new Bucket(bucket.index(),
          bucket.entries().stream().sorted(Comparator.comparingLong(Entry::id)).toList()));
    }
    return List.copyOf(normalized);
  }

  private static List<Bucket> emptyBuckets(int capacity) {
    List<Bucket> result = new ArrayList<>(capacity);
    for (int index = 0; index < capacity; index++) {
      result.add(new Bucket(index, List.of()));
    }
    return result;
  }
}
