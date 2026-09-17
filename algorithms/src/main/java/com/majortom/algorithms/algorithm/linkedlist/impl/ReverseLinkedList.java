package com.majortom.algorithms.algorithm.linkedlist.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.runtime.Observations;
import com.majortom.algorithms.structure.linked.LinkedStructure;
import com.majortom.algorithms.structure.linked.ListNode;

@Algorithm(id = "reverse-linked-list", name = "反转链表", type = Integer.class, structure = LinkedStructure.class)
public class ReverseLinkedList {
  @AlgorithmEntry
  public void reverse(LinkedStructure<Integer> list) {
    ListNode<Integer> prev = null;
    ListNode<Integer> current = list.getHead();
    while (current != null) {
      ListNode<Integer> next = current.getNext();
      current.setNext(prev);
      prev = current;
      current = next;
    }
    list.setHead(prev);
  }
}
