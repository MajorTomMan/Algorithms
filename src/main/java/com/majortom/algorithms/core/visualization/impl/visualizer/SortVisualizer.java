package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * 排序算法可视化画布 (JavaFX 版)
 * 职责：
 * 1. 自动适配海量数据绘制（降采样逻辑）。
 * 2. 响应式高亮活跃元素。
 * 3. 适配硬件加速 Canvas 渲染。
 */
public class SortVisualizer extends BaseVisualizer<int[]> {

    // 默认配色方案（可适配 AtlantaFX 主题）
    private final Color NORMAL_BAR_COLOR = Color.web("#61AFEF"); // 科技蓝
    private final Color ACTIVE_BAR_COLOR = Color.web("#E06C75"); // 亮红色

    public SortVisualizer(int[] data) {
        super(data);
    }

    @Override
    protected void onMeasure(double width, double height) {
        // JavaFX 的 Canvas 尺寸由 bind 自动管理，此处无需手动计算 cellSize
    }

    @Override
    protected void draw(GraphicsContext gc, double width, double height) {
        int[] currentData = data.get();
        if (currentData == null || currentData.length == 0)
            return;

        int maxVal = getMaxValue(currentData);
        double viewW = width - 2 * padding;
        double viewH = height - 2 * padding;

        // 核心优化：降采样逻辑
        double step = Math.max(1.0, (double) currentData.length / viewW);
        double barWidth = Math.max(1.0, viewW / (currentData.length / step));

        for (double i = 0; i < currentData.length; i += step) {
            int idx = (int) i;

            // 映射坐标计算 (double 精度避免像素缝隙)
            double x = padding + (i / currentData.length) * viewW;
            double h = ((double) currentData[idx] / maxVal) * viewH;
            double y = height - padding - h;

            // 高亮逻辑判断
            if (isActive(idx, currentData.length, width)) {
                gc.setFill(ACTIVE_BAR_COLOR);
            } else {
                gc.setFill(NORMAL_BAR_COLOR);
            }

            // 绘制逻辑
            if (barWidth > 2) {
                // JavaFX 的圆角矩形绘制：x, y, w, h, arcW, arcH
                gc.fillRoundRect(x, y, barWidth - 1, h, 4, 4);
            } else {
                gc.fillRect(x, y, barWidth, h);
            }
        }
    }

    /**
     * 判断当前索引是否处于活跃状态（正在比较或移动）
     */
    private boolean isActive(int idx, int dataLength, double viewWidth) {
        Object a = activeA.get();
        Object b = activeB.get();

        if (a == null && b == null)
            return false;

        // 采样模式下的红点补偿阈值（确保极细 bar 模式下高亮依然可见）
        int threshold = Math.max(1, (int) (dataLength / viewWidth));

        if (a instanceof Integer valA && Math.abs(valA - idx) < threshold)
            return true;
        if (b instanceof Integer valB && Math.abs(valB - idx) < threshold)
            return true;

        return false;
    }

    private int getMaxValue(int[] arr) {
        int max = 1;
        for (int val : arr) {
            if (val > max)
                max = val;
        }
        return max;
    }
}