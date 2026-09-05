package com.majortom.algorithms.visualization.impl.visualizer.linked;

import com.majortom.algorithms.visualization.common.geometry.RectangleGeometry;
import com.majortom.algorithms.visualization.common.view.NodeView;
import javafx.beans.InvalidationListener;
import javafx.scene.Group;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

/** Pointer-compartment decoration for one logical linked-list node. */
final class LinkedNodeDecoration extends Group {
    private final NodeView node;
    private final Line divider = new Line();
    private final Line portDivider = new Line();
    private final Text previousLabel = new Text("prev");
    private final Text nextLabel = new Text("next");
    private final Circle previousPort = new Circle(3.4d);
    private final Circle nextPort = new Circle(3.4d);
    private final InvalidationListener geometryListener = observable -> updateGeometry();

    LinkedNodeDecoration(NodeView node) {
        this.node = node;
        getStyleClass().add("linked-node-decoration");
        divider.getStyleClass().add("linked-node-divider");
        portDivider.getStyleClass().add("linked-node-port-divider");
        previousLabel.getStyleClass().add("linked-port-label");
        nextLabel.getStyleClass().add("linked-port-label");
        previousPort.getStyleClass().add("linked-port-dot");
        nextPort.getStyleClass().add("linked-port-dot");
        getChildren().addAll(divider, portDivider, previousLabel, nextLabel, previousPort, nextPort);
        setMouseTransparent(true);
        node.centerXProperty().addListener(geometryListener);
        node.centerYProperty().addListener(geometryListener);
        node.geometryProperty().addListener(geometryListener);
        updateGeometry();
    }

    void setLinks(Long previousId, Long nextId) {
        setEmpty(previousPort, previousId == null);
        setEmpty(nextPort, nextId == null);
    }

    void dispose() {
        node.centerXProperty().removeListener(geometryListener);
        node.centerYProperty().removeListener(geometryListener);
        node.geometryProperty().removeListener(geometryListener);
    }

    private void updateGeometry() {
        if (!(node.getGeometry() instanceof RectangleGeometry geometry)) {
            return;
        }
        double cx = node.getCenterX();
        double cy = node.getCenterY();
        double left = cx - geometry.width() / 2.0d;
        double right = cx + geometry.width() / 2.0d;
        double top = cy - geometry.height() / 2.0d;
        double bottom = cy + geometry.height() / 2.0d;
        double splitY = top + 40.0d;
        double middle = cx;

        divider.setStartX(left + 1.0d);
        divider.setEndX(right - 1.0d);
        divider.setStartY(splitY);
        divider.setEndY(splitY);

        portDivider.setStartX(middle);
        portDivider.setEndX(middle);
        portDivider.setStartY(splitY);
        portDivider.setEndY(bottom - 1.0d);

        previousLabel.relocate(left + geometry.width() * 0.18d, splitY + 4.0d);
        nextLabel.relocate(left + geometry.width() * 0.66d, splitY + 4.0d);
        previousPort.setCenterX(left + geometry.width() * 0.25d);
        nextPort.setCenterX(left + geometry.width() * 0.75d);
        previousPort.setCenterY(bottom - 5.0d);
        nextPort.setCenterY(bottom - 5.0d);
    }

    private static void setEmpty(Circle port, boolean empty) {
        if (empty) {
            if (!port.getStyleClass().contains("linked-port-empty")) {
                port.getStyleClass().add("linked-port-empty");
            }
        } else {
            port.getStyleClass().remove("linked-port-empty");
        }
    }
}
