package com.majortom.algorithms.library.structure;

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
