package com.majortom.algorithms.library.sort.event;

import java.util.List;
import java.util.Objects;

/** Establishes the initial replay state. */
public record SortInitializedEvent(List<Integer> values) implements IntegerSortEvent {

    public SortInitializedEvent {
        Objects.requireNonNull(values, "values");
        values = List.copyOf(values);
    }
}
