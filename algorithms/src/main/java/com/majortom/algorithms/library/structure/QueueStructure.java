package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.library.basic.node.ListNode;

public interface QueueStructure<T> {
    int size();
    boolean isEmpty();
    void enqueue(T value);
    T dequeue();
    T front();
    T rear();
    ListNode<T> raw();
}
