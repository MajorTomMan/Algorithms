package com.majortom.algorithms.structure.hash;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.StructureModule;


@Structure(id = StructureIds.HASH, name = "Hash Table", module = StructureModule.HASH, implementation = ChainedHashTable.class)
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

  record Entry<K, V>(K key, V value) {
  }
}
