package com.majortom.algorithms.core.io;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * Shared classpath resource loader for JSON and header-based CSV files.
 *
 * <p>Module-specific static facades provide the classpath anchor. Deserialization only:
 * business-specific mapping and graph construction belong to the caller.
 */
public abstract class AbstractResourceLoader implements ResourceLoader {
  private final Class<?> resourceAnchor;

  protected AbstractResourceLoader(Class<?> resourceAnchor) {
    this.resourceAnchor = Objects.requireNonNull(resourceAnchor, "resourceAnchor");
  }

  @Override
  public final <T> T load(String resourcePath, Class<T> type) {
    return read(resourcePath, "JSON",
        new JsonDataCodec<>(Objects.requireNonNull(type, "type")));
  }

  @Override
  public final <T> T load(String resourcePath, TypeReference<T> type) {
    return read(resourcePath, "JSON",
        new JsonDataCodec<>(Objects.requireNonNull(type, "type")));
  }

  @Override
  public final <T> List<T> loadCsv(String resourcePath, Class<T> rowType) {
    return read(resourcePath, "CSV",
        new CsvDataReader<>(Objects.requireNonNull(rowType, "rowType")));
  }

  @Override
  public final <T> List<T> loadCsv(String resourcePath, TypeReference<T> rowType) {
    return read(resourcePath, "CSV",
        new CsvDataReader<>(Objects.requireNonNull(rowType, "rowType")));
  }

  private <T> T read(String resourcePath, String format, DataReader<T> reader) {
    String path = normalizedPath(resourcePath);
    try (InputStream input = resourceAnchor.getResourceAsStream("/" + path)) {
      if (input == null)
        throw new IllegalArgumentException(format + " resource not found: " + path);
      T value = reader.read(input);
      if (value == null)
        throw new IllegalArgumentException(format + " resource must not be null: " + path);
      return value;
    } catch (IOException exception) {
      throw new IllegalArgumentException("Cannot read " + format + " resource: " + path, exception);
    }
  }

  private static String normalizedPath(String resourcePath) {
    Objects.requireNonNull(resourcePath, "resourcePath");
    String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
    if (path.isBlank() || path.indexOf('\\') >= 0)
      throw new IllegalArgumentException("Invalid classpath resource path: " + resourcePath);
    for (String part : path.split("/", -1)) {
      if (part.isEmpty() || part.equals(".") || part.equals(".."))
        throw new IllegalArgumentException("Invalid classpath resource path: " + resourcePath);
    }
    return path;
  }
}
