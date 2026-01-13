package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.core.visualization.base.BaseMazeVisualizer;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * 方形网格迷宫可视化器
 * 职责：执行标准二维数组的坐标映射与霓虹单元格渲染。
 */
public class SquareMazeVisualizer extends BaseMazeVisualizer<BaseMaze<int[][]>> {

    @Override
    protected void drawMaze(BaseMaze<int[][]> mazeEntity, Object a, Object b) {
        int[][] grid = mazeEntity.getData();
        if (grid == null)
            return;

        int rows = mazeEntity.getRows();
        int cols = mazeEntity.getCols();

        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 0 || height <= 0)
            return;

        double cellW = width / cols;
        double cellH = height / rows;

        // 遍历网格进行绘制
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int type = grid[r][c];
                // 优化：ROAD 类型作为背景不重复绘制，提升性能
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
        boolean hasGlow = false;

        // 建立逻辑类型与霓虹色的映射
        switch (type) {
            case MazeConstant.WALL -> targetColor = NEON_RED;
            case MazeConstant.PATH -> {
                targetColor = NEON_BLUE;
                hasGlow = true;
            }
            case MazeConstant.BACKTRACK -> {
                targetColor = NEON_GOLD;
                hasGlow = true;
            }
            case MazeConstant.START -> targetColor = START_VIOLET;
            case MazeConstant.END -> targetColor = NEON_PINK;
            default -> targetColor = CRYSTAL_WHITE;
        }

        // --- 层级 1：扩散光晕 (营造氛围感) ---
        if (hasGlow && w > 8) {
            RadialGradient glow = new RadialGradient(0, 0, x + w / 2, y + h / 2, w * 1.5,
                    false, CycleMethod.NO_CYCLE,
                    new Stop(0, targetColor.deriveColor(0, 1, 1, 0.3)),
                    new Stop(1, Color.TRANSPARENT));
            gc.setFill(glow);
            gc.fillRect(x - w / 2, y - h / 2, w * 2, h * 2);
        }

        // --- 层级 2：核心霓虹色块 ---
        gc.setFill(targetColor);
        double arc = Math.min(w, h) * 0.3; // 圆角增加柔和感
        // 留出 0.5 像素的 Margin 营造电子元件间隔感
        gc.fillRoundRect(x + 0.5, y + 0.5, w - 1, h - 1, arc, arc);

        // --- 层级 3：亮芯绘制 (模拟冷阴极管效果) ---
        if (type != MazeConstant.WALL && w > 6) {
            gc.setStroke(CRYSTAL_WHITE);
            gc.setLineWidth(Math.max(0.5, w * 0.1));
            double padding = w * 0.2;
            gc.strokeRoundRect(x + padding, y + padding, w - padding * 2, h - padding * 2, arc * 0.5, arc * 0.5);
        }
    }
}