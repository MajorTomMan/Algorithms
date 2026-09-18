package com.majortom.algorithms.visualization.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

/** Per-execution maxima derived from reducer ViewStates, independent of JavaFX presentation. */
public final class RuntimeMetricTracker {
  private final Map<String, Long> peaks = new LinkedHashMap<>();

  public void reset() {
    peaks.clear();
  }

  public void observe(StructureMetricsRegistry registry, String structureId, Object state) {
    if (state == null) return;
    registry.samples(structureId, state).forEach((key, value) -> {
      if (value != null && value >= 0L) peaks.merge(key, value, Math::max);
    });
  }

  public Map<String, Long> peaks() {
    return Map.copyOf(peaks);
  }
}
