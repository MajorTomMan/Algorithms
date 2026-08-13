package com.majortom.algorithms.library.sort.model;

import com.majortom.algorithms.core.api.AlgorithmInput;

import java.util.List;
import java.util.Objects;

/** Immutable integer sequence accepted by the production integer sorts. */
public record IntegerSortInput(List<Integer> values) implements AlgorithmInput {

    public static final int MAX_VALUES = 100_000;

    public IntegerSortInput {
        Objects.requireNonNull(values, "values");
        if (values.size() > MAX_VALUES) {
            throw new IllegalArgumentException("sort input must contain at most " + MAX_VALUES + " values");
        }
        values = List.copyOf(values);
    }
}
