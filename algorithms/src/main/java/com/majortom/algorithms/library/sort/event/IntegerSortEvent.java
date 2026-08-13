package com.majortom.algorithms.library.sort.event;

import com.majortom.algorithms.core.api.AlgorithmEvent;
import com.majortom.algorithms.core.api.StatisticsContribution;

import java.util.Map;

/** Closed event set shared by every production integer sort. */
public sealed interface IntegerSortEvent extends AlgorithmEvent, StatisticsContribution
        permits SortInitializedEvent, SortComparedEvent, SortWrittenEvent, SortSwappedEvent,
        SortRangeSelectedEvent, SortPivotSelectedEvent, SortElementSettledEvent,
        SortCompletedEvent {

    @Override
    default Map<String, Long> metricDeltas() {
        if (this instanceof SortComparedEvent) {
            return Map.of("comparisons", 1L);
        }
        if (this instanceof SortWrittenEvent) {
            return Map.of("writes", 1L);
        }
        if (this instanceof SortSwappedEvent) {
            return Map.of("swaps", 1L, "writes", 2L);
        }
        if (this instanceof SortRangeSelectedEvent) {
            return Map.of("ranges.selected", 1L);
        }
        if (this instanceof SortPivotSelectedEvent) {
            return Map.of("pivots.selected", 1L);
        }
        if (this instanceof SortElementSettledEvent) {
            return Map.of("elements.settled", 1L);
        }
        return Map.of();
    }
}
