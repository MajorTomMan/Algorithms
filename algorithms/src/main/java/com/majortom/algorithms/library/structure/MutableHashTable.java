package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.structure.event.HashTableStructureEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MutableHashTable<K, V> implements HashTableStructure<K, V> {
    private static final int DEFAULT_CAPACITY = 8;
    private static final double LOAD_FACTOR = 0.75d;

    private List<HashEntry<K, V>> buckets;
    private int size;

    public MutableHashTable() { this(DEFAULT_CAPACITY); }

    public MutableHashTable(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        buckets = emptyBuckets(normalizeCapacity(capacity));
    }

    @Override public int size() { return size; }
    @Override public int capacity() { return buckets.size(); }

    @Override
    public boolean containsKey(K key) {
        return findEntry(Objects.requireNonNull(key, "key")) != null;
    }

    @Override
    public V get(K key) {
        HashEntry<K, V> entry = findEntry(Objects.requireNonNull(key, "key"));
        return entry == null ? null : entry.value();
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        int bucketIndex = bucketIndex(key, capacity());
        HashEntry<K, V> current = buckets.get(bucketIndex);
        while (current != null) {
            if (current.key().equals(key)) {
                V previous = current.value();
                current.value(value);
                ExecutionEvents.emit(new HashTableStructureEvent.Put(key, value, previous, bucketIndex, true));
                return previous;
            }
            current = current.next();
        }
        if (size + 1 > capacity() * LOAD_FACTOR) {
            resize(capacity() * 2);
            bucketIndex = bucketIndex(key, capacity());
        }
        append(bucketIndex, new HashEntry<>(key, value));
        size++;
        ExecutionEvents.emit(new HashTableStructureEvent.Put(key, value, null, bucketIndex, false));
        return null;
    }

    @Override
    public V remove(K key) {
        Objects.requireNonNull(key, "key");
        int bucketIndex = bucketIndex(key, capacity());
        HashEntry<K, V> previous = null;
        HashEntry<K, V> current = buckets.get(bucketIndex);
        while (current != null) {
            if (current.key().equals(key)) {
                if (previous == null) buckets.set(bucketIndex, current.next());
                else previous.next(current.next());
                size--;
                ExecutionEvents.emit(new HashTableStructureEvent.Removed(key, current.value(), bucketIndex));
                return current.value();
            }
            previous = current;
            current = current.next();
        }
        return null;
    }

    @Override
    public List<HashEntry<K, V>> raw() {
        return Collections.unmodifiableList(buckets);
    }

    private HashEntry<K, V> findEntry(K key) {
        HashEntry<K, V> current = buckets.get(bucketIndex(key, capacity()));
        while (current != null) {
            if (current.key().equals(key)) return current;
            current = current.next();
        }
        return null;
    }

    private void resize(int requestedCapacity) {
        int previousCapacity = capacity();
        List<HashEntry<K, V>> previousBuckets = buckets;
        buckets = emptyBuckets(normalizeCapacity(requestedCapacity));
        for (HashEntry<K, V> head : previousBuckets) {
            HashEntry<K, V> current = head;
            while (current != null) {
                append(bucketIndex(current.key(), capacity()), new HashEntry<>(current.key(), current.value()));
                current = current.next();
            }
        }
        ExecutionEvents.emit(new HashTableStructureEvent.Resized(previousCapacity, capacity()));
    }

    private void append(int bucketIndex, HashEntry<K, V> entry) {
        HashEntry<K, V> head = buckets.get(bucketIndex);
        if (head == null) {
            buckets.set(bucketIndex, entry);
            return;
        }
        HashEntry<K, V> current = head;
        while (current.next() != null) current = current.next();
        current.next(entry);
    }

    private static int bucketIndex(Object key, int capacity) {
        return Math.floorMod(key.hashCode(), capacity);
    }

    private static int normalizeCapacity(int capacity) {
        int normalized = 1;
        while (normalized < capacity && normalized < (1 << 30)) normalized <<= 1;
        return normalized;
    }

    private static <K, V> List<HashEntry<K, V>> emptyBuckets(int capacity) {
        return new ArrayList<>(Collections.nCopies(capacity, null));
    }
}
