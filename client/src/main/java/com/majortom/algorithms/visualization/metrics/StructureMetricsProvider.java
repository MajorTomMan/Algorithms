package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.util.List;
import java.util.Map;

/** Converts one immutable structure ViewState into semantic overview metrics. */
public interface StructureMetricsProvider<S> {
  Class<S> stateType();

  default boolean supports(String structureId) {
    return true;
  }

  List<MetricItem> metrics(S state, StructureMetricsContext context);

  /** Structure-specific metrics derived from the current algorithm execution stream. */
  default List<MetricItem> executionMetrics(
      S state, List<EventEnvelope> events, Map<String, Long> peaks) {
    return List.of();
  }

  /** Numeric state samples whose maxima should be retained during one algorithm execution. */
  default Map<String, Long> samples(S state) {
    return Map.of();
  }
}
