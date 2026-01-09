package com.majortom.algorithms.core.visualization.impl.panel;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.visualization.BasePanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;

/**
 * 迷宫算法可视化画布（Maze Visualization Panel）
 * * 职责：
 * 1. 栅格化渲染：将二维矩阵数据映射为网格地图。
 * 2. 类型识别：通过数值状态（墙、路、起点、终点）渲染对应的颜色。
 * 3. 实时追踪：利用 activeA (行) 和 activeB (列) 标记算法当前探测的坐标。
 */
public class MazePanel extends BasePanel<int[][]> {

    /**
     * 构造函数
     * @param data 迷宫二维矩阵
     * @param cellSize 单个格子的像素边长
     */
    public MazePanel(int[][] data, int cellSize) {
        super(data);
        this.cellSize = cellSize;
        
        // 根据矩阵规模和格子大小预设画布首选尺寸
        int rows = data.length;
        int cols = data[0].length;
        setPreferredSize(new Dimension(cols * cellSize + padding * 2, rows * cellSize + padding * 2));
    }

    /**
     * 执行具体的渲染逻辑
     */
    @Override
    protected void render(Graphics2D g2d) {
        int rows = data.length;
        int cols = data[0].length;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // 1. 基础格子绘制：根据单元格类型填充颜色
                drawCell(g2d, r, c, data[r][c]);

                // 2. 焦点增强渲染：映射算法当前的探测“探针”
                // 在迷宫上下文中，activeA 通常存储 Row 指针，activeB 存储 Col 指针
                if (activeA != null && activeB != null) {
                    int currR = (int) activeA;
                    int currC = (int) activeB;
                    if (r == currR && c == currC) {
                        drawCursor(g2d, r, c);
                    }
                }
            }
        }
    }

    /**
     * 绘制单元格
     * @param type 对应 BaseMaze 中定义的常量（WALL, PATH, START, END）
     */
    private void drawCell(Graphics g, int r, int c, int type) {
        switch (type) {
            case BaseMaze.WALL -> g.setColor(new Color(44, 62, 80));    // 墙体：深蓝灰（扁平化色系）
            case BaseMaze.PATH -> g.setColor(new Color(236, 240, 241)); // 路径：浅灰色
            case BaseMaze.START -> g.setColor(new Color(46, 204, 113));  // 起点：翡翠绿
            case BaseMaze.END -> g.setColor(new Color(231, 76, 60));    // 终点：茜红
            default -> g.setColor(new Color(33, 37, 43));               // 背景
        }
        // cellSize - 1 用于留出网格缝隙，增强立体感和可读性
        g.fillRect(padding + c * cellSize, padding + r * cellSize, cellSize - 1, cellSize - 1);
    }

    /**
     * 绘制探针光标
     * 用于表现“正在拆墙”或“正在寻路”的动态效果
     */
    private void drawCursor(Graphics g, int r, int c) {
        // 使用半透明黄色（RGBA），避免完全遮挡底色状态
        g.setColor(new Color(241, 196, 15, 150)); 
        g.drawRect(padding + c * cellSize, padding + r * cellSize, cellSize - 1, cellSize - 1);
        g.fillRect(padding + c * cellSize, padding + r * cellSize, cellSize - 1, cellSize - 1);
    }

    /**
     * 比例尺适配逻辑
     */
    @Override
    protected void calculateScale() {
        // 当前版本采用固定 cellSize。
        // 如需支持窗口缩放自适应，可解除下方逻辑注释：
        /*
        if (data != null && data[0].length > 0) {
            this.cellSize = (getWidth() - 2 * padding) / data[0].length;
        }
        */
    }
}