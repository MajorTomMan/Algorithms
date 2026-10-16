package com.majortom.algorithms.algorithm.io;

import com.majortom.algorithms.core.io.AbstractJsonResourceLoader;

/**
 * Algorithm module resource loader.
 *
 * <p>Example: new AlgorithmResourceLoader().load(
 * "graph/regions/demo.json", MyInput.class).
 */
public final class AlgorithmResourceLoader extends AbstractJsonResourceLoader {
  public AlgorithmResourceLoader() {
    super(AlgorithmResourceLoader.class);
  }
}
