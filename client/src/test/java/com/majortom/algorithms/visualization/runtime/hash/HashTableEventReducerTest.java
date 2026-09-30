package com.majortom.algorithms.visualization.runtime.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.event.structure.HashStructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class HashTableEventReducerTest {

  @Test
  void preservesEntryIdentityAcrossUpdateAndRehash() {
    HashTableEventReducer reducer = new HashTableEventReducer(4);
    HashTableViewState state = reducer.initialState();

    state = reduce(reducer, state, 1,
        new HashStructureEvent.EntryInserted(1, "alpha", 10));
    long alphaId = state.buckets().get(1).entries().getFirst().id();

    state = reduce(reducer, state, 2,
        new HashStructureEvent.EntryUpdated(2, "alpha", 10, 20));
    HashTableViewState.Entry updated = state.buckets().get(2).entries().getFirst();
    assertEquals(alphaId, updated.id());
    assertEquals(20, updated.value().value());

    state = reduce(reducer, state, 3,
        new HashStructureEvent.Rehashed(4, 8, List.of(
            new HashStructureEvent.EntryPlacement(6, "alpha", 20),
            new HashStructureEvent.EntryPlacement(3, "beta", 30))));

    HashTableViewState.Entry alpha = find(state, "alpha");
    HashTableViewState.Entry beta = find(state, "beta");
    assertNotNull(alpha);
    assertNotNull(beta);
    assertEquals(alphaId, alpha.id());
    assertEquals(8, state.capacity());
    assertEquals(2, state.size());
    assertEquals(6, bucketOf(state, "alpha"));
    assertEquals(3, bucketOf(state, "beta"));
  }

  @Test
  void remainsIdempotentWhenRehashArrivesBeforeInsertEvent() {
    HashTableEventReducer reducer = new HashTableEventReducer(4);
    HashTableViewState state = reducer.initialState();

    state = reduce(reducer, state, 1,
        new HashStructureEvent.Rehashed(4, 8, List.of(
            new HashStructureEvent.EntryPlacement(5, "alpha", 10))));
    long id = find(state, "alpha").id();

    state = reduce(reducer, state, 2,
        new HashStructureEvent.EntryInserted(5, "alpha", 10));

    assertEquals(1, state.size());
    assertEquals(id, find(state, "alpha").id());
    assertEquals(5, bucketOf(state, "alpha"));
  }

  @Test
  void removesEntryByFactualKey() {
    HashTableEventReducer reducer = new HashTableEventReducer(4);
    HashTableViewState state = reducer.initialState();
    state = reduce(reducer, state, 1,
        new HashStructureEvent.EntryInserted(0, "alpha", 10));
    state = reduce(reducer, state, 2,
        new HashStructureEvent.EntryInserted(0, "beta", 20));

    state = reduce(reducer, state, 3,
        new HashStructureEvent.EntryRemoved(0, "alpha", 10));

    assertEquals(1, state.size());
    assertEquals("beta", state.buckets().get(0).entries().getFirst().key().value());
    assertEquals(HashTableViewState.Type.REMOVED, state.mutation().type());
  }

  private static HashTableViewState reduce(
      HashTableEventReducer reducer,
      HashTableViewState state,
      long sequence,
      ExecutionEvent event) {
    return reducer.reduce(state, envelope(sequence, event)).state();
  }

  private static EventEnvelope envelope(long sequence, ExecutionEvent event) {
    return new EventEnvelope(
        "test-run", "hash-test", sequence, Instant.EPOCH.plusMillis(sequence),
        "test", event);
  }

  private static HashTableViewState.Entry find(HashTableViewState state, Object key) {
    return state.entries().stream()
        .filter(entry -> java.util.Objects.equals(entry.key().value(), key))
        .findFirst()
        .orElse(null);
  }

  private static int bucketOf(HashTableViewState state, Object key) {
    return state.buckets().stream()
        .filter(bucket -> bucket.entries().stream()
            .anyMatch(entry -> java.util.Objects.equals(entry.key().value(), key)))
        .map(HashTableViewState.Bucket::index)
        .findFirst()
        .orElse(-1);
  }
}
