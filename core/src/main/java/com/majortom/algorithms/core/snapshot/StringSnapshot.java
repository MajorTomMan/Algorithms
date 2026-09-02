package com.majortom.algorithms.core.snapshot;

import java.util.Objects;

public record StringSnapshot(String value) {
    public StringSnapshot { value = Objects.requireNonNull(value, "value"); }
}
