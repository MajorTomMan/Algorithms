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
    return build(structureState, new StructureMetricsContext(structureId, structureEvents),
        statistics, executionEvents, summary, playbackDuration, peaks);
  }

  /** Uses the caller's already summarized structure history instead of rescanning it. */
  public RuntimeOverviewModel build(
      Object structureState,
      StructureMetricsContext context,
      ExecutionStatistics statistics,
      List<EventEnvelope> executionEvents,
      ExecutionSummary summary,
      Optional<Duration> playbackDuration,
      Map<String, Long> peaks) {
    String structureId = context.structureId();
    List<MetricItem> algorithmMetrics = new java.util.ArrayList<>(
        algorithms.metrics(structureId, statistics, executionEvents, peaks));
    // Typed event contributions are authoritative; structure-specific counters fill gaps.
    java.util.Set<String> seen = new java.util.HashSet<>();
    for (MetricItem metric : algorithmMetrics) seen.add(metric.key());
    for (MetricItem metric : structures.executionMetrics(structureId, structureState, executionEvents, peaks)) {
      if (seen.add(metric.key())) algorithmMetrics.add(metric);
    }
    return new RuntimeOverviewModel(
        structures.metrics(structureState, context),
        List.copyOf(algorithmMetrics),
        performance.metrics(summary, playbackDuration));
  }

  public StructureMetricsRegistry structures() {
    return structures;
  }
}
