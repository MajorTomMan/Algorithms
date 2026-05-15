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
 *
 * <p>使用 Group 承载变换后的布局边界，避免控件在缩放后被裁切或局部丢失。</p>
 */
public final class ResponsiveStageScaler {

    private static final double MIN_SCALE = 0.55;
    private static final double MAX_SCALE = 1.4;

    /**
     * 工具类不允许实例化。
     */
    private ResponsiveStageScaler() {
    }

    /**
     * 创建带整体等比缩放能力的 Scene。
     *
     * @param content 原始界面根节点
     * @param designWidth 设计稿宽度
     * @param designHeight 设计稿高度
     * @return 可随窗口大小等比缩放的 Scene
     */
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

    /**
     * 将缩放值限制在允许范围内。
     *
     * @param value 原始缩放值
     * @param min 最小缩放
     * @param max 最大缩放
     * @return 限制后的缩放值
     */
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
