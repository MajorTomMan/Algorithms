package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/**
 * 柱状图排序可视化器 (重构版)
 * 职责：将 BaseSort 实体映射为高度不一的霓虹柱体，支持动态高亮与玻璃质感渲染。
 * * @param <T> 排序元素类型
 */
public class HistogramSortVisualizer<T extends Comparable<T>> extends BaseVisualizer<BaseSort<T>> {

    // 🚩 定义符合你气质的色卡 (从基类色调扩展)
    private final Color COLOR_BAR_BASE = baseColor; // 冷灰色 #CFD8DC
    private final Color COLOR_ACTIVE = highlightColor; // 忧郁紫 #7E57C2
    private final Color COLOR_COMPARE = Color.web("#00ACC1"); // 霓虹青

    @Override
    protected void draw(BaseSort<T> sortData, Object a, Object b) {
        if (sortData == null || sortData.getData() == null) {
            clear();
            return;
        }

        T[] data = sortData.getData();
        int n = data.length;
        if (n == 0)
            return;

        // 1. 基础布局计算
        double padding = 40.0; // 留出呼吸感
        double canvasW = canvas.getWidth();
        double canvasH = canvas.getHeight();

        if (canvasW <= 0 || canvasH <= 0)
            return;

        double barW = (canvasW - 2 * padding) / n;
        double maxAvailableH = canvasH - 2 * padding;

        // 获取数组中的最大值用于归一化高度
        double maxVal = 0;
        for (T item : data) {
            double v = Double.parseDouble(item.toString());
            if (v > maxVal)
                maxVal = v;
        }
        double scale = (maxVal == 0) ? 1 : maxAvailableH / maxVal;

        // 2. 预清空画布 (背景色已经在 BaseVisualizer 定义)
        clear();

        // 3. 循环绘制每一根“通电”柱体
        for (int i = 0; i < n; i++) {
            double val = Double.parseDouble(data[i].toString());
            double h = Math.max(3, val * scale); // 确保即便值很小也至少可见
            double x = padding + i * barW;
            double y = canvasH - padding - h;

            // 🚩 判定颜色状态：a 通常代表“主操作/交换”，b 代表“对比”
            Color barColor = COLOR_BAR_BASE;

            if (a instanceof Integer && (int) a == i) {
                barColor = COLOR_ACTIVE;
            } else if (b instanceof Integer && (int) b == i) {
                barColor = COLOR_COMPARE;
            } else if (i == sortData.getActiveIndex()) {
                barColor = COLOR_ACTIVE;
            }

            renderBar(x, y, barW, h, barColor);
        }
    }

    /**
     * 绘制具有玻璃质感的渲染逻辑
     */
    private void renderBar(double x, double y, double w, double h, Color color) {
        double margin = w > 3 ? 0.8 : 0.0;
        double actualW = Math.max(0.1, w - 2 * margin);

        // 使用线性渐变模拟光影从左侧打来的质感
        LinearGradient gradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, color.deriveColor(0, 0.7, 1.3, 1)), // 亮边
                new Stop(0.4, color), // 基色
                new Stop(1, color.deriveColor(0, 1.0, 0.7, 1))); // 暗部

        gc.setFill(gradient);

        // 绘制圆角矩形
        double arc = Math.min(w, 6);
        gc.fillRoundRect(x + margin, y, actualW, h, arc, arc);

        // 🚩 视觉增强：当柱子足够宽时，增加顶部的“反光条”
        if (w > 5) {
            gc.setStroke(Color.rgb(255, 255, 255, 0.3));
            gc.setLineWidth(1.0);
            gc.strokeLine(x + margin + 1, y + 1.5, x + margin + actualW - 1, y + 1.5);
        }
    }
}