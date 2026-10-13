package com.majortom.algorithms.visualization.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.majortom.algorithms.core.annotation.GraphNodes;
import com.majortom.algorithms.core.metadata.AnnotatedMembers;
import java.io.IOException;
import java.util.Objects;

/**
 * Converts user-supplied JSON to an explicitly annotated Java batch object.
 * Relationship discovery and graph construction remain owned by MultiGraph.from().
 * No application-specific names or dynamic Java compilation are involved.
 */
public final class MultiGraphJsonLoader {
  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
  private MultiGraphJsonLoader() {}

  public static Object read(String className, String json) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) loader = MultiGraphJsonLoader.class.getClassLoader();
    return read(className, json, loader);
  }

  public static Object read(String className, String json, ClassLoader loader) {
    Objects.requireNonNull(loader, "loader");
    if (className == null || className.isBlank())
      throw new IllegalArgumentException("Enter the batch model's fully qualified Java class name");
    if (json == null || json.isBlank())
      throw new IllegalArgumentException("The batch JSON must not be empty");
    Class<?> type;
    try {
      type = Class.forName(className.trim(), false, loader);
    } catch (ClassNotFoundException exception) {
      throw new IllegalArgumentException("Batch type not on the application classpath: " + className,
          exception);
    }
    if (type.isInterface() || type.isEnum() || type.isAnnotation()
        || AnnotatedMembers.marked(type, GraphNodes.class).isEmpty())
      throw new IllegalArgumentException("Batch type must declare at least one @GraphNodes member: "
          + type.getName());
    try {
      return MAPPER.readValue(json, type);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Cannot read batch JSON as " + type.getName()
          + ": " + exception.getMessage(), exception);
    }
  }
}
