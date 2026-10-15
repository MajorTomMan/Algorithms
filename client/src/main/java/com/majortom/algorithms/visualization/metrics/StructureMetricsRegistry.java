package com.majortom.algorithms.visualization.metrics;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Registry of semantic metrics for every currently visualized structure family. */
public final class StructureMetricsRegistry {
  private final List<StructureMetricsProvider<?>> providers;

  public StructureMetricsRegistry(List<StructureMetricsProvider<?>> providers) {
    this.providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
  }

  public static StructureMetricsRegistry defaults() {
    return new StructureMetricsRegistry(List.of(
        new ArrayMetricsProvider(),
        new LinearMetricsProvider(),
        new LinkedMetricsProvider(),
        new TreeMetricsProvider(),
        new GraphMetricsProvider(),
        new StringMetricsProvider(),
        new MazeMetricsProvider(),
        new HashMetricsProvider()));
  }

  public List<MetricItem> metrics(String structureId, Object state, List<com.majortom.algorithms.core.runtime.EventEnvelope> events) {
    if (state == null) return List.of();
    return metrics(state, new StructureMetricsContext(structureId, events));
  }

  /** Reuse a history summary across frames while the structure events stay unchanged. */
  public List<MetricItem> metrics(Object state, StructureMetricsContext context) {
    Objects.requireNonNull(context, "context");
    if (state == null) return List.of();
    StructureMetricsProvider<Object> provider = provider(context.structureId(), state);
    return provider == null ? List.of() : provider.metrics(state, context);
  }

  public Map<String, Long> samples(String structureId, Object state) {
    if (state == null) return Map.of();
    StructureMetricsProvider<Object> provider = provider(structureId, state);
    return provider == null ? Map.of() : provider.samples(state);
  }

  public List<MetricItem> executionMetrics(
      String structureId, Object state, List<com.majortom.algorithms.core.runtime.EventEnvelope> events,
      Map<String, Long> peaks) {
    if (state == null) return List.of();
    StructureMetricsProvider<Object> provider = provider(structureId, state);
    return provider == null ? List.of() : provider.executionMetrics(state, events, peaks);
  }

  @SuppressWarnings("unchecked")
  private StructureMetricsProvider<Object> provider(String structureId, Object state) {
    for (StructureMetricsProvider<?> candidate : providers) {
      if (candidate.stateType().isInstance(state) && candidate.supports(structureId)) {
        return (StructureMetricsProvider<Object>) candidate;
      }
    }
    return null;
  }


}
