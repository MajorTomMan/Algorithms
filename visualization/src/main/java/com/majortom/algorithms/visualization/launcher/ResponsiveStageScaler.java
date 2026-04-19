package com.majortom.algorithms.visualization.launcher;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

/**
 * 为整套界面提供统一的等比缩放能力。
 * 启动时先根据可用视口自动计算一次缩放比例，待首轮布局稳定后冻结，
 * 避免持续监听 layoutBounds 导致的缩放-布局反馈抖动。
 */
public final class ResponsiveStageScaler {

    private static final double MIN_SCALE = 0.55;
    private static final double MAX_SCALE = 1.0;

    private ResponsiveStageScaler() {
    }

    public static Scene createScene(Parent content, double designWidth, double designHeight) {
        StackPane viewport = new StackPane();
        viewport.getStylesheets().setAll(content.getStylesheets());
        viewport.getStyleClass().add("root-pane");
        viewport.setAlignment(Pos.CENTER);
        viewport.setPrefSize(designWidth, designHeight);

        Scale scale = new Scale(1.0, 1.0, 0, 0);
        content.getTransforms().setAll(scale);

        Group scaledGroup = new Group(content);
        viewport.getChildren().add(scaledGroup);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);

        Scene scene = new Scene(viewport, designWidth, designHeight);

        DoubleBinding scaleBinding = Bindings.createDoubleBinding(() -> {
            double baseWidth = content.getLayoutBounds().getWidth() > 0
                    ? content.getLayoutBounds().getWidth()
                    : designWidth;
            double baseHeight = content.getLayoutBounds().getHeight() > 0
                    ? content.getLayoutBounds().getHeight()
                    : designHeight;

            double viewportWidth = viewport.getWidth() > 0 ? viewport.getWidth() : designWidth;
            double viewportHeight = viewport.getHeight() > 0 ? viewport.getHeight() : designHeight;

            double scaleX = viewportWidth / baseWidth;
            double scaleY = viewportHeight / baseHeight;
            return clamp(Math.min(scaleX, scaleY), MIN_SCALE, MAX_SCALE);
        }, viewport.widthProperty(), viewport.heightProperty(), content.layoutBoundsProperty());

        scale.xProperty().bind(scaleBinding);
        scale.yProperty().bind(scaleBinding);

        final Region region = (content instanceof Region r) ? r : null;
        final DoubleBinding contentWidthBinding;
        final DoubleBinding contentHeightBinding;

        if (region != null) {
            contentWidthBinding = Bindings.createDoubleBinding(() -> {
                double currentScale = scaleBinding.get();
                double viewportWidth = viewport.getWidth() > 0 ? viewport.getWidth() : designWidth;
                return currentScale <= 0 ? designWidth : viewportWidth / currentScale;
            }, viewport.widthProperty(), scaleBinding);

            contentHeightBinding = Bindings.createDoubleBinding(() -> {
                double currentScale = scaleBinding.get();
                double viewportHeight = viewport.getHeight() > 0 ? viewport.getHeight() : designHeight;
                return currentScale <= 0 ? designHeight : viewportHeight / currentScale;
            }, viewport.heightProperty(), scaleBinding);

            region.prefWidthProperty().bind(contentWidthBinding);
            region.prefHeightProperty().bind(contentHeightBinding);
        }

        freezeAfterInitialLayout(content, viewport, scale, scaleBinding, region, designWidth, designHeight);

        return scene;
    }

    /**
     * 等首轮 CSS、布局、窗口显示基本稳定后，把自动计算出来的缩放结果冻结。
     */
    private static void freezeAfterInitialLayout(
            Parent content,
            StackPane viewport,
            Scale scale,
            DoubleBinding scaleBinding,
            Region region,
            double designWidth,
            double designHeight) {
        Platform.runLater(() -> Platform.runLater(() -> {
            double fixedScale = scaleBinding.get();

            scale.xProperty().unbind();
            scale.yProperty().unbind();

            scale.setX(fixedScale);
            scale.setY(fixedScale);

            if (region != null) {
                double frozenPrefWidth = region.prefWidthProperty().isBound()
                        ? region.prefWidthProperty().get()
                        : (viewport.getWidth() > 0 ? viewport.getWidth() / fixedScale : designWidth);

                double frozenPrefHeight = region.prefHeightProperty().isBound()
                        ? region.prefHeightProperty().get()
                        : (viewport.getHeight() > 0 ? viewport.getHeight() / fixedScale : designHeight);

                region.prefWidthProperty().unbind();
                region.prefHeightProperty().unbind();

                region.setPrefWidth(frozenPrefWidth);
                region.setPrefHeight(frozenPrefHeight);
            }

            content.applyCss();
            content.layout();
        }));
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}