package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.core.visualization.base.BaseMazeVisualizer;
import javafx.scene.paint.Color;

/**
 * 矩形迷宫可视化具体实现
 * 职责：将 int[][] 矩阵的数值映射为《乱》配色体系中的视觉单元。
 */
public class SquareMazeVisualizer extends BaseMazeVisualizer<BaseMaze<int[][]>> {

    @Override
    protected void drawMazeGrid(BaseMaze<int[][]> mazeEntity, double cellW, double cellH) {
        int[][] grid = mazeEntity.getData();
        if (grid == null)
            return;

        int rows = mazeEntity.getRows();
        int cols = mazeEntity.getCols();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int type = grid[r][c];

                // 1. 确定单元格颜色
                Color cellColor = getRanColorByType(type);
                if (cellColor == Color.TRANSPARENT)
                    continue;

                // 2. 调用基类工具渲染：墙体(MazeConstant.WALL)使用硬边，路径使用内缩边
                boolean isWall = (type == MazeConstant.WALL);
                renderCell(r, c, cellW, cellH, cellColor, isWall);
            }
        }
    }

    private Color getRanColorByType(int type) {
        return switch (type) {
            case MazeConstant.WALL -> RAN_RED; // 太郎红
            case MazeConstant.PATH -> RAN_BLUE; // 次郎蓝
            case MazeConstant.BACKTRACK -> RAN_GOLD; // 三郎黄
            case MazeConstant.START,
                    MazeConstant.END ->
                BONE_WHITE; // 骨白
            default -> Color.TRANSPARENT;
        };
    }

    @Override
    protected void drawFocus(Object a, Object b, double cellW, double cellH) {
        // a, b 约定为当前探测的 row 和 col
        if (a instanceof Integer r && b instanceof Integer c) {
            double x = c * cellW;
            double y = r * cellH;

            // 绘制一个带有呼吸感的次郎蓝边框，指示当前算法指针位置
            gc.setStroke(RAN_BLUE);
            gc.setLineWidth(2.0);
            gc.strokeRect(x + 1.5, y + 1.5, cellW - 3, cellH - 3);

            // 增加微弱的光晕
            drawGlow(x + cellW / 2, y + cellH / 2, Math.min(cellW, cellH) / 2, RAN_BLUE);
        }
    }
}