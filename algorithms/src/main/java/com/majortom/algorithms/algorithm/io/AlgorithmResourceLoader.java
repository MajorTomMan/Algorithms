package com.majortom.algorithms.algorithm.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.majortom.algorithms.core.io.AbstractResourceLoader;
import com.majortom.algorithms.core.io.ResourceLoader;
import java.util.List;

/**
 * Static JSON and CSV resource access for algorithms.
 * Only deserializes classpath resources; algorithms construct their own
 * structures.
 */
public final class AlgorithmResourceLoader {
  private static final ResourceLoader LOADER = new AbstractResourceLoader(AlgorithmResourceLoader.class) {
  };

  private AlgorithmResourceLoader() {
  }

  /** Read JSON into the requested Java type. */
  public static <T> T load(String path, Class<T> type) {
    return LOADER.load(path, type);
  }

  /** Read JSON into a parameterized Java type. */
  public static <T> T load(String path, TypeReference<T> type) {
    return LOADER.load(path, type);
  }

  /** Read CSV with a header row into List<T>. */
  public static <T> List<T> loadCsv(String path, Class<T> rowType) {
    return LOADER.loadCsv(path, rowType);
  }

  /** For generic CSV row types, e.g. Map<String, String>. */
  public static <T> List<T> loadCsv(String path, TypeReference<T> rowType) {
    return LOADER.loadCsv(path, rowType);
  }
}
