package com.majortom.algorithms.visualization.runtime.value;

/** Supplies distinct values for identity-sensitive structures, without collision retries. */
public interface DistinctSampleProvider<T> {
  int capacity();
  T valueAt(int index);
}
