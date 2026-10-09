package com.majortom.algorithms.algorithm.array.sort;

import com.majortom.algorithms.core.domain.observation.ArrayObservationDomains;
import com.majortom.algorithms.core.runtime.AlgorithmEvents;
import com.majortom.algorithms.structure.array.ArrayStructure;

/** Shared direct helpers for concrete integer sorting algorithms. */
public abstract class BaseSort<T> {
  protected abstract int compare(T left, T right);

  protected final int compareAt(ArrayStructure<T> array, int leftIndex, int rightIndex) {
    AlgorithmEvents.compared(ArrayObservationDomains.INDEX_SOURCE, leftIndex,
        ArrayObservationDomains.INDEX_SOURCE, rightIndex);
    return compare(array.get(leftIndex), array.get(rightIndex));
  }

  protected final int compareValue(ArrayStructure<T> array, int index, T value) {
    AlgorithmEvents.compared(ArrayObservationDomains.INDEX_SOURCE, index, value);
    return compare(array.get(index), value);
  }

  protected final void write(ArrayStructure<T> array, int index, T value) {
    array.set(index, value);
  }

  protected final void swap(ArrayStructure<T> array, int leftIndex, int rightIndex) {
    if (leftIndex != rightIndex) {
      array.swap(leftIndex, rightIndex);
    }
  }
}
