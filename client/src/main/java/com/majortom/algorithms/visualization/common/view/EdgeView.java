package com.majortom.algorithms.visualization.common.view;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.QuadCurveTo;

import java.util.List;
import java.util.Objects;

/** Edge primitive with boundary attachment, straight/curved/self-loop paths and optional arrow. */
public final class EdgeView extends Group {
    private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");

    private final NodeView source;
    private final NodeView target;
    private final Path path = new Path();
    private final Polygon arrow = new Polygon();
    private final BooleanProperty directed = new SimpleBooleanProperty();
    private final BooleanProperty curved = new SimpleBooleanProperty();
    private final BooleanProperty highlighted = new SimpleBooleanProperty();
    private final InvalidationListener geometryListener = observable -> updateGeometry();
    private List<Point2D> route = List.of();

    public EdgeView(NodeView source, NodeView target, boolean directed) {
        this.source = Objects.requireNonNull(source, "source");
        this.target = Objects.requireNonNull(target, "target");
        getStyleClass().add("visual-edge");
        path.getStyleClass().add("visual-edge-path");
        path.setFill(null);
        path.setStroke(Color.GRAY);
        path.setStrokeWidth(2.0d);
        arrow.getStyleClass().add("visual-edge-arrow");
        arrow.fillProperty().bind(path.strokeProperty());
        getChildren().addAll(path, arrow);
        setMouseTransparent(true);

        source.centerXProperty().addListener(geometryListener);
        source.centerYProperty().addListener(geometryListener);
        source.geometryProperty().addListener(geometryListener);
        target.centerXProperty().addListener(geometryListener);
        target.centerYProperty().addListener(geometryListener);
        target.geometryProperty().addListener(geometryListener);
        this.directed.addListener(geometryListener);
        curved.addListener(geometryListener);
        highlighted.addListener((observable, previous, current) -> {
            pseudoClassStateChanged(HIGHLIGHTED, current);
            path.setStrokeWidth(current ? 3.5d : 2.0d);
        });
        setDirected(directed);
        updateGeometry();
    }

    public NodeView source() {
        return source;
    }

    public NodeView target() {
        return target;
    }

    public boolean isDirected() {
        return directed.get();
    }

    public void setDirected(boolean directed) {
        this.directed.set(directed);
    }

    public BooleanProperty directedProperty() {
        return directed;
    }

    public boolean isCurved() {
        return curved.get();
    }

    public void setCurved(boolean curved) {
        this.curved.set(curved);
    }

    public BooleanProperty curvedProperty() {
        return curved;
    }

    public boolean isHighlighted() {
        return highlighted.get();
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted.set(highlighted);
    }

    public BooleanProperty highlightedProperty() {
        return highlighted;
    }

    public Path path() {
        return path;
    }

    /** Applies presentation-only route geometry, typically produced by ELK. */
    public void setRoute(List<Point2D> points) {
        route = List.copyOf(Objects.requireNonNull(points, "points"));
        updateGeometry();
    }

    /** Restores project-owned dynamic source/target attachment while nodes are moving. */
    public void clearRoute() {
        if (route.isEmpty()) {
            return;
        }
        route = List.of();
        updateGeometry();
    }

    public boolean hasRoute() {
        return !route.isEmpty();
    }

    private void updateGeometry() {
        if (route.size() >= 2) {
            updateRoutedGeometry();
            return;
        }
        path.getElements().clear();
        Point2D sourceCenter = source.center();
        Point2D targetCenter = target.center();
        if (source == target) {
            updateSelfLoop(sourceCenter);
            return;
        }
        if (sourceCenter.equals(targetCenter)) {
            arrow.setVisible(false);
            return;
        }

        Point2D start = source.boundaryPointToward(targetCenter);
        Point2D end = target.boundaryPointToward(sourceCenter);
        path.getElements().add(new MoveTo(start.getX(), start.getY()));

        Point2D tangent;
        if (isCurved()) {
            Point2D delta = end.subtract(start);
            Point2D normal = new Point2D(-delta.getY(), delta.getX()).normalize();
            double offset = Math.max(28.0d, delta.magnitude() * 0.16d);
            Point2D control = start.midpoint(end).add(normal.multiply(offset));
            path.getElements().add(new QuadCurveTo(control.getX(), control.getY(), end.getX(), end.getY()));
            tangent = end.subtract(control);
        } else {
            path.getElements().add(new LineTo(end.getX(), end.getY()));
            tangent = end.subtract(start);
        }
        updateArrow(end, tangent);
    }

    private void updateRoutedGeometry() {
        path.getElements().clear();
        Point2D start = route.getFirst();
        path.getElements().add(new MoveTo(start.getX(), start.getY()));
        for (int index = 1; index < route.size(); index++) {
            Point2D point = route.get(index);
            path.getElements().add(new LineTo(point.getX(), point.getY()));
        }
        Point2D end = route.getLast();
        Point2D tangent = end.subtract(route.get(route.size() - 2));
        updateArrow(end, tangent);
    }

    private void updateSelfLoop(Point2D center) {
        Point2D start = source.boundaryPointToward(center.add(1.0d, -1.0d));
        Point2D end = source.boundaryPointToward(center.add(-1.0d, -1.0d));
        double width = source.getGeometry().width();
        double height = source.getGeometry().height();
        Point2D control1 = center.add(width * 1.1d, -height * 1.7d);
        Point2D control2 = center.add(-width * 1.1d, -height * 1.7d);
        path.getElements().add(new MoveTo(start.getX(), start.getY()));
        path.getElements().add(new CubicCurveTo(
                control1.getX(), control1.getY(),
                control2.getX(), control2.getY(),
                end.getX(), end.getY()));
        updateArrow(end, end.subtract(control2));
    }

    private void updateArrow(Point2D tip, Point2D tangent) {
        if (!isDirected() || tangent.magnitude() == 0.0d) {
            arrow.setVisible(false);
            return;
        }
        Point2D direction = tangent.normalize();
        Point2D normal = new Point2D(-direction.getY(), direction.getX());
        double length = 11.0d;
        double width = 5.5d;
        Point2D base = tip.subtract(direction.multiply(length));
        Point2D left = base.add(normal.multiply(width));
        Point2D right = base.subtract(normal.multiply(width));
        arrow.getPoints().setAll(
                tip.getX(), tip.getY(),
                left.getX(), left.getY(),
                right.getX(), right.getY());
        arrow.setVisible(true);
    }
}
