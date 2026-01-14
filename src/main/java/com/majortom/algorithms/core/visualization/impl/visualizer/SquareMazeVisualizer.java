package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.core.visualization.base.BaseMazeVisualizer;
import javafx.scene.paint.Color;

/**
 * 迷宫可视化器 - 参考黑泽明《乱》配色
 * 红色：墙体 (太郎)
 * 蓝色：当前路径 (次郎)
 * 黄色：回溯路径 (三郎)
 */
public class SquareMazeVisualizer extends BaseMazeVisualizer<BaseMaze<int[][]>> {

    @Override
    protected void drawMaze(BaseMaze<int[][]> mazeEntity, Object a, Object b) {
        int[][] grid = mazeEntity.getData();
        if (grid == null)
            return;

        double cellW = canvas.getWidth() / mazeEntity.getCols();
        double cellH = canvas.getHeight() / mazeEntity.getRows();

        for (int r = 0; r < mazeEntity.getRows(); r++) {
            for (int c = 0; c < mazeEntity.getCols(); c++) {
                int type = grid[r][c];
                // 仅渲染非通路部分
                if (type != MazeConstant.ROAD) {
                    renderRanCell(r, c, cellW, cellH, type);
                }
            }
        }
        drawFocus(a, b, cellW, cellH);
    }

    private void renderRanCell(int r, int c, double w, double h, int type) {
        double x = c * w;
        double y = r * h;

        if (type == MazeConstant.WALL) {
            gc.setFill(RAN_RED);
            gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
            return;
        }

        Color targetColor = switch (type) {
            case MazeConstant.PATH -> RAN_BLUE;
            case MazeConstant.BACKTRACK -> RAN_GOLD;
            case MazeConstant.START, MazeConstant.END -> BONE_WHITE;
            default -> Color.TRANSPARENT;
        };

        gc.setFill(targetColor);
        gc.fillRect(x + 1, y + 1, w - 2, h - 2);
    }

    @Override
    protected void drawFocus(Object a, Object b, double w, double h) {
        if (a instanceof Integer r && b instanceof Integer c) {
            double x = c * w;
            double y = r * h;
            // 蓝色硬边框作为当前扫描焦点
            gc.setStroke(RAN_BLUE);
            gc.setLineWidth(2.5);
            gc.strokeRect(x + 1, y + 1, w - 2, h - 2);
        }
    }
}