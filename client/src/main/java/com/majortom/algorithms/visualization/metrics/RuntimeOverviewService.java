package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.core.runtime.ExecutionSummary;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Aggregates structure facts, algorithm observations and performance into one UI-neutral model. */
public final class RuntimeOverviewService {
  private final StructureMetricsRegistry structures;
  private final AlgorithmMetricsProvider algorithms;
  private final PerformanceMetricsProvider performance;

  public RuntimeOverviewService() {
    this(StructureMetricsRegistry.defaults(), new DefaultAlgorithmMetricsProvider(),
        new PerformanceMetricsProvider());
  }

  RuntimeOverviewService(
      StructureMetricsRegistry structures,
      AlgorithmMetricsProvider algorithms,
      PerformanceMetricsProvider performance) {
    this.structures = Objects.requireNonNull(structures, "structures");
    this.algorithms = Objects.requireNonNull(algorithms, "algorithms");
    this.performance = Objects.requireNonNull(performance, "performance");
  }

  public RuntimeOverviewModel build(
      String structureId,
      Object structureState,
      List<EventEnvelope> structureEvents,
      ExecutionStatistics statistics,
      List<EventEnvelope> executionEvents,
      ExecutionSummary summary,
      Optional<Duration> playbackDuration,
      Map<String, Long> peaks) {
    List<MetricItem> algorithmMetrics = new java.util.ArrayList<>(
        algorithms.metrics(structureId, statistics, executionEvents, peaks));
    for (MetricItem metric : structures.executionMetrics(structureId, structureState, executionEvents, peaks)) {
      if (algorithmMetrics.stream().noneMatch(existing -> existing.key().equals(metric.key()))) {
        algorithmMetrics.add(metric);
      }
    }
    return new RuntimeOverviewModel(
        structures.metrics(structureId, structureState, structureEvents),
        List.copyOf(algorithmMetrics),
        performance.metrics(summary, playbackDuration));
  }

  public StructureMetricsRegistry structures() {
    return structures;
  }
}
