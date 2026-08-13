package com.majortom.algorithms.library.sort.model;

import com.majortom.algorithms.core.api.AlgorithmOutput;

import java.util.List;
import java.util.Objects;

/** Immutable sorted result. */
public record IntegerSortOutput(List<Integer> values) implements AlgorithmOutput {

    public IntegerSortOutput {
        Objects.requireNonNull(values, "values");
        values = List.copyOf(values);
    }
}
