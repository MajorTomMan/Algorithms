package com.majortom.algorithms.library.structure;

import java.util.List;

public interface HashTableStructure<K, V> {
    int size();
    int capacity();
    boolean containsKey(K key);
    V get(K key);
    V put(K key, V value);
    V remove(K key);
    List<HashEntry<K, V>> raw();
}
