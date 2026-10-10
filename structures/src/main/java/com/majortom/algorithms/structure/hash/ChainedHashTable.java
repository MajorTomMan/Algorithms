
package com.majortom.algorithms.structure.hash;

import com.majortom.algorithms.core.event.structure.HashStructureEvent;
import com.majortom.algorithms.core.snapshot.HashTableSnapshot;
import com.majortom.algorithms.core.runtime.StructureEvents;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ChainedHashTable<K, V> implements HashTableStructure<K, V>, HashTableBulkLoadSupport<K, V> {

    private static final int DEFAULT_CAPACITY = 8;
    private static final double LOAD_FACTOR = 0.75;

    private static final class Node<K, V> {
        final K key;
        V value;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private Node<K, V>[] buckets;
    private int size;

    public ChainedHashTable() {
        buckets = newBuckets(DEFAULT_CAPACITY);
    }

    public static <K, V> ChainedHashTable<K, V> fromSnapshot(HashTableSnapshot<K, V> snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        ChainedHashTable<K, V> table = new ChainedHashTable<>();
        List<HashTableStructure.Entry<K, V>> entries = new ArrayList<>();
        for (HashTableSnapshot.Entry<K, V> entry : snapshot.entries()) {
            int expectedBucket = table.indexFor(entry.key(), snapshot.capacity());
            if (entry.bucketIndex() != expectedBucket) {
                throw new IllegalArgumentException("snapshot bucket index does not match key");
            }
            entries.add(new HashTableStructure.Entry<>(entry.key(), entry.value()));
        }
        table.initialize(snapshot.capacity(), entries);
        return table;
    }

    public HashTableSnapshot<K, V> snapshot() {
        List<HashTableSnapshot.Entry<K, V>> entries = new ArrayList<>();
        for (int index = 0; index < buckets.length; index++) {
            Node<K, V> current = buckets[index];
            while (current != null) {
                entries.add(new HashTableSnapshot.Entry<>(index, current.key, current.value));
                current = current.next;
            }
        }
        return new HashTableSnapshot<>(capacity(), entries);
    }

    @SuppressWarnings("unchecked")
    private Node<K, V>[] newBuckets(int capacity) {
        return (Node<K, V>[]) new Node[capacity];
    }

    private int indexFor(K key, int capacity) {
        return Math.floorMod(key.hashCode(), capacity);
    }

    private Node<K, V> findNode(K key) {
        int index = indexFor(key, capacity());
        Node<K, V> current = buckets[index];

        while (current != null) {
            if (Objects.equals(current.key, key)) {
                return current;
            }
            current = current.next;
        }
        return null;
    }

    private void append(Node<K, V>[] target, int index, K key, V value) {
        Node<K, V> node = new Node<>(key, value);

        if (target[index] == null) {
            target[index] = node;
            return;
        }

        Node<K, V> current = target[index];
        while (current.next != null) {
            current = current.next;
        }
        current.next = node;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int capacity() {
        return buckets.length;
    }

    @Override
    public boolean containsKey(K key) {
        Objects.requireNonNull(key, "key");
        return findNode(key) != null;
    }

    @Override
    public V get(K key) {
        Objects.requireNonNull(key, "key");
        Node<K, V> node = findNode(key);

        if (node == null) {
            return null;
        }
        return node.value;
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        Node<K, V> existing = findNode(key);
        if (existing != null) {
            V previous = existing.value;
            if (!Objects.equals(previous, value)) {
                existing.value = value;
                int index = indexFor(key, capacity());
                StructureEvents.hashUpdated(index, key, previous, value);
            }
            return previous;
        }

        if (size + 1 > capacity() * LOAD_FACTOR) {
            rehash(capacity() * 2);
        }

        int index = indexFor(key, capacity());
        append(buckets, index, key, value);
        size++;

        StructureEvents.hashInserted(index, key, value);
        return null;
    }

    @Override
    public V remove(K key) {
        Objects.requireNonNull(key, "key");
        int index = indexFor(key, capacity());

        Node<K, V> previous = null;
        Node<K, V> current = buckets[index];

        while (current != null) {
            if (Objects.equals(current.key, key)) {
                if (previous == null) {
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }

                size--;
                StructureEvents.hashRemoved(index, key, current.value);
                return current.value;
            }

            previous = current;
            current = current.next;
        }
        return null;
    }

    @Override
    public Iterable<HashTableStructure.Entry<K, V>> entries() {
        List<HashTableStructure.Entry<K, V>> result = new ArrayList<>();

        for (Node<K, V> head : buckets) {
            Node<K, V> current = head;
            while (current != null) {
                result.add(new HashTableStructure.Entry<>(current.key, current.value));
                current = current.next;
            }
        }
        return List.copyOf(result);
    }

    private List<HashStructureEvent.EntryPlacement> placements() {
        List<HashStructureEvent.EntryPlacement> result = new ArrayList<>();

        for (int index = 0; index < buckets.length; index++) {
            Node<K, V> current = buckets[index];
            while (current != null) {
                result.add(new HashStructureEvent.EntryPlacement(index, current.key, current.value));
                current = current.next;
            }
        }
        return result;
    }

    private void rehash(int newCapacity) {
        int previousCapacity = capacity();
        Node<K, V>[] next = newBuckets(newCapacity);

        for (Node<K, V> head : buckets) {
            Node<K, V> current = head;
            while (current != null) {
                int index = indexFor(current.key, newCapacity);
                append(next, index, current.key, current.value);
                current = current.next;
            }
        }

        buckets = next;
        StructureEvents.hashRehashed(previousCapacity, newCapacity, placements());
    }

    @Override
    public void initialize(int capacity, Iterable<HashTableStructure.Entry<K, V>> entries) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        Objects.requireNonNull(entries, "entries");

        int previousCapacity = capacity();
        Node<K, V>[] next = newBuckets(capacity);
        int count = 0;

        for (HashTableStructure.Entry<K, V> entry : entries) {
            Objects.requireNonNull(entry, "entry");
            K key = Objects.requireNonNull(entry.key(), "key");
            V value = Objects.requireNonNull(entry.value(), "value");

            int index = indexFor(key, capacity);
            Node<K, V> current = next[index];
            while (current != null) {
                if (Objects.equals(current.key, key)) {
                    throw new IllegalArgumentException("Duplicate key: " + key);
                }
                current = current.next;
            }

            append(next, index, key, value);
            count++;
        }

        buckets = next;
        size = count;
        StructureEvents.hashRehashed(previousCapacity, capacity, placements());
    }
}
