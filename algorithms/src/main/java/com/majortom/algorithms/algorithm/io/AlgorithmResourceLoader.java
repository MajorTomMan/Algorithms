package com.majortom.algorithms.algorithm.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.majortom.algorithms.core.io.AbstractJsonResourceLoader;
import com.majortom.algorithms.core.io.ResourceLoader;

/**
 * Static JSON resource access for algorithms.
 *
 * <p>Only deserializes classpath resources to caller-specified types.
 * Graph construction and other domain behavior belong to the algorithm.
 */
public final class AlgorithmResourceLoader {
  private static final ResourceLoader LOADER =
      new AbstractJsonResourceLoader(AlgorithmResourceLoader.class) {};

  private AlgorithmResourceLoader() {}

  public static <T> T load(String path, Class<T> type) {
    return LOADER.load(path, type);
  }

  public static <T> T load(String path, TypeReference<T> type) {
    return LOADER.load(path, type);
  }
}
