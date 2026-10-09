package com.majortom.algorithms.algorithm.hash.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.runtime.AlgorithmEvents;
import com.majortom.algorithms.structure.hash.HashTableStructure;
import java.util.Iterator;
import java.util.Objects;

/**
 * Demonstrates one logical key lookup using only strategy-neutral HashTable operations.
 * The algorithm owns the sample target selection; the framework receives only generic facts.
 * Internal collision-chain probes cannot be observed through the current HashTable contract.
 */
@Algorithm(id = "hash-search", name = "哈希查找",
    types = {Integer.class, Integer.class}, structure = HashTableStructure.class)
public final class HashSearch {
  private static final String SEARCH_ID = "key-lookup";
  private static final Integer EMPTY_TABLE_TARGET = 0;

  @AlgorithmEntry
  public Integer search(HashTableStructure<Integer, Integer> table) {
    Objects.requireNonNull(table, "table");
    Iterator<HashTableStructure.Entry<Integer, Integer>> entries = table.entries().iterator();
    Integer target = entries.hasNext() ? entries.next().key() : EMPTY_TABLE_TARGET;

    AlgorithmEvent.ValueRef targetRef = new AlgorithmEvent.ValueRef(target);
    AlgorithmEvents.searchStarted(SEARCH_ID, targetRef);
    AlgorithmEvents.computed("hashCode", targetRef, new AlgorithmEvent.ValueRef(target.hashCode()));
    // One logical HashTable lookup; never invent internal bucket or collision-chain steps.
    AlgorithmEvents.searchProbed(SEARCH_ID, targetRef);
    if (table.containsKey(target)) {
      Integer result = table.get(target);
      AlgorithmEvents.searchFound(SEARCH_ID, targetRef);
      AlgorithmEvents.searchCompleted(SEARCH_ID, 1);
      return result;
    }
    AlgorithmEvents.searchCompleted(SEARCH_ID, 0);
    return null;
  }
}
