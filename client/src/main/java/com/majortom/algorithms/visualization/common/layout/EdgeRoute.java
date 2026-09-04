package com.majortom.algorithms.visualization.common.layout;

import javafx.geometry.Point2D;

import java.util.List;
import java.util.Objects;

/** Presentation-only routed edge geometry. */
public record EdgeRoute(String id, List<Point2D> points) {
    public EdgeRoute {
        Objects.requireNonNull(id, "id");
        points = List.copyOf(Objects.requireNonNull(points, "points"));
    }
}
