package com.majortom.algorithms.core.visualization.base;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.visualization.BaseVisualizer;

/**
 * 迷宫视觉呈现基类
 * 职责：
 * 1. 统一迷宫网格的单位计算逻辑。
 * 2. 映射迷宫状态至《乱》配色方案：墙体(红)、路径(蓝)、回溯(黄)。
 */
public abstract class BaseMazeVisualizer<S extends BaseMaze<?>> extends BaseVisualizer<S> {

    @Override
    protected void draw(S maze, Object a, Object b) {
        // 1. 获取迷宫维度
        int rows = maze.getRows();
        int cols = maze.getCols();

        // 2. 计算自适应单元格尺寸
        double cellW = canvas.getWidth() / cols;
        double cellH = canvas.getHeight() / rows;

        // 3. 执行具体的迷宫阵列绘制
        drawMazeGrid(maze, cellW, cellH);

        // 4. 执行焦点（当前扫描位置）绘制
        if (a != null && b != null) {
            drawFocus(a, b, cellW, cellH);
        }
    }

    /**
     * 子类需实现：遍历数据矩阵并调用渲染单元格
     */
    protected abstract void drawMazeGrid(S maze, double cellW, double cellH);

    /**
     * 子类需实现：绘制当前的搜索焦点
     * 
     * @param a 通常为行坐标 (Integer)
     * @param b 通常为列坐标 (Integer)
     */
    protected abstract void drawFocus(Object a, Object b, double cellW, double cellH);

    /**
     * 通用工具：绘制一个标准的网格单元
     */
    protected void renderCell(double r, double c, double w, double h, javafx.scene.paint.Color color,
            boolean isHardEdge) {
        double x = c * w;
        double y = r * h;

        gc.setFill(color);
        if (isHardEdge) {
            // 墙体使用硬边，增强结构感
            gc.fillRect(x, y, w, h);
        } else {
            // 路径或节点使用微量内缩，产生网格感
            gc.fillRect(x + 1, y + 1, w - 2, h - 2);
        }
    }
}