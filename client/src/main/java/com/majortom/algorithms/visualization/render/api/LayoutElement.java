package com.majortom.algorithms.visualization.render.api;

import java.util.Objects;

public record LayoutElement(String id, double width, double height) {
    public LayoutElement {
        Objects.requireNonNull(id, "id");
        if (!(width > 0.0d) || !(height > 0.0d))
            throw new IllegalArgumentException("element size must be positive: " + id);
    }
}
