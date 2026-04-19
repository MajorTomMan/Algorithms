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

    private ResponsiveStageScaler() {
    }

    public static Scene createScene(Parent content, double designWidth, double designHeight) {
        if (content instanceof Region region) {
            region.setPrefSize(designWidth, designHeight);
            region.setMinSize(0, 0);
        }
        return new Scene(content, designWidth, designHeight);
    }
}
