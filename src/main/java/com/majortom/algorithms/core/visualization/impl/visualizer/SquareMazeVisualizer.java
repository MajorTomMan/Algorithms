package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.core.visualization.base.BaseMazeVisualizer;

import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.Color;

/**
 * 方形网格迷宫可视化器
 * 职责：执行标准二维数组的坐标映射与霓虹单元格渲染。
 */
public class SquareMazeVisualizer extends BaseMazeVisualizer<int[][]> {

    @Override
    protected void drawMaze(int[][] grid, Object a, Object b) {
        int rows = grid.length;
        int cols = grid[0].length;

        // 获取画布实际尺寸，如果布局还没完成，尝试使用 Pref 尺寸或保底
        double width = canvas.getWidth() > 0 ? canvas.getWidth() : getPrefWidth();
        double height = canvas.getHeight() > 0 ? canvas.getHeight() : getPrefHeight();

        // 如果还是 0（说明是刚启动），暂时不画，等待下一次 resize 触发重绘
        if (width <= 0 || height <= 0)
            return;

        double cellW = width / cols;
        double cellH = height / rows;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                renderSquareCell(r, c, cellW, cellH, grid[r][c]);
            }
        }
    }

    /**
     * 渲染方形霓虹单元格
     */
    private void renderSquareCell(int r, int c, double w, double h, int type) {
        double x = c * w;
        double y = r * h;

        Color targetColor;
        boolean hasGlow = false;

        // 根据常量映射配色方案
        switch (type) {
            case MazeConstant.WALL -> targetColor = NEON_RED;
            case MazeConstant.PATH -> {
                targetColor = NEON_BLUE;
                hasGlow = true; // 路径具有发光特效
            }
            case MazeConstant.START -> targetColor = START_VIOLET;
            case MazeConstant.END -> targetColor = NEON_PINK;
            case MazeConstant.BACKTRACK, MazeConstant.DEADEND -> targetColor = NEON_GOLD;
            case MazeConstant.ROAD -> {
                return;
            } // 空地保留背景色
            default -> targetColor = CRYSTAL_WHITE;
        }

        // 层级 1：底层扩散光晕渲染
        if (hasGlow) {
            RadialGradient glow = new RadialGradient(0, 0, x + w / 2, y + h / 2, w * 1.5, false, CycleMethod.NO_CYCLE,
                    new Stop(0, targetColor.deriveColor(0, 1, 1, 0.4)),
                    new Stop(1, Color.TRANSPARENT));
            gc.setFill(glow);
            gc.fillRect(x - w, y - h, w * 3, h * 3);
        }

        // 层级 2：核心色块绘制
        gc.setFill(targetColor);
        // 留出 0.5 像素间隙以模拟电子元器件的物理间隔感
        gc.fillRoundRect(x + 0.5, y + 0.5, w - 1, h - 1, 2, 2);

        // 层级 3：顶层亮芯勾勒
        if (type != MazeConstant.WALL) {
            gc.setStroke(CRYSTAL_WHITE);
            gc.setLineWidth(0.5);
            // 亮芯位于方块中心区域
            gc.strokeRoundRect(x + 2, y + 2, Math.max(0, w - 4), Math.max(0, h - 4), 1, 1);
        }
    }
}