package com.majortom.algorithms.server.request;

import java.util.List;
import java.util.Objects;

/** HTTP request body for integer sorting algorithms. */
public record IntegerSortRequest(List<Integer> values) {

    private static final int MAX_VALUES = 100_000;

    public IntegerSortRequest {
        values = List.copyOf(Objects.requireNonNull(values, "values"));
        if (values.size() > MAX_VALUES) {
            throw new IllegalArgumentException("sort input must contain at most " + MAX_VALUES + " values");
        }
    }
}
