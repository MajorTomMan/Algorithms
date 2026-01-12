package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.visualization.base.BaseSortVisualizer;
import javafx.scene.paint.Color;

public class HistogramSortVisualizer<T extends Comparable<T>> extends BaseSortVisualizer<T> {

    @Override
    protected void drawSortContent(BaseSort<T> sortData, Object a, Object b) {
        T[] data = sortData.getData();
        int n = data.length;

        double padding = 30.0;
        double w = (getWidth() - 2 * padding) / n;
        double maxH = getHeight() - 2 * padding;
        double scale = maxH / getMaxValue(data);

        for (int i = 0; i < n; i++) {
            double val = Double.parseDouble(data[i].toString());
            double h = val * scale;
            double x = padding + i * w;
            double y = getHeight() - padding - h;

            // 确定颜色逻辑
            Color color = COLOR_BAR_BASE;
            if (i == sortData.getActiveIndex() || (a instanceof Integer && (int) a == i)) {
                color = COLOR_ACTIVE;
            } else if (i == sortData.getCompareIndex() || (b instanceof Integer && (int) b == i)) {
                color = COLOR_COMPARE;
            }

            // 执行绘制
            renderBar(x, y, w, h, color);
        }
    }

    private void renderBar(double x, double y, double w, double h, Color color) {
        gc.setFill(color);
        double margin = w > 3 ? 1.0 : 0.0;
        // 绘制带有一点圆角的霓虹柱体
        gc.fillRoundRect(x + margin, y, Math.max(0.1, w - 2 * margin), h, Math.min(w, 5), Math.min(w, 5));

        // 如果宽度足够，增加顶部高亮线
        if (w > 5) {
            gc.setStroke(Color.rgb(255, 255, 255, 0.3));
            gc.strokeLine(x + margin + 1, y, x + w - margin - 1, y);
        }
    }
}