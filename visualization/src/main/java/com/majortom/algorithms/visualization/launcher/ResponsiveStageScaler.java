package com.majortom.algorithms.visualization.launcher;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;

/**
 * 为整套界面提供统一的等比缩放能力。
 * 使用 Group 承载变换后的布局边界，避免控件在缩放后被裁切或局部丢失。
 */
public final class ResponsiveStageScaler {

    private static final double MIN_SCALE = 0.55;
    private static final double MAX_SCALE = 1.4;

    private ResponsiveStageScaler() {
    }

    public static Scene createScene(Parent content, double designWidth, double designHeight) {
        StackPane viewport = new StackPane();
        viewport.getStylesheets().setAll(content.getStylesheets());
        viewport.getStyleClass().add("root-pane");

        Scale scale = new Scale(1.0, 1.0, 0, 0);
        content.getTransforms().setAll(scale);

        Group scaledGroup = new Group(content);
        viewport.getChildren().add(scaledGroup);

        Scene scene = new Scene(viewport, designWidth, designHeight);

        DoubleBinding scaleBinding = Bindings.createDoubleBinding(() -> {
            double scaleX = scene.getWidth() / designWidth;
            double scaleY = scene.getHeight() / designHeight;
            return clamp(Math.min(scaleX, scaleY), MIN_SCALE, MAX_SCALE);
        }, scene.widthProperty(), scene.heightProperty());

        scale.xProperty().bind(scaleBinding);
        scale.yProperty().bind(scaleBinding);

        return scene;
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
