package com.majortom.algorithms.visualization.common.view;

import com.majortom.algorithms.visualization.common.geometry.CircleGeometry;
import com.majortom.algorithms.visualization.common.geometry.NodeGeometry;
import com.majortom.algorithms.visualization.common.geometry.RectangleGeometry;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

import java.util.Objects;

/** Visual node primitive. It knows geometry and visual state, never structure relationships. */
public final class NodeView extends StackPane {
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");

    private final ObjectProperty<NodeGeometry> geometry = new SimpleObjectProperty<>();
    private final DoubleProperty centerX = new SimpleDoubleProperty();
    private final DoubleProperty centerY = new SimpleDoubleProperty();
    private final BooleanProperty selected = new SimpleBooleanProperty();
    private final BooleanProperty highlighted = new SimpleBooleanProperty();
    private final Text label = new Text();
    private Shape selectionRing;
    private Shape shape;

    public NodeView(NodeGeometry geometry, String text) {
        getStyleClass().add("visual-node");
        label.getStyleClass().add("visual-node-label");
        this.geometry.addListener((observable, previous, current) -> rebuildShape(current));
        centerX.addListener(observable -> updatePosition());
        centerY.addListener(observable -> updatePosition());
        selected.addListener((observable, previous, current) -> pseudoClassStateChanged(SELECTED, current));
        highlighted.addListener((observable, previous, current) -> pseudoClassStateChanged(HIGHLIGHTED, current));
        setGeometry(geometry);
        setText(text);
    }

    public NodeGeometry getGeometry() {
        return geometry.get();
    }

    public void setGeometry(NodeGeometry geometry) {
        this.geometry.set(Objects.requireNonNull(geometry, "geometry"));
    }

    public ObjectProperty<NodeGeometry> geometryProperty() {
        return geometry;
    }

    public String getText() {
        return label.getText();
    }

    public void setText(String text) {
        label.setText(text == null ? "" : text);
    }

    /** Current CSS-resolved label bounds for family-specific size measurement. */
    public Bounds labelBounds() {
        return label.getLayoutBounds();
    }

    public double getCenterX() {
        return centerX.get();
    }

    public DoubleProperty centerXProperty() {
        return centerX;
    }

    public double getCenterY() {
        return centerY.get();
    }

    public DoubleProperty centerYProperty() {
        return centerY;
    }

    public Point2D center() {
        return new Point2D(getCenterX(), getCenterY());
    }

    public void setCenter(double x, double y) {
        centerX.set(x);
        centerY.set(y);
    }

    public Point2D boundaryPointToward(Point2D target) {
        return getGeometry().boundaryPoint(center(), target);
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selected;
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

    private void rebuildShape(NodeGeometry geometry) {
        if (geometry instanceof CircleGeometry circle) {
            shape = new Circle(circle.radius());
            selectionRing = new Circle(circle.radius() + 5.0d);
        } else if (geometry instanceof RectangleGeometry rectangle) {
            shape = new Rectangle(rectangle.width(), rectangle.height());
            selectionRing = new Rectangle(rectangle.width() + 10.0d, rectangle.height() + 10.0d);
        } else {
            throw new IllegalArgumentException("Unsupported geometry: " + geometry.getClass().getName());
        }
        selectionRing.getStyleClass().add("visual-node-selection-ring");
        selectionRing.setMouseTransparent(true);
        selectionRing.visibleProperty().bind(selected);
        shape.getStyleClass().add("visual-node-shape");
        getChildren().setAll(selectionRing, shape, label);
        setMinSize(geometry.width(), geometry.height());
        setPrefSize(geometry.width(), geometry.height());
        setMaxSize(geometry.width(), geometry.height());
        updatePosition();
    }

    private void updatePosition() {
        NodeGeometry value = geometry.get();
        if (value == null) {
            return;
        }
        relocate(getCenterX() - value.width() / 2.0d, getCenterY() - value.height() / 2.0d);
    }
}
