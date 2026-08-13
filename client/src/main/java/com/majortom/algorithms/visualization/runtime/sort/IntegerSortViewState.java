package com.majortom.algorithms.visualization.runtime.sort;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable JavaFX-neutral state for every visible sort phase. */
public record IntegerSortViewState(
        List<Integer> values,
        int comparedIndex,
        int insertionIndex,
        int swapLeftIndex,
        int swapRightIndex,
        int pivotIndex,
        int rangeStart,
        int rangeEnd,
        Set<Integer> settledIndices,
        Phase phase,
        boolean completed) {

    public IntegerSortViewState {
        values = List.copyOf(Objects.requireNonNull(values, "values"));
        settledIndices = Set.copyOf(Objects.requireNonNull(settledIndices, "settledIndices"));
        Objects.requireNonNull(phase, "phase");
    }

    public static IntegerSortViewState empty() {
        return new IntegerSortViewState(List.of(), -1, -1, -1, -1, -1, -1, -1,
                Set.of(), Phase.IDLE, false);
    }

    public static IntegerSortViewState source(List<Integer> values) {
        return new IntegerSortViewState(values, -1, -1, -1, -1, -1, -1, -1,
                Set.of(), Phase.IDLE, false);
    }

    public enum Phase {
        IDLE,
        INITIALIZED,
        RANGE_SELECTED,
        PIVOT_SELECTED,
        COMPARING,
        WRITING,
        SWAPPING,
        SETTLED,
        COMPLETED
    }
}
