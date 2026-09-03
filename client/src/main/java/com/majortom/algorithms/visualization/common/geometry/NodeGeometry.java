package com.majortom.algorithms.visualization.common.geometry;

import javafx.geometry.Point2D;

/** Pure visual boundary geometry used to attach edges to node borders. */
public interface NodeGeometry {
    Point2D boundaryPoint(Point2D center, Point2D toward);

    double width();

    double height();
}
