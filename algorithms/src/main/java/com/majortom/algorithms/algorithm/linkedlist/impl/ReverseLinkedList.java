package com.majortom.algorithms.algorithm.linkedlist.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.runtime.AlgorithmEvents;
import com.majortom.algorithms.core.event.algorithm.LinkedAlgorithmEvent;
import com.majortom.algorithms.structure.linked.LinkedStructure;
import com.majortom.algorithms.structure.linked.ListNode;

@Algorithm(id = "reverse-linked-list", name = "反转链表", type = Integer.class, structure = LinkedStructure.class)
public class ReverseLinkedList {
  @AlgorithmEntry
  public void reverse(LinkedStructure<Integer> list) {
    ListNode<Integer> prev = null;
    ListNode<Integer> current = list.getHead();
    ListNode<Integer> oldHead = list.getHead();
    while (current != null) {
      AlgorithmEvents.emit(new LinkedAlgorithmEvent.CursorMoved("current", current.getId()));
      ListNode<Integer> next = current.getNext();
      current.setNext(prev);
      current.setPrevious(next);
      prev = current;
      current = next;
    }
    list.setHead(prev);
    list.setTail(oldHead);
  }
}
