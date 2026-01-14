package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.visualization.base.BaseSortVisualizer;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * 柱状图排序可视化器 - 适配《乱》配色体系
 * 红色：常规状态
 * 蓝色：当前操作
 * 黄色：比较状态
 * 骨白：交换/高亮
 */
public class HistogramSortVisualizer<T extends Comparable<T>> extends BaseSortVisualizer<T> {

    @Override
    protected void drawSortContent(BaseSort<T> sortData, Object a, Object b) {
        T[] data = sortData.getData();
        int n = data.length;

        double canvasW = canvas.getWidth();
        double canvasH = canvas.getHeight();
        double paddingSide = 40.0;
        double paddingBottom = 50.0;

        double barW = (canvasW - 2 * paddingSide) / n;
        double maxAvailableH = canvasH - paddingBottom - 40.0;
        double maxVal = getMaxValue(data);
        double scale = maxAvailableH / maxVal;

        for (int i = 0; i < n; i++) {
            double val = Double.parseDouble(data[i].toString());
            double h = Math.max(2, val * scale);
            double x = paddingSide + i * barW;
            double y = canvasH - paddingBottom - h;

            // --- 状态判定逻辑 ---
            Color barColor = RAN_RED; // 默认：红色 (太郎)

            // 比较状态：黄色 (三郎)
            if (Objects.equals(i, a) || Objects.equals(i, b)) {
                barColor = RAN_GOLD;
            }

            // 活跃/移动状态：蓝色 (次郎)
            if (i == sortData.getActiveIndex()) {
                barColor = RAN_BLUE;
            }

            // 交换瞬态判定 (若 a, b 同时存在通常代表交换)
            if (a != null && b != null && (Objects.equals(i, a) || Objects.equals(i, b))) {
                // 可以在交换时使用骨白增强反馈，若不需要则维持黄色
                barColor = BONE_WHITE;
            }

            renderHardBar(x, y, barW, h, barColor);
        }
    }

    private void renderHardBar(double x, double y, double w, double h, Color color) {
        double gap = w > 4 ? 1.0 : 0.2;
        double actualW = Math.max(0.5, w - gap);

        // 填充硬边色块
        gc.setFill(color);
        gc.fillRect(x, y, actualW, h);

        // 顶部骨白细线
        if (w > 6) {
            gc.setStroke(BONE_WHITE.deriveColor(0, 1, 1, 0.5));
            gc.setLineWidth(1.0);
            gc.strokeLine(x, y, x + actualW, y);
        }
    }
}