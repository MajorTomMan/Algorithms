package com.majortom.algorithms.library.structure;

public interface StackStructure<T> extends Iterable<T> {
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    void push(T value);
    T pop();
    T peek();
}
