package com.majortom.algorithms.library.sort.event;

import java.util.List;
import java.util.Objects;

/** Supplies a terminal snapshot that can verify the replayed writes. */
public record SortCompletedEvent(List<Integer> values) implements IntegerSortEvent {

    public SortCompletedEvent {
        Objects.requireNonNull(values, "values");
        values = List.copyOf(values);
    }
}
