package com.majortom.algorithms.core.io;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Reads classpath resources into caller-selected Java types.
 *
 * <p>Paths are relative to a module's src/main/resources (or its test resources).
 * Implementations are responsible for the resource format and location.
 */
public interface ResourceLoader {

  /** Load a JSON resource into a concrete class or record. */
  <T> T load(String resourcePath, Class<T> type);

  /** Load a JSON resource into a parameterized type such as List<MyRecord>. */
  <T> T load(String resourcePath, TypeReference<T> type);
}
