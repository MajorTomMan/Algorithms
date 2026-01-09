package com.majortom.algorithms.core.visualization.impl.panel;

import com.majortom.algorithms.core.visualization.BasePanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * 排序算法可视化画布（Sort Visualization Panel）
 * * 职责：
 * 1. 将整型数组（int[]）映射为垂直柱状图。
 * 2. 实时呈现算法的扫描、比较或交换状态（通过颜色标记索引）。
 * 3. 自动适配窗口尺寸变化，动态调整柱体宽度。
 */
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

        // --- 核心优化：降采样逻辑 ---
        // 如果数据量远大于像素宽度，计算步长
        // 例如 10万数据在 1000 像素宽的屏幕，每 100 个数据取 1 个画出来
        double step = Math.max(1.0, (double) data.length / viewW);

        // 动态计算单根柱子的宽度，至少 1 像素
        int barWidth = Math.max(1, (int) Math.ceil((double) viewW / (data.length / step)));

        for (double i = 0; i < data.length; i += step) {
            int idx = (int) i;

            // 映射坐标
            int x = padding + (int) ((i / data.length) * viewW);
            int h = (int) ((double) data[idx] / maxVal * viewH);
            int y = getHeight() - padding - h;

            // 颜色高亮逻辑：只高亮当前采样点附近的活跃元素
            if (isActive(idx)) {
                g.setColor(new Color(224, 108, 117)); // 原子红
            } else {
                g.setColor(new Color(97, 175, 239)); // 科技蓝
            }

            g.fillRect(x, y, barWidth, h);
        }
    }

    private boolean isActive(int idx) {
        // 由于是采样绘制，如果活跃索引在采样点附近，我们也将其高亮，否则看不见红点
        if (activeA == null && activeB == null)
            return false;
        int threshold = Math.max(1, data.length / getWidth());
        if (activeA instanceof Integer a && Math.abs(a - idx) < threshold)
            return true;
        if (activeB instanceof Integer b && Math.abs(b - idx) < threshold)
            return true;
        return false;
    }

    @Override
    protected void calculateScale() {
        // 在大数据量模式下，cellSize 的含义变为“逻辑比例”，不再直接用于循环计数
        if (data != null && data.length > 0) {
            this.cellSize = Math.max(1, (getWidth() - 2 * padding) / data.length);
        }
    }

    private int getMaxValue(int[] arr) {
        int max = 1;
        // 10万级数据量的 max 查找很快，可以保持
        for (int val : arr)
            if (val > max)
                max = val;
        return max;
    }
}