package com.majortom.algorithms.visualization.metrics;

import java.util.List;
import java.util.Objects;

/** Complete statistics model for the current structure and algorithm execution. */
public record RuntimeOverviewModel(
    List<MetricItem> structureMetrics,
    List<MetricItem> algorithmMetrics,
    List<MetricItem> performanceMetrics) {
  public RuntimeOverviewModel {
    structureMetrics = List.copyOf(Objects.requireNonNull(structureMetrics, "structureMetrics"));
    algorithmMetrics = List.copyOf(Objects.requireNonNull(algorithmMetrics, "algorithmMetrics"));
    performanceMetrics = List.copyOf(Objects.requireNonNull(performanceMetrics, "performanceMetrics"));
  }

  public static RuntimeOverviewModel empty() {
    return new RuntimeOverviewModel(List.of(), List.of(), List.of());
  }
}
