package com.majortom.algorithms.visualization.common.geometry;

import javafx.geometry.Point2D;

public record CircleGeometry(double radius) implements NodeGeometry {
    public CircleGeometry {
        if (radius <= 0.0d) {
            throw new IllegalArgumentException("radius must be positive");
        }
    }

    @Override
    public Point2D boundaryPoint(Point2D center, Point2D toward) {
        Point2D delta = toward.subtract(center);
        if (delta.magnitude() == 0.0d) {
            return center;
        }
        return center.add(delta.normalize().multiply(radius));
    }

    @Override
    public double width() {
        return radius * 2.0d;
    }

    @Override
    public double height() {
        return radius * 2.0d;
    }
}
