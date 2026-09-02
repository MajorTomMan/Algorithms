package com.majortom.algorithms.library.structure;

import java.util.Objects;

public final class HashEntry<K, V> {
    private final K key;
    private V value;
    private HashEntry<K, V> next;

    HashEntry(K key, V value) {
        this.key = Objects.requireNonNull(key, "key");
        this.value = Objects.requireNonNull(value, "value");
    }

    public K key() { return key; }
    public V value() { return value; }
    public HashEntry<K, V> next() { return next; }
    void value(V value) { this.value = Objects.requireNonNull(value, "value"); }
    void next(HashEntry<K, V> next) { this.next = next; }
}
