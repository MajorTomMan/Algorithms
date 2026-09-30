package com.majortom.algorithms.structure.hash;

/**
 * Optional workbench capability for exact snapshot restore, including capacity.
 *
 * <p>This does not prescribe collision handling. Implementations may rebuild themselves using any
 * internal strategy, but should emit one factual rehash/rebuild event after the state is installed.</p>
 */
public interface HashTableBulkLoadSupport<K, V> {
  void initialize(int capacity, Iterable<HashTableStructure.Entry<K, V>> entries);
}
