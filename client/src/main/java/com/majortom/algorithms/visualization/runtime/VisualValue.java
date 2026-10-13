package com.majortom.algorithms.visualization.runtime;

import java.util.Objects;

/** JavaFX-neutral display projection for an arbitrary runtime value. */
public record VisualValue(Object value, String text,
    com.majortom.algorithms.visualization.runtime.value.ValueProjection projection) {
  /** Backward compatible constructor for callers that explicitly choose a display label. */
  public VisualValue(Object value, String text) {
    this(value, text,
        com.majortom.algorithms.visualization.runtime.value.ValueAdapters.project(value));
  }
  public VisualValue {
    text = Objects.requireNonNull(text, "text");
    projection = Objects.requireNonNull(projection, "projection");
  }

  /** Compact value for large graphs: full annotated detail is projected on selection. */
  public static VisualValue compact(Object value) {
    String summary = com.majortom.algorithms.visualization.runtime.value.ValueAdapters.summary(value);
    return new VisualValue(value, summary,
        new com.majortom.algorithms.visualization.runtime.value.ValueProjection(
            summary, java.util.List.of(), false));
  }

  public static VisualValue of(Object value) {
    var projection = com.majortom.algorithms.visualization.runtime.value.ValueAdapters.project(value);
    return new VisualValue(value, projection.summary(), projection);
  }

  public <T> T as(Class<T> type) {
    Objects.requireNonNull(type, "type");
    return type.cast(value);
  }

  @Override
  public String toString() {
    return text;
  }
}
