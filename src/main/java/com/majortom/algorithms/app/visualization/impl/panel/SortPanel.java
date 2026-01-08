package com.majortom.algorithms.app.visualization.impl.panel;

import com.majortom.algorithms.app.visualization.BasePanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Dimension;


public class SortPanel extends BasePanel<int[]> {
    private int markA = -1;
    private int markB = -1;

    public SortPanel(int[] data) {
        super(data);
        // 关键：给一个首选大小，这样 pack() 才知道缩放到多大
        this.setPreferredSize(new Dimension(800, 500));
        this.setBackground(new Color(33, 37, 43));
    }

    public void updateData(int[] data, int a, int b) {
        this.data = data;
        this.markA = a;
        this.markB = b;
        // 注意：refresh 逻辑在 SortFrame 里调 repaint()，所以这里只需要更新引用
    }

    @Override
    protected void calculateScale() {
        if (data == null || data.length == 0)
            return;
        // 使用 double 确保精度，避免大数据量时右侧留白过大
        this.cellSize = (int) Math.max(1, (double) (getWidth() - 2 * padding) / data.length);
    }

    @Override
    protected void render(Graphics2D g) {
        if (data == null)
            return;

        int maxVal = getMaxValue(data);

        for (int i = 0; i < data.length; i++) {
            int x = padding + i * cellSize;
            // 修正高度映射：使用动态 maxVal
            int h = (int) ((double) data[i] / maxVal * (getHeight() - 2 * padding));
            int y = getHeight() - padding - h;

            // --- 颜色切换逻辑 ---
            if (i == markA || i == markB) {
                g.setColor(new Color(224, 108, 117)); // 正在交换的红
            } else {
                g.setColor(new Color(97, 175, 239)); // 经典的科技蓝
            }

            g.fillRect(x, y, Math.max(1, cellSize - 1), h);
        }
    }

    private int getMaxValue(int[] arr) {
        int max = 1; // 防止除以0
        for (int val : arr) {
            if (val > max)
                max = val;
        }
        return max;
    }
}