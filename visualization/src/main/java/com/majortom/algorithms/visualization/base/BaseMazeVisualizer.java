package com.majortom.algorithms.visualization.base;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.visualization.BaseVisualizer;


/**
 * 迷宫算法可视化基类
 */
public abstract class BaseMazeVisualizer<S extends BaseMaze<?>> extends BaseVisualizer<S> {

    @Override
    protected void draw(S data, Object a, Object b) {
        clear(); // 使用 BaseVisualizer 的极夜黑清空

        if (data == null)
            return;

        // 计算基础单元格尺寸
        double cellW = canvas.getWidth() / data.getCols();
        double cellH = canvas.getHeight() / data.getRows();

        drawMaze(data, a, b, cellW, cellH);
        drawFocus(a, b, cellW, cellH);
    }

    /**
     * 核心绘制逻辑，交给具体形状实现类（如 Square, Hexagon）
     */
    protected abstract void drawMaze(S mazeEntity, Object a, Object b, double cellW, double cellH);

    /**
     * 获取单元格的屏幕坐标 X
     */
    protected double getX(int col, double cellW) {
        return col * cellW;
    }

    /**
     * 获取单元格的屏幕坐标 Y
     */
    protected double getY(int row, double cellH) {
        return row * cellH;
    }

    /**
     * 焦点绘制由子类实现，以适配不同形状的焦点框
     */
    protected abstract void drawFocus(Object a, Object b, double w, double h);
}