package com.majortom.algorithms.structure.linked;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.structure.linked.ListNode;
import java.util.Collection;

@Structure(id = StructureIds.LINKED_LIST, name = "Linked List", module = StructureModule.LINKED_LIST,
    implementation = LinkedList.class)
public interface LinkedStructure<T> extends Iterable<T> {
  int size();

  default boolean isEmpty() {
    return size() == 0;
  }

  /** Replaces the complete linked topology through the trusted bulk-load path. */
  void initialize(Collection<? extends T> values);

  T get(int index);
  void insert(int index, T value);
  T remove(int index);
  T set(int index, T value);
  ListNode<T> getHead();
  ListNode<T> getTail();
  void setHead(ListNode<T> node);
  void setTail(ListNode<T> node);
}
