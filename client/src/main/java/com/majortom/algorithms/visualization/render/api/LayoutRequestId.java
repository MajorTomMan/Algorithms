package com.majortom.algorithms.visualization.render.api;

public record LayoutRequestId(long value) {
    public LayoutRequestId {
        if (value < 0L) throw new IllegalArgumentException("request id must be >= 0");
    }
}
