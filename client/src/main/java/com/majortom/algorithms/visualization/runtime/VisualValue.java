package com.majortom.algorithms.visualization.runtime;

import java.util.Objects;

/** JavaFX-neutral display projection for an arbitrary runtime value. */
public record VisualValue(Object value, String text) {
    public VisualValue {
        text = Objects.requireNonNull(text, "text");
    }

    public static VisualValue of(Object value) {
        return new VisualValue(value, String.valueOf(value));
    }

    public <T> T as(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return type.cast(value);
    }

    @Override
    public String toString() {
        return text;
    }
}
