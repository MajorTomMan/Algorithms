package com.majortom.algorithms.structure.linked;

public interface QueueStructure<T> extends Iterable<T> {
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    void enqueue(T value);
    T dequeue();
    T front();
    T rear();
}
