package com.majortom.algorithms.practice.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.majortom.algorithms.core.io.AbstractJsonResourceLoader;
import com.majortom.algorithms.core.io.ResourceLoader;

/**
 * Static JSON resource access for the program/practice module.
 *
 * <p>Only deserializes classpath resources to caller-specified types.
 */
public final class ProgramResourceLoader {
  private static final ResourceLoader LOADER =
      new AbstractJsonResourceLoader(ProgramResourceLoader.class) {};

  private ProgramResourceLoader() {}

  public static <T> T load(String path, Class<T> type) {
    return LOADER.load(path, type);
  }

  public static <T> T load(String path, TypeReference<T> type) {
    return LOADER.load(path, type);
  }
}
