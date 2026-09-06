package com.majortom.algorithms.visualization.launcher;

import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Screen;

/**
 * 主界面场景构造器。
 *
 * <p>窗口优先保持 1600x900 参考尺寸，但不会超过当前屏幕的可用区域。
 * 高 DPI / 笔记本环境下直接以 Screen visual bounds 计算初始 Scene，避免
 * 先创建一个大于桌面的窗口再依赖系统缩放或整页 scale。</p>
 */
public final class ResponsiveStageScaler {

    public static final double DEFAULT_WIDTH = 1600.0d;
    public static final double DEFAULT_HEIGHT = 900.0d;
    public static final double MIN_WIDTH = 840.0d;
    public static final double MIN_HEIGHT = 540.0d;
    private static final double SCREEN_USAGE = 0.94d;

    private ResponsiveStageScaler() {
    }

    public static Scene createScene(Parent content, double designWidth, double designHeight) {
        if (content instanceof Region region) {
            region.setPrefSize(designWidth, designHeight);
            region.setMinSize(0, 0);
        }
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double width = initialDimension(designWidth, MIN_WIDTH, visualBounds.getWidth());
        double height = initialDimension(designHeight, MIN_HEIGHT, visualBounds.getHeight());
        return new Scene(content, width, height);
    }

    public static double stageMinWidth() {
        return Math.min(MIN_WIDTH, Screen.getPrimary().getVisualBounds().getWidth());
    }

    public static double stageMinHeight() {
        return Math.min(MIN_HEIGHT, Screen.getPrimary().getVisualBounds().getHeight());
    }

    private static double initialDimension(double design, double minimum, double available) {
        if (available <= 0.0d) {
            return Math.max(minimum, design);
        }
        double preferred = Math.min(design, available * SCREEN_USAGE);
        double floor = Math.min(minimum, available);
        return Math.min(available, Math.max(floor, preferred));
    }
}
