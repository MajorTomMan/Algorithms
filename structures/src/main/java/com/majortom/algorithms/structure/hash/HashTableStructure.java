package com.majortom.algorithms.structure.hash;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureModule;

/**
 * Strategy-neutral hash-table contract.
 *
 * <p>The Workbench deliberately does not expose probing, chaining, load thresholds or hashing
 * policy here. A concrete implementation owns those decisions. To participate in visualization and
 * replay, perform the real mutation first and then publish the resulting factual placement through
 * {@code StructureEvents.hashInserted/hashUpdated/hashRemoved}. When capacity changes, publish a
 * complete final placement with {@code StructureEvents.hashRehashed} as well. If an entry event
 * reports a bucket index that exists only in the new capacity (for example after growth), emit the
 * rehash event first; entry insertion/update reduction is key-idempotent, so the following entry
 * event will not duplicate the key.</p>
 *
 * <p>The family metadata stays implementation-free until a concrete table is ready. At that point
 * register the implementation on this {@link Structure} annotation in the same way as the other
 * structure families.</p>
 */
@Structure(name = "Hash Table", module = StructureModule.HASH)
public interface HashTableStructure<K, V> {
  int size();

  int capacity();

  default boolean isEmpty() {
    return size() == 0;
  }

  boolean containsKey(K key);

  V get(K key);

  V put(K key, V value);

  V remove(K key);

  Iterable<Entry<K, V>> entries();

  record Entry<K, V>(K key, V value) {}
}
