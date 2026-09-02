package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.library.basic.node.ListNode;

public interface StackStructure<T> {
    int size();
    boolean isEmpty();
    void push(T value);
    T pop();
    T peek();
    ListNode<T> raw();
}
