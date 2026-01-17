package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.visualization.base.BaseSortVisualizer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 柱状图排序可视化器
 * 专注于柱体的几何计算与文本渲染
 */
public class HistogramSortVisualizer<T extends Comparable<T>> extends BaseSortVisualizer<T> {

    @Override
    protected void drawSortContent(BaseSort<T> sortData, Object a, Object b) {
        T[] data = sortData.getData();
        int n = data.length;

        double canvasW = canvas.getWidth();
        double canvasH = canvas.getHeight();
        double paddingSide = 40.0;
        double paddingBottom = 70.0;
        double paddingTop = 60.0;

        double barW = (canvasW - 2 * paddingSide) / n;
        double maxAvailableH = canvasH - paddingBottom - paddingTop;
        double maxVal = getMaxValue(data);
        double scale = maxVal == 0 ? 1 : maxAvailableH / maxVal;

        for (int i = 0; i < n; i++) {
            double val = Double.parseDouble(data[i].toString());
            double h = Math.max(2, val * scale);
            double x = paddingSide + i * barW;
            double y = canvasH - paddingBottom - h;

            // 使用基类上浮的色彩引擎
            Color barColor = getRanColor(i, sortData, a, b);

            renderHardBar(x, y, barW, h, barColor, data[i].toString());
        }
    }

    private void renderHardBar(double x, double y, double w, double h, Color color, String valueText) {
        double gap = w > 4 ? 1.5 : 0.2;
        double actualW = Math.max(0.5, w - gap);

        // 1. 填充硬边色块
        gc.setFill(color);
        gc.fillRect(x, y, actualW, h);

        // 2. 顶部骨白细线
        if (w > 6) {
            gc.setStroke(RAN_WHITE.deriveColor(0, 1, 1, 0.6));
            gc.setLineWidth(1.2);
            gc.strokeLine(x, y, x + actualW, y);
        }

        // 3. 绘制下方数值
        if (w > 12) {
            gc.setFill(RAN_WHITE);
            double fontSize = Math.min(12, Math.max(9, w * 0.6));
            gc.setFont(Font.font("Consolas", fontSize));

            // 使用基类可能存在的辅助方法或直接绘制
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.fillText(valueText, x + actualW / 2, y + h + 20);
        }
    }
}