package com.majortom.algorithms.structure.array;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureModule;
import java.util.Collection;

@Structure(
    id = "array", name = "Array", module = StructureModule.ARRAY, implementation = Array.class)
public interface ArrayStructure<T> extends Iterable<T> {
  int size();

  default boolean isEmpty() {
    return size() == 0;
  }

  /** Replaces the complete array state through the trusted bulk-load path. */
  void initialize(Collection<? extends T> values);

  T get(int index);
  T set(int index, T value);
  void insert(int index, T value);
  T remove(int index);
  void swap(int leftIndex, int rightIndex);
}
