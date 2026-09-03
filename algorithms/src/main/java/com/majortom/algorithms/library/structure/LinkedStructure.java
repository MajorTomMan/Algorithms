package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.library.basic.node.ListNode;

public interface LinkedStructure<T> extends Iterable<T> {
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    T get(int index);
    void insert(int index, T value);
    T remove(int index);
    T set(int index, T value);
    ListNode<T> head();
    ListNode<T> tail();
}
