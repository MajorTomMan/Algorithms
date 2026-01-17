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
        // 增加底部留白，为数值显示预留空间 (从 50 增加到 70)
        double paddingBottom = 70.0;

        double barW = (canvasW - 2 * paddingSide) / n;
        // 顶部也预留一点空间
        double maxAvailableH = canvasH - paddingBottom - 60.0;
        double maxVal = getMaxValue(data);
        double scale = maxVal == 0 ? 1 : maxAvailableH / maxVal;

        for (int i = 0; i < n; i++) {
            double val = Double.parseDouble(data[i].toString());
            double h = Math.max(2, val * scale);
            double x = paddingSide + i * barW;
            double y = canvasH - paddingBottom - h;

            // --- 状态判定逻辑 (保持乱美学色彩体系) ---
            Color barColor = RAN_RED;

            if (Objects.equals(i, a) || Objects.equals(i, b)) {
                barColor = RAN_GOLD; // 比较状态：三郎黄
            }

            if (i == sortData.getActiveIndex()) {
                barColor = RAN_BLUE; // 活跃状态：次郎蓝
            }

            // 绘制柱体及下方数值
            renderHardBar(x, y, barW, h, barColor, data[i].toString());
        }
    }

    private void renderHardBar(double x, double y, double w, double h, Color color, String valueText) {
        double gap = w > 4 ? 1.5 : 0.2;
        double actualW = Math.max(0.5, w - gap);

        // 1. 填充硬边色块
        gc.setFill(color);
        gc.fillRect(x, y, actualW, h);

        // 2. 顶部骨白细线 (增强轮廓感)
        if (w > 6) {
            gc.setStroke(BONE_WHITE.deriveColor(0, 1, 1, 0.6));
            gc.setLineWidth(1.2);
            gc.strokeLine(x, y, x + actualW, y);
        }

        // 3. 绘制下方数值 - 极致对比度处理
        if (w > 12) { // 只有宽度足够时才显示文字，避免拥挤
            gc.setFill(BONE_WHITE);
            // 动态字体大小：根据柱宽微调，最大 12px，最小 9px
            double fontSize = Math.min(12, Math.max(9, w * 0.6));
            gc.setFont(javafx.scene.text.Font.font("Consolas", fontSize));

            // 计算文字居中位置
            javafx.scene.text.Text textNode = new javafx.scene.text.Text(valueText);
            textNode.setFont(gc.getFont());
            double textWidth = textNode.getLayoutBounds().getWidth();

            // 在柱子下方 15 像素处绘制
            gc.fillText(valueText, x + (actualW - textWidth) / 2, y + h + 20);
        }
    }
}