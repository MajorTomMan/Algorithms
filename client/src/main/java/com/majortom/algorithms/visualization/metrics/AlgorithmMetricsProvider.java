package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import java.util.List;
import java.util.Map;

/** Derives algorithm-facing metrics from authoritative events plus reducer-state peaks. */
public interface AlgorithmMetricsProvider {
  List<MetricItem> metrics(
      String structureId,
      ExecutionStatistics statistics,
      List<EventEnvelope> events,
      Map<String, Long> peaks);
}
