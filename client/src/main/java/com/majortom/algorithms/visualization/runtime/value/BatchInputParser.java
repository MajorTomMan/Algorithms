package com.majortom.algorithms.visualization.runtime.value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Parse every element before a caller attempts to mutate its structure. */
public final class BatchInputParser {
  private BatchInputParser() {}

  public static <T> List<T> parse(String text, ValueAdapter<T> adapter) {
    Objects.requireNonNull(adapter, "adapter");
    if (text == null || text.isBlank()) throw new IllegalArgumentException("Batch input is empty");
    // Preserve the original compact input for built-in scalars. Custom codecs own commas/spaces;
    // each custom item occupies a line or is separated by a semicolon.
    String[] tokens = isScalar(adapter.type())
        ? text.trim().split("[,;\\s]+") : text.split("[;\\r\\n]+");
    List<T> values = new ArrayList<>(tokens.length);
    for (int index = 0; index < tokens.length; index++) {
      String token = tokens[index].trim();
      if (token.isEmpty()) continue;
      try {
        T value = Objects.requireNonNull(adapter.parse(token), "parsed element");
        if (!adapter.type().isInstance(value))
          throw new IllegalArgumentException("Wrong parsed element type: " + value.getClass().getName());
        values.add(value);
      } catch (RuntimeException failure) {
        throw new IllegalArgumentException("Invalid batch element " + (index + 1) + ": " + token, failure);
      }
    }
    if (values.isEmpty()) throw new IllegalArgumentException("Batch input is empty");
    return List.copyOf(values);
  }

  private static boolean isScalar(Class<?> type) {
    return type == Byte.class || type == Short.class || type == Integer.class
        || type == Long.class || type == Float.class || type == Double.class
        || type == Boolean.class || type == Character.class || type == String.class;
  }
}
