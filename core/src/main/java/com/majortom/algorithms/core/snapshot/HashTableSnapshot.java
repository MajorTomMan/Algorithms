package com.majortom.algorithms.core.snapshot;

import java.util.List;
import java.util.Objects;

public record HashTableSnapshot<K, V>(int capacity, List<Entry<K, V>> entries) {
    public HashTableSnapshot {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        entries = List.copyOf(entries);
    }

    public record Entry<K, V>(int bucketIndex, K key, V value) {
        public Entry {
            if (bucketIndex < 0) throw new IllegalArgumentException("bucketIndex must not be negative");
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
        }
    }
}
