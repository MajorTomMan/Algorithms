package com.majortom.algorithms.visualization.runtime.value;

import com.majortom.algorithms.core.metadata.ValueMetadata;
import java.util.ArrayList;
import java.util.List;

/** Read-only presentation from the same cached type descriptor used for batch graph imports. */
final class AnnotatedValueProjector {
  private AnnotatedValueProjector() {}

  /** Projection for lists/canvas only: never invoke DETAIL-only getters. */
  static String summary(Object value) {
    if (value == null) return "null";
    if (isScalar(value)) return trim(format(value));
    var descriptor = ValueMetadata.describe(value.getClass());
    List<String> parts = new ArrayList<>();
    for (var field : descriptor.fields()) {
      if (!field.summary()) continue;
      try { parts.add(format(field.read(value))); }
      catch (RuntimeException failure) { parts.add("<error>"); }
    }
    return trim(parts.isEmpty() ? descriptor.name() : String.join(" · ", parts));
  }

  private static String trim(String label) {
    String compact = label.replace('\n', ' ').replace('\r', ' ');
    return compact.length() > 72 ? compact.substring(0, 71) + "…" : compact;
  }

  static ValueProjection project(Object value) {
    if (value == null || isScalar(value)) {
      String text = format(value);
      return new ValueProjection(trim(text),
          List.of(new FieldView("value", "值", text, true, false)), true);
    }
    var descriptor = ValueMetadata.describe(value.getClass());
    if (descriptor.fields().isEmpty())
      return new ValueProjection(descriptor.name(), List.of(), false);

    List<FieldView> details = new ArrayList<>();
    List<String> summary = new ArrayList<>();
    for (var field : descriptor.fields()) {
      String formatted;
      boolean error = false;
      try { formatted = format(field.read(value)); }
      catch (RuntimeException exception) { formatted = "<error>"; error = true; }
      if (field.summary()) summary.add(formatted);
      if (field.detail()) {
        details.add(new FieldView(field.key(), field.label(), formatted, field.summary(), error));
      }
    }
    String label = summary.isEmpty() ? descriptor.name() : String.join(" · ", summary);
    return new ValueProjection(trim(label), details, true);
  }

  private static boolean isScalar(Object value) {
    return value instanceof String || value instanceof Boolean
        || value instanceof Character || value instanceof Enum<?>
        || (value instanceof Number && value.getClass().getName().startsWith("java."));
  }

  private static String format(Object value) {
    if (value == null) return "null";
    if (value instanceof Enum<?> constant) return constant.name();
    if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean
        || value instanceof Character || value instanceof Enum<?>)
      return String.valueOf(value);
    // Do not automatically expand recursive user object references.
    return value.getClass().getSimpleName();
  }
}
