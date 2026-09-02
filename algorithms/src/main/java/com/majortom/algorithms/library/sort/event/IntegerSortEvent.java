package com.majortom.algorithms.library.sort.event;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.statistics.StatisticsContribution;

import java.util.Map;

/** Closed event set shared by every production integer sort. */
public sealed interface IntegerSortEvent extends ExecutionEvent, StatisticsContribution
        permits SortInitializedEvent, SortComparedEvent, SortRangeSelectedEvent, SortPivotSelectedEvent, SortElementSettledEvent,
        SortCompletedEvent {

    @Override
    default Map<String, Long> metricDeltas() {
        if (this instanceof SortComparedEvent) {
            return Map.of("comparisons", 1L);
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
