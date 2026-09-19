package com.majortom.algorithms.visualization.runtime.value;

import java.util.Objects;
import java.util.function.Function;

/** Developer-declared field. A field is shown in details; summary fields also appear on nodes. */
public record DisplayField<T>(String key, String label, Function<T, ?> getter, boolean summary) {
  public DisplayField {
    if (key == null || key.isBlank()) throw new IllegalArgumentException("field key must not be blank");
    if (label == null || label.isBlank()) throw new IllegalArgumentException("field label must not be blank");
    Objects.requireNonNull(getter, "getter");
  }
}
