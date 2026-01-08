package com.majortom.algorithms.app.visualization;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * 通用可视化画布
 * 
 * @param <T> 数据模型类型（如 int[] 或 int[][]）
 */
public abstract class BasePanel<T> extends JPanel {
    protected T data; // 状态（State）
    protected int cellSize; // 动态比例尺结果
    protected final int padding = 20; // 留白，防止顶格

    public BasePanel(T data) {
        this.data = data;
        setBackground(new Color(33, 37, 43)); // 深色主题
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        // 开启抗锯齿，让映射出的画面更顺滑
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 自动计算比例尺的逻辑入口
        calculateScale();

        // 执行你定义的映射逻辑 $View = f(State)$
        render(g2d);
    }

    /**
     * 子类实现：具体的映射逻辑（比如画柱子还是画迷宫格）
     */
    protected abstract void render(Graphics2D g);

    /**
     * 子类实现：根据当前的窗口大小和数据量计算比例
     */
    protected abstract void calculateScale();
}