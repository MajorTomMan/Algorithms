package com.majortom.algorithms.visualization.common;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/** Reusable pan/zoom/center/fit viewport around one visual content group. */
public final class ViewportPane extends StackPane {
    private static final double MIN_SCALE = 0.1d;
    private static final double MAX_SCALE = 8.0d;

    private final Pane surface = new Pane();
    private final Group content = new Group();
    private double dragSceneX;
    private double dragSceneY;
    private double dragTranslateX;
    private double dragTranslateY;

    public ViewportPane() {
        getStyleClass().add("visual-viewport");
        surface.getChildren().add(content);
        getChildren().add(surface);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
        installPanAndZoom();
    }

    public Group content() {
        return content;
    }

    public double zoom() {
        return content.getScaleX();
    }

    public void reset() {
        content.setScaleX(1.0d);
        content.setScaleY(1.0d);
        center();
    }

    public void center() {
        Bounds bounds = content.getBoundsInLocal();
        if (bounds.isEmpty()) {
            content.setTranslateX(0.0d);
            content.setTranslateY(0.0d);
            return;
        }
        double scaledWidth = bounds.getWidth() * content.getScaleX();
        double scaledHeight = bounds.getHeight() * content.getScaleY();
        content.setTranslateX(getWidth() / 2.0d - (bounds.getMinX() * content.getScaleX() + scaledWidth / 2.0d));
        content.setTranslateY(getHeight() / 2.0d - (bounds.getMinY() * content.getScaleY() + scaledHeight / 2.0d));
    }

    public void fitToViewport() {
        Bounds bounds = content.getBoundsInLocal();
        if (bounds.isEmpty() || getWidth() <= 0.0d || getHeight() <= 0.0d) {
            return;
        }
        double availableWidth = Math.max(1.0d, getWidth() * 0.9d);
        double availableHeight = Math.max(1.0d, getHeight() * 0.9d);
        double widthScale = bounds.getWidth() == 0.0d ? MAX_SCALE : availableWidth / bounds.getWidth();
        double heightScale = bounds.getHeight() == 0.0d ? MAX_SCALE : availableHeight / bounds.getHeight();
        setZoom(clamp(Math.min(widthScale, heightScale)));
        center();
    }

    public void setZoom(double zoom) {
        double next = clamp(zoom);
        content.setScaleX(next);
        content.setScaleY(next);
    }

    private void installPanAndZoom() {
        setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY && event.getButton() != MouseButton.MIDDLE) {
                return;
            }
            dragSceneX = event.getSceneX();
            dragSceneY = event.getSceneY();
            dragTranslateX = content.getTranslateX();
            dragTranslateY = content.getTranslateY();
            event.consume();
        });
        setOnMouseDragged(event -> {
            if (!event.isPrimaryButtonDown() && !event.isMiddleButtonDown()) {
                return;
            }
            content.setTranslateX(dragTranslateX + event.getSceneX() - dragSceneX);
            content.setTranslateY(dragTranslateY + event.getSceneY() - dragSceneY);
            event.consume();
        });
        setOnScroll(event -> {
            double previousScale = zoom();
            double factor = event.getDeltaY() >= 0.0d ? 1.12d : 1.0d / 1.12d;
            double nextScale = clamp(previousScale * factor);
            if (nextScale == previousScale) {
                return;
            }
            Point2D mouse = sceneToLocal(event.getSceneX(), event.getSceneY());
            double ratio = nextScale / previousScale;
            content.setTranslateX(mouse.getX() - (mouse.getX() - content.getTranslateX()) * ratio);
            content.setTranslateY(mouse.getY() - (mouse.getY() - content.getTranslateY()) * ratio);
            setZoom(nextScale);
            event.consume();
        });
    }

    private double clamp(double value) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
    }
}
