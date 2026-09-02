package com.majortom.algorithms.library.string;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.statistics.StatisticsContribution;

import java.util.List;
import java.util.Map;

public sealed interface KmpEvent extends ExecutionEvent
        permits KmpEvent.Initialized, KmpEvent.Compared, KmpEvent.Fallback,
        KmpEvent.Matched, KmpEvent.Completed {
    record Initialized(String target, String pattern) implements KmpEvent {}
    record Compared(int targetIndex, int patternIndex, char targetChar, char patternChar, boolean matched)
            implements KmpEvent, StatisticsContribution {
        @Override public Map<String, Long> metricDeltas() { return Map.of("comparisons", 1L); }
    }
    record Fallback(int targetIndex, int fromPatternIndex, int toPatternIndex) implements KmpEvent {}
    record Matched(int startIndex, int length) implements KmpEvent {}
    record Completed(List<Integer> matchPositions) implements KmpEvent {
        public Completed { matchPositions = List.copyOf(matchPositions); }
    }
}
