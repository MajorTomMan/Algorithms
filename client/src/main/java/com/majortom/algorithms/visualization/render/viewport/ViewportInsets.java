package com.majortom.algorithms.visualization.render.viewport;

public record ViewportInsets(double top, double right, double bottom, double left) {
    public static ViewportInsets none() {
        return new ViewportInsets(0, 0, 0, 0);
    }
}
