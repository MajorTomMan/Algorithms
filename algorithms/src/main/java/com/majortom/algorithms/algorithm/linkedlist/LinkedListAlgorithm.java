package com.majortom.algorithms.algorithm.linkedlist;

import com.majortom.algorithms.structure.linked.LinkedStructure;

public interface LinkedListAlgorithm<T> extends LinkedListFamilyAlgorithm<T> {
    void reverse(LinkedStructure<T> list);
}
