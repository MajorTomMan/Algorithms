package com.majortom.algorithms.algorithm.linkedlist.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.structure.linked.LinkedStructure;

@Algorithm(id = "reverse-linked-list", name = "反转链表", type = Integer.class,
    structure = LinkedStructure.class)
public class ReverseLinkedList {
  @AlgorithmEntry
  public void reverse(LinkedStructure<Integer> list) {}
}
