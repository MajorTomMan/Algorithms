package com.majortom.algorithms.core.snapshot;

import java.util.List;
import java.util.Objects;

public record SequenceSnapshot<T>(List<T> values) {

    public SequenceSnapshot {
        values = List.copyOf(Objects.requireNonNull(values));
    }
}
