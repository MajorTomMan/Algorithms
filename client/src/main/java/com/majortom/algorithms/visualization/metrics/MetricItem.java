package com.majortom.algorithms.visualization.metrics;

import java.util.Objects;
import java.util.Optional;

/** One presentation-neutral metric value. Labels are resolved by the UI through I18N. */
public record MetricItem(String key, String labelKey, String value, Optional<String> valueLabelKey) {
  public MetricItem {
    key = requireText(key, "key");
    labelKey = requireText(labelKey, "labelKey");
    value = Objects.requireNonNull(value, "value");
    valueLabelKey = Objects.requireNonNull(valueLabelKey, "valueLabelKey");
  }

  public MetricItem(String key, String labelKey, String value) {
    this(key, labelKey, value, Optional.empty());
  }

  public static MetricItem of(String key, String labelKey, long value) {
    return new MetricItem(key, labelKey, Long.toString(value));
  }

  public static MetricItem text(String key, String labelKey, String value) {
    return new MetricItem(key, labelKey, value);
  }

  public static MetricItem localizedValue(String key, String labelKey, String valueLabelKey) {
    return new MetricItem(key, labelKey, "", Optional.of(requireText(valueLabelKey, "valueLabelKey")));
  }

  private static String requireText(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    return value;
  }
}
