package com.majortom.algorithms.core.io;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Shared classpath JSON loader; subclasses supply their module's anchor class.
 *
 * <p>Uses the framework's JsonDataCodec for typed deserialization. This class never
 * interprets business fields or creates graph topology. Resources are read-only.
 */
public abstract class AbstractJsonResourceLoader implements ResourceLoader {
  private final Class<?> resourceAnchor;

  protected AbstractJsonResourceLoader(Class<?> resourceAnchor) {
    this.resourceAnchor = Objects.requireNonNull(resourceAnchor, "resourceAnchor");
  }

  @Override
  public final <T> T load(String resourcePath, Class<T> type) {
    return read(resourcePath, new JsonDataCodec<>(Objects.requireNonNull(type, "type")));
  }

  @Override
  public final <T> T load(String resourcePath, TypeReference<T> type) {
    return read(resourcePath, new JsonDataCodec<>(Objects.requireNonNull(type, "type")));
  }

  private <T> T read(String resourcePath, DataReader<T> reader) {
    String path = normalizedPath(resourcePath);
    try (InputStream input = resourceAnchor.getResourceAsStream("/" + path)) {
      if (input == null)
        throw new IllegalArgumentException("JSON resource not found: " + path);
      T value = reader.read(input);
      if (value == null)
        throw new IllegalArgumentException("JSON resource must not be null: " + path);
      return value;
    } catch (IOException exception) {
      throw new IllegalArgumentException("Cannot read JSON resource: " + path, exception);
    }
  }

  private static String normalizedPath(String resourcePath) {
    Objects.requireNonNull(resourcePath, "resourcePath");
    String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
    if (path.isBlank() || path.indexOf('\\') >= 0) {
      throw new IllegalArgumentException("Invalid classpath resource path: " + resourcePath);
    }
    for (String part : path.split("/", -1)) {
      if (part.isEmpty() || part.equals(".") || part.equals("..")) {
        throw new IllegalArgumentException("Invalid classpath resource path: " + resourcePath);
      }
    }
    return path;
  }
}
