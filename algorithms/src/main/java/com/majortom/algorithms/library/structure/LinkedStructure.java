package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.library.basic.node.ListNode;

public interface LinkedStructure<T> extends Iterable<T> {
    int size();
    boolean isEmpty();
    T get(int index);
    void insert(int index, T value);
    T remove(int index);
    T update(int index, T value);
    ListNode<T> raw();
}
