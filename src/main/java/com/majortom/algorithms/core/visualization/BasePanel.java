package com.majortom.algorithms.core.visualization;

import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * 通用可视化画布基类
 */
public abstract class BasePanel<T> extends JPanel {
    protected T data;
    protected Object activeA;
    protected Object activeB;
    protected int cellSize;
    protected final int padding = 20;

    public BasePanel(T data) {
        this.data = data;
        // 改造：不再硬编码颜色，自动获取 FlatLaf 的面板背景色
        setBackground(UIManager.getColor("Panel.background"));
    }

    public void updateData(T data, Object a, Object b) {
        this.data = data;
        this.activeA = a;
        this.activeB = b;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data == null)
            return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        calculateScale();
        render(g2d);
    }

    protected abstract void render(Graphics2D g);

    protected abstract void calculateScale();
}