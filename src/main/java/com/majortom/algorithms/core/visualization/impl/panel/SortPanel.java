package com.majortom.algorithms.core.visualization.impl.panel;

import com.majortom.algorithms.core.visualization.BasePanel;
import javax.swing.UIManager;
import java.awt.*;

public class SortPanel extends BasePanel<int[]> {

    public SortPanel(int[] data) {
        super(data);
        this.setPreferredSize(new Dimension(800, 500));
    }

    @Override
    protected void render(Graphics2D g) {
        if (data == null || data.length == 0)
            return;

        int maxVal = getMaxValue(data);
        int viewW = getWidth() - 2 * padding;
        int viewH = getHeight() - 2 * padding;

        // 核心优化：降采样逻辑
        double step = Math.max(1.0, (double) data.length / viewW);
        int barWidth = Math.max(1, (int) Math.ceil((double) viewW / (data.length / step)));

        // 预取主题色以提升循环效率
        Color normalColor = UIManager.getColor("ProgressBar.foreground"); // 科技蓝风格
        Color activeColor = UIManager.getColor("ProgressBar.selectionBackground"); // 亮红色风格
        if (activeColor == null)
            activeColor = new Color(224, 108, 117);

        for (double i = 0; i < data.length; i += step) {
            int idx = (int) i;

            // 映射坐标
            int x = padding + (int) ((i / data.length) * viewW);
            int h = (int) ((double) data[idx] / maxVal * viewH);
            int y = getHeight() - padding - h;

            // 高亮逻辑
            if (isActive(idx)) {
                g.setColor(activeColor);
            } else {
                g.setColor(normalColor);
            }

            // 如果 barWidth 较宽，绘制圆角矩形视觉效果更好
            if (barWidth > 2) {
                g.fillRoundRect(x, y, barWidth - 1, h, 2, 2);
            } else {
                g.fillRect(x, y, barWidth, h);
            }
        }
    }

    private boolean isActive(int idx) {
        if (activeA == null && activeB == null)
            return false;

        // 采样模式下的红点补偿阈值
        int threshold = Math.max(1, data.length / getWidth());

        if (activeA instanceof Integer a && Math.abs(a - idx) < threshold)
            return true;
        if (activeB instanceof Integer b && Math.abs(b - idx) < threshold)
            return true;
        return false;
    }

    @Override
    protected void calculateScale() {
        if (data != null && data.length > 0) {
            // cellSize 在此仅作为 UI 层参考值
            this.cellSize = Math.max(1, (getWidth() - 2 * padding) / data.length);
        }
    }

    private int getMaxValue(int[] arr) {
        int max = 1;
        for (int val : arr)
            if (val > max)
                max = val;
        return max;
    }
}