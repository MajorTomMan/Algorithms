package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.visualization.base.BaseSortVisualizer;
import javafx.scene.paint.Color;
import java.util.Objects;

/**
 * 柱状图排序可视化具体实现
 * 职责：执行基于《乱》美学的柱体绘制，支持动态文字渲染。
 */
public class HistogramSortVisualizer<T extends Comparable<T>> extends BaseSortVisualizer<T> {

    @Override
    protected void drawSortContent(BaseSort<T> sortData, Object a, Object b,
            double barW, double scale,
            double offsetLeft, double offsetBottom) {
        T[] data = sortData.getData();
        double canvasH = canvas.getHeight();

        for (int i = 0; i < data.length; i++) {
            double val = Double.parseDouble(data[i].toString());
            double h = Math.max(2, val * scale); // 确保极小值也能被看见
            double x = offsetLeft + i * barW;
            double y = canvasH - offsetBottom - h;

            // --- 状态判定逻辑 (引用父类 Ran 调色盘) ---
            Color barColor = RAN_RED; // 太郎红：基础状态

            if (Objects.equals(i, a) || Objects.equals(i, b)) {
                barColor = RAN_GOLD; // 三郎黄：比较状态
            }

            if (i == sortData.getActiveIndex()) {
                barColor = RAN_BLUE; // 次郎蓝：行动/活跃焦点
            }

            // 执行具体的柱体与文字绘制
            renderRanBar(x, y, barW, h, barColor, data[i].toString());
        }
    }

    /**
     * 绘制具有“硬边”质感的柱体
     */
    private void renderRanBar(double x, double y, double w, double h, Color color, String valueText) {
        double gap = w > 4 ? 1.5 : 0.2;
        double actualW = Math.max(0.5, w - gap);

        // 1. 绘制核心色块
        gc.setFill(color);
        gc.fillRect(x, y, actualW, h);

        // 2. 顶部轮廓增强 (骨白细线)
        if (w > 6) {
            gc.setStroke(BONE_WHITE.deriveColor(0, 1, 1, 0.4));
            gc.setLineWidth(1.0);
            gc.strokeLine(x, y, x + actualW, y);
        }

        // 3. 数值渲染 (复用父类字体工具)
        if (w > 15) {
            double textY = y + h + 22; // 在柱子底部留白处绘制
            // 使用父类提供的统一文字绘制接口
            drawText(valueText, x + actualW / 2, textY, BONE_WHITE, Math.min(11, w * 0.5), false);
        }
    }
}