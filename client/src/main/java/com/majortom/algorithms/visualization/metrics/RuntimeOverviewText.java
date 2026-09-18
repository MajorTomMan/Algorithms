package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.visualization.international.I18N;
import java.util.ArrayList;
import java.util.List;

/** Temporary compact rendering for the legacy text overview; card UI consumes the same model later. */
public final class RuntimeOverviewText {
  private RuntimeOverviewText() {}

  public static String format(RuntimeOverviewModel model) {
    List<String> parts = new ArrayList<>();
    append(parts, model.structureMetrics());
    append(parts, model.algorithmMetrics());
    append(parts, model.performanceMetrics());
    return String.join(" | ", parts);
  }

  public static String formatStructure(RuntimeOverviewModel model) {
    List<String> parts = new ArrayList<>();
    append(parts, model.structureMetrics());
    return String.join(" | ", parts);
  }

  public static String label(MetricItem metric) {
    return I18N.text(metric.labelKey());
  }

  public static String value(MetricItem metric) {
    return metric.valueLabelKey().map(I18N::text).orElse(metric.value());
  }

  private static void append(List<String> parts, List<MetricItem> metrics) {
    for (MetricItem metric : metrics) {
      parts.add(label(metric) + ": " + value(metric));
    }
  }
}
