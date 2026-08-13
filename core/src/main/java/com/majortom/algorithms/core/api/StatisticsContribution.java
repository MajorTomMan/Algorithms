package com.majortom.algorithms.core.api;

import java.util.Map;

/**
 * Optional capability for domain events that contribute algorithm-specific counter deltas.
 * Metric names are stable, server-safe identifiers such as {@code comparisons} or
 * {@code nodes.visited}; values are non-negative increments.
 */
public interface StatisticsContribution {

    Map<String, Long> metricDeltas();
}
