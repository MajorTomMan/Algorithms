package com.majortom.algorithms.algorithm.io;

import com.majortom.algorithms.core.io.AbstractJsonResourceLoader;

/**
 * Algorithm module resource loader.
 *
 * <p>Only deserializes JSON into the caller's chosen type; no algorithm-specific mapping.
 * Example: new AlgorithmResourceLoader().load("input.json", MyInput.class).
 */
public final class AlgorithmResourceLoader extends AbstractJsonResourceLoader {
  public AlgorithmResourceLoader() {
    super(AlgorithmResourceLoader.class);
  }
}
