package com.majortom.algorithms.structure.linked;

public interface StackStructure<T> extends Iterable<T> {
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    void push(T value);
    T pop();
    T peek();
}
