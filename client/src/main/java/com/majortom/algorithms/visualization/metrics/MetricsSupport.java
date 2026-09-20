package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.util.List;
import java.util.Map;

/** Shared metric aggregation and formatting; providers retain their own domain calculations. */
final class MetricsSupport {
  private MetricsSupport() {}

  static void addEventMetric(
      List<MetricItem> result, List<com.majortom.algorithms.core.runtime.EventEnvelope> events,
      Class<?> eventType, String key, String labelKey) {
    long value = countEvents(events, eventType);
    if (value > 0L) result.add(MetricItem.of(key, labelKey, value));
  }

  static long countEvents(
      List<com.majortom.algorithms.core.runtime.EventEnvelope> events, Class<?> eventType) {
    return events.stream().filter(event -> eventType.isInstance(event.event())).count();
  }

  static void addPeakMetric(
      List<MetricItem> result, Map<String, Long> peaks, String sampleKey, String key, String labelKey) {
    long value = peaks.getOrDefault(sampleKey, 0L);
    if (value > 0L) result.add(MetricItem.of(key, labelKey, value));
  }

  static String formatDecimal(double value) {
    if (Math.rint(value) == value) return Long.toString((long) value);
    return String.format(java.util.Locale.ROOT, "%.2f", value);
  }
}
