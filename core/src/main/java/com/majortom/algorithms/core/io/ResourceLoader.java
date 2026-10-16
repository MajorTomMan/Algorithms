package com.majortom.algorithms.core.io;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;

/**
 * Reads classpath resources into caller-selected Java types.
 *
 * <p>Paths are relative to src/main/resources (or test resources) on the classpath.
 * The loader only deserializes; domain-specific interpretation remains with the caller.
 */
public interface ResourceLoader {

  /** Read JSON into a class or record. */
  <T> T load(String resourcePath, Class<T> type);

  /** Read JSON into a parameterized type such as List<MyRecord>. */
  <T> T load(String resourcePath, TypeReference<T> type);

  /**
   * Read a header-based CSV file into row objects.
   * Each header must match a readable property of the row type.
   */
  <T> List<T> loadCsv(String resourcePath, Class<T> rowType);

  /**
   * Read CSV rows into a generic row type such as Map<String, String>.
   * The TypeReference describes ONE ROW, not List<Row>.
   */
  <T> List<T> loadCsv(String resourcePath, TypeReference<T> rowType);
}
