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

  /**
   * Immutable values can defer DETAIL getters until selection. Mutable values must
   * capture their display fields when a playback frame is created.
   */
  public static VisualValue compact(Object value) {
    if (!canDeferDetails(value)) return of(value);
    String summary = com.majortom.algorithms.visualization.runtime.value.ValueAdapters.summary(value);
    return new VisualValue(value, summary,
        new com.majortom.algorithms.visualization.runtime.value.ValueProjection(
            summary, java.util.List.of(), false));
  }

  public com.majortom.algorithms.visualization.runtime.value.ValueProjection detailProjection() {
    if (projection.fieldsDeclared() || !canDeferDetails(value)) return projection;
    return com.majortom.algorithms.visualization.runtime.value.ValueAdapters.project(value);
  }

  /** Extra annotated fields for inspectors; do not echo a scalar already in the Value row. */
  public String additionalDetails() {
    var details = detailProjection();
    if (!details.fieldsDeclared()) return "";
    if (details.fields().size() == 1 && "value".equals(details.fields().getFirst().key())
        && isScalar(value)) return "";
    return details.details();
  }

  private static boolean canDeferDetails(Object value) {
    return isScalar(value) || com.majortom.algorithms.core.metadata.ValueMetadata
        .describe(value.getClass()).immutable()
        || com.majortom.algorithms.visualization.runtime.value.ValueAdapters.canReplay(value.getClass());
  }

  private static boolean isScalar(Object value) {
    return value == null || value instanceof CharSequence || value instanceof Number
        || value instanceof Boolean || value instanceof Character || value instanceof Enum<?>;
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
