package com.majortom.algorithms.structure.linked;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.StructureModule;

@Structure(id = StructureIds.QUEUE, name = "Queue", module = StructureModule.QUEUE, implementation = LinkedList.class)
public interface QueueStructure<T> extends LinkedStructure<T> {
  int size();

  default boolean isEmpty() {
    return size() == 0;
  }

  void enqueue(T value);

  T dequeue();

  T front();

  T rear();
}
