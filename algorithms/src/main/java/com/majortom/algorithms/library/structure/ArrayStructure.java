package com.majortom.algorithms.library.structure;

public interface ArrayStructure<T> extends Iterable<T> {
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    T get(int index);
    T set(int index, T value);
    void insert(int index, T value);
    T remove(int index);
    void swap(int leftIndex, int rightIndex);
}
