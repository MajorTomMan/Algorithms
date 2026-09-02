package com.majortom.algorithms.library.structure.event;

import com.majortom.algorithms.core.event.ExecutionEvent;

public sealed interface HashTableStructureEvent extends ExecutionEvent
        permits HashTableStructureEvent.Put, HashTableStructureEvent.Removed, HashTableStructureEvent.Resized {

    record Put(Object key, Object value, Object previousValue, int bucketIndex, boolean replaced)
            implements HashTableStructureEvent {}

    record Removed(Object key, Object value, int bucketIndex) implements HashTableStructureEvent {}

    record Resized(int previousCapacity, int capacity) implements HashTableStructureEvent {}
}
