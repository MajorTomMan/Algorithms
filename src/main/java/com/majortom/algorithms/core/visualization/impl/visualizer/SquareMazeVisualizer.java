package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * 方形网格迷宫可视化器 (重构版)
 * 职责：执行标准二维数组的坐标映射与霓虹单元格渲染。
 */
public class SquareMazeVisualizer extends BaseVisualizer<BaseMaze<int[][]>> {

    // 🚩 定义符合你气质的霓虹色板
    private final Color NEON_WALL = Color.web("#FF3D00"); // 霓虹红 (墙)
    private final Color NEON_PATH = highlightColor; // 忧郁紫 (路径)
    private final Color NEON_START = Color.web("#00E676"); // 荧光绿 (起点)
    private final Color NEON_END = Color.web("#D500F9"); // 霓虹粉 (终点)
    private final Color NEON_BACK = Color.web("#FFD600"); // 琥珀金 (回溯)
    private final Color GLOW_WHITE = Color.web("#FFFFFF", 0.8); // 亮芯白

    @Override
    protected void draw(BaseMaze<int[][]> mazeEntity, Object a, Object b) {
        if (mazeEntity == null || mazeEntity.getData() == null) {
            clear();
            return;
        }

        int[][] grid = mazeEntity.getData();
        int rows = grid.length;
        int cols = grid[0].length;

        double width = canvas.getWidth();
        double height = canvas.getHeight();

        if (width <= 0 || height <= 0)
            return;

        double cellW = width / cols;
        double cellH = height / rows;

        // 统一预清空背景
        clear();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int type = grid[r][c];
                // 只有非道路的格子需要特殊渲染
                if (type != MazeConstant.ROAD) {
                    renderSquareCell(r, c, cellW, cellH, type);
                }
            }
        }
    }

    private void renderSquareCell(int r, int c, double w, double h, int type) {
        double x = c * w;
        double y = r * h;
        Color targetColor;
        boolean enableGlow = false;

        // 常量映射
        switch (type) {
            case MazeConstant.WALL -> targetColor = NEON_WALL;
            case MazeConstant.PATH -> {
                targetColor = NEON_PATH;
                enableGlow = true;
            }
            case MazeConstant.START -> targetColor = NEON_START;
            case MazeConstant.END -> targetColor = NEON_END;
            case MazeConstant.BACKTRACK -> targetColor = NEON_BACK;
            default -> targetColor = GLOW_WHITE;
        }

        // --- 层级 1：底层扩散光晕 (性能增强优化) ---
        // 只有当格子大于 10 像素且是路径时才渲染光晕，避免大规模下的渲染卡顿
        if (enableGlow && w > 10) {
            RadialGradient glow = new RadialGradient(0, 0, x + w / 2, y + h / 2, w * 1.2,
                    false, CycleMethod.NO_CYCLE,
                    new Stop(0, targetColor.deriveColor(0, 1, 1, 0.4)),
                    new Stop(1, Color.TRANSPARENT));
            gc.setFill(glow);
            gc.fillRect(x - w / 2, y - h / 2, w * 2, h * 2);
        }

        // --- 层级 2：核心色块绘制 ---
        gc.setFill(targetColor);
        double arc = Math.min(w, h) * 0.25;
        // 留出 0.5 像素的 Margin 营造电子元件间隔感
        gc.fillRoundRect(x + 0.5, y + 0.5, w - 1, h - 1, arc, arc);

        // --- 层级 3：顶层亮芯 (Highlight Center) ---
        // 模拟灯管内部最亮的区域
        if (type != MazeConstant.WALL && w > 6) {
            gc.setStroke(GLOW_WHITE);
            gc.setLineWidth(Math.max(0.5, w * 0.08));
            double inset = w * 0.25;
            gc.strokeRoundRect(x + inset, y + inset, w - inset * 2, h - inset * 2, arc * 0.5, arc * 0.5);
        }
    }
}