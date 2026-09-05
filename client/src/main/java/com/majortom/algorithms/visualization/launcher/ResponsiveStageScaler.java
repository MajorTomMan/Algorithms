package com.majortom.algorithms.visualization.launcher;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;

/**
 * 主界面场景构造器。
 * 当前策略是让 BorderPane 按正常布局参与窗口伸缩，
 * 不再对整页 UI 做统一缩放，以避免底部控制栏和侧边栏被压缩裁切。
 */
public final class ResponsiveStageScaler {

    public static final double DEFAULT_WIDTH = 1600.0d;
    public static final double DEFAULT_HEIGHT = 900.0d;
    public static final double MIN_WIDTH = 960.0d;
    public static final double MIN_HEIGHT = 640.0d;

    private ResponsiveStageScaler() {
    }

    public static Scene createScene(Parent content, double designWidth, double designHeight) {
        if (content instanceof Region region) {
            region.setPrefSize(designWidth, designHeight);
            region.setMinSize(0, 0);
        }
        double width = Math.max(MIN_WIDTH, designWidth);
        double height = Math.max(MIN_HEIGHT, designHeight);
        return new Scene(content, width, height);
    }
}
