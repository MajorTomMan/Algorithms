package com.majortom.algorithms.algorithm.linkedlist.impl;

import com.majortom.algorithms.algorithm.linkedlist.LinkedListAlgorithm;
import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.structure.linked.LinkedStructure;

@Algorithm(id = "reverse-linked-list", module = "linked-list", type = Integer.class, structure = LinkedStructure.class)
public class ReverseLinkedList implements LinkedListAlgorithm<Integer> {

    @Override
    public void reverse(LinkedStructure<Integer> list) {
        System.out.println("test");
    }
    
}
