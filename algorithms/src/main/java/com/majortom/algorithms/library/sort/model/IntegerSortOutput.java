package com.majortom.algorithms.library.sort.model;


import java.util.List;
import java.util.Objects;

/** Immutable sorted result. */
public record IntegerSortOutput(List<Integer> values) {

    public IntegerSortOutput {
        Objects.requireNonNull(values, "values");
        values = List.copyOf(values);
    }
}
