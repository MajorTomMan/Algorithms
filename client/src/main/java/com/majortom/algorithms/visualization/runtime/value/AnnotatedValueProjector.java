package com.majortom.algorithms.visualization.runtime.value;

import com.majortom.algorithms.core.metadata.ValueMetadata;
import java.util.ArrayList;
import java.util.List;

/** Read-only presentation from the same cached type descriptor used for batch graph imports. */
final class AnnotatedValueProjector {
  private AnnotatedValueProjector() {}

  static ValueProjection project(Object value) {
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
    label = label.replace('\n', ' ').replace('\r', ' ');
    if (label.length() > 72) label = label.substring(0, 71) + "…";
    return new ValueProjection(label, details, true);
  }

  private static String format(Object value) {
    if (value == null) return "null";
    if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean
        || value instanceof Character || value instanceof Enum<?>)
      return String.valueOf(value);
    // Do not automatically expand recursive user object references.
    return value.getClass().getSimpleName();
  }
}
