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
        // 设置首选尺寸，确保在 JFrame.pack() 时能获得合理的初始显示区域
        this.setPreferredSize(new Dimension(800, 500));
    }

    /**
     * 执行具体的渲染逻辑：将数据状态映射为图形
     * 映射公式：$View = f(State)$
     */
    @Override
    protected void render(Graphics2D g) {
        // 防御性编程：确保数据有效
        if (data == null || data.length == 0)
            return;

        int maxVal = getMaxValue(data);
        // 计算有效绘图区域（减去内边距）
        int totalHeight = getHeight() - 2 * padding;

        for (int i = 0; i < data.length; i++) {
            // 1. 几何位置计算
            // x: 根据索引 i 和单元宽度 cellSize 计算水平偏移
            int x = padding + i * cellSize;
            // h: 根据数值占最大值的比例计算高度
            int h = (int) ((double) data[i] / maxVal * totalHeight);
            // y: 坐标系原点在左上角，需用总高度减去柱体高度实现“底部对齐”
            int y = getHeight() - padding - h;

            // 2. 状态颜色映射逻辑
            // 检查当前索引是否为算法同步过来的 activeA 或 activeB（视觉焦点）
            if ((activeA != null && activeA.equals(i)) || (activeB != null && activeB.equals(i))) {
                g.setColor(new Color(224, 108, 117)); // 激活状态：原子红 (Atom One Dark Style)
            } else {
                g.setColor(new Color(97, 175, 239)); // 默认状态：科技蓝
            }

            // 3. 绘制实体柱形
            // cellSize - 1 用于在柱体间留出 1 像素的视觉间隙
            g.fillRect(x, y, Math.max(1, cellSize - 1), h);
        }
    }

    /**
     * 动态比例尺计算
     * 在每次重绘前执行，确保在缩放窗口时柱子能自动填满横向空间。
     */
    @Override
    protected void calculateScale() {
        if (data == null || data.length == 0)
            return;

        // 计算每个元素可分配的平均宽度，最小保留 1 像素
        this.cellSize = (int) Math.max(1, (double) (getWidth() - 2 * padding) / data.length);
    }

    /**
     * 辅助方法：获取数组最大值
     * 用于纵向比例归一化，确保最高的柱子始终适配画布高度。
     */
    private int getMaxValue(int[] arr) {
        int max = 1;
        for (int val : arr)
            if (val > max)
                max = val;
        return max;
    }
}