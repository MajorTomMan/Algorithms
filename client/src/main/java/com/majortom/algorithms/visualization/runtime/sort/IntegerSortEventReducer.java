package com.majortom.algorithms.visualization.runtime.sort;

import com.majortom.algorithms.core.runtime.EventImportance;
import com.majortom.algorithms.core.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.ExecutionEvent;
import com.majortom.algorithms.core.runtime.Reduction;
import com.majortom.algorithms.library.sort.event.SortComparedEvent;
import com.majortom.algorithms.library.sort.event.SortCompletedEvent;
import com.majortom.algorithms.library.sort.event.SortElementSettledEvent;
import com.majortom.algorithms.library.sort.event.SortInitializedEvent;
import com.majortom.algorithms.library.sort.event.SortPivotSelectedEvent;
import com.majortom.algorithms.library.sort.event.SortRangeSelectedEvent;
import com.majortom.algorithms.library.sort.event.SortSwappedEvent;
import com.majortom.algorithms.library.sort.event.SortWrittenEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Stateless reducer shared by every integer-sort visualization. */
public final class IntegerSortEventReducer implements EventReducer<IntegerSortViewState> {

    @Override
    public IntegerSortViewState initialState() {
        return IntegerSortViewState.empty();
    }

    @Override
    public Reduction<IntegerSortViewState> reduce(IntegerSortViewState previous, ExecutionEvent event) {
        Object payload = event.payload();
        if (payload instanceof SortInitializedEvent initialized) {
            return changed(new IntegerSortViewState(initialized.values(), -1, -1, -1, -1,
                    -1, -1, -1, Set.of(), IntegerSortViewState.Phase.INITIALIZED, false),
                    EventImportance.CHECKPOINT);
        }
        if (payload instanceof SortComparedEvent compared) {
            return changed(copy(previous, previous.values(), compared.leftIndex(), compared.rightIndex(),
                    -1, -1, previous.rangeStart(), previous.rangeEnd(), previous.settledIndices(),
                    IntegerSortViewState.Phase.COMPARING, false), EventImportance.TRANSIENT);
        }
        if (payload instanceof SortWrittenEvent written) {
            List<Integer> values = new ArrayList<>(previous.values());
            requireIndex(written.index(), values.size());
            values.set(written.index(), written.value());
            return changed(copy(previous, values, written.index(), -1, -1, -1,
                    previous.rangeStart(), previous.rangeEnd(), previous.settledIndices(),
                    IntegerSortViewState.Phase.WRITING, false), EventImportance.STATE_CHANGE);
        }
        if (payload instanceof SortSwappedEvent swapped) {
            List<Integer> values = new ArrayList<>(previous.values());
            requireIndex(swapped.leftIndex(), values.size());
            requireIndex(swapped.rightIndex(), values.size());
            values.set(swapped.leftIndex(), swapped.leftValue());
            values.set(swapped.rightIndex(), swapped.rightValue());
            return changed(copy(previous, values, -1, -1, swapped.leftIndex(), swapped.rightIndex(),
                    previous.rangeStart(), previous.rangeEnd(), previous.settledIndices(),
                    IntegerSortViewState.Phase.SWAPPING, false), EventImportance.STATE_CHANGE);
        }
        if (payload instanceof SortRangeSelectedEvent selected) {
            return changed(copy(previous, previous.values(), -1, -1, -1, -1,
                    selected.lowIndex(), selected.highIndex(), previous.settledIndices(),
                    IntegerSortViewState.Phase.RANGE_SELECTED, false), EventImportance.STATE_CHANGE);
        }
        if (payload instanceof SortPivotSelectedEvent selected) {
            return changed(new IntegerSortViewState(previous.values(), -1, -1, -1, -1,
                    selected.index(), previous.rangeStart(), previous.rangeEnd(), previous.settledIndices(),
                    IntegerSortViewState.Phase.PIVOT_SELECTED, false), EventImportance.STATE_CHANGE);
        }
        if (payload instanceof SortElementSettledEvent settled) {
            requireIndex(settled.index(), previous.values().size());
            Set<Integer> indices = new LinkedHashSet<>(previous.settledIndices());
            indices.add(settled.index());
            return changed(copy(previous, previous.values(), -1, -1, -1, -1,
                    previous.rangeStart(), previous.rangeEnd(), indices,
                    IntegerSortViewState.Phase.SETTLED, false), EventImportance.STATE_CHANGE);
        }
        if (payload instanceof SortCompletedEvent completed) {
            Set<Integer> settled = new LinkedHashSet<>();
            for (int index = 0; index < completed.values().size(); index++) {
                settled.add(index);
            }
            return changed(new IntegerSortViewState(completed.values(), -1, -1, -1, -1,
                    -1, -1, -1, settled, IntegerSortViewState.Phase.COMPLETED, true),
                    EventImportance.TERMINAL);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static IntegerSortViewState copy(
            IntegerSortViewState previous,
            List<Integer> values,
            int comparedIndex,
            int insertionIndex,
            int swapLeftIndex,
            int swapRightIndex,
            int rangeStart,
            int rangeEnd,
            Set<Integer> settled,
            IntegerSortViewState.Phase phase,
            boolean completed) {
        return new IntegerSortViewState(values, comparedIndex, insertionIndex, swapLeftIndex,
                swapRightIndex, previous.pivotIndex(), rangeStart, rangeEnd, settled, phase, completed);
    }

    private static Reduction<IntegerSortViewState> changed(
            IntegerSortViewState state, EventImportance importance) {
        return Reduction.changed(state, importance, true);
    }

    private static void requireIndex(int index, int size) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Sort event index " + index + " is outside state size " + size);
        }
    }
}
