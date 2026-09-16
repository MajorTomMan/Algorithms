package com.majortom.algorithms.visualization.render.api;

import java.util.List;
import java.util.Objects;

public record EdgeGeometry(String id, List<Point> points) {
    public EdgeGeometry {
        Objects.requireNonNull(id, "id");
        points = List.copyOf(Objects.requireNonNull(points, "points"));
    }

    public record Point(double x, double y) {}
}
