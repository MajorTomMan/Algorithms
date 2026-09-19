package com.majortom.algorithms.visualization.runtime.value;

import java.util.random.RandomGenerator;

/** Optional random-input capability; a parser does not imply a generator. */
@FunctionalInterface
public interface SampleGenerator<T> {
  T next(RandomGenerator random);
}
