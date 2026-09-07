package com.majortom.algorithms.visualization.common.geometry;

import javafx.geometry.Point2D;

public record RectangleGeometry(double width, double height) implements NodeGeometry {
    public RectangleGeometry {
        if (width <= 0.0d || height <= 0.0d) {
            throw new IllegalArgumentException("rectangle dimensions must be positive");
        }
    }

    @Override
    public Point2D boundaryPoint(Point2D center, Point2D toward) {
        double dx = toward.getX() - center.getX();
        double dy = toward.getY() - center.getY();
        if (dx == 0.0d && dy == 0.0d) {
            return center;
        }
        double halfWidth = width / 2.0d;
        double halfHeight = height / 2.0d;
        double scaleX;
        if (dx == 0.0d) {
            scaleX = Double.POSITIVE_INFINITY;
        } else {
            scaleX = halfWidth / Math.abs(dx);
        }
        double scaleY;
        if (dy == 0.0d) {
            scaleY = Double.POSITIVE_INFINITY;
        } else {
            scaleY = halfHeight / Math.abs(dy);
        }
        double scale = Math.min(scaleX, scaleY);
        return new Point2D(center.getX() + dx * scale, center.getY() + dy * scale);
    }
}
