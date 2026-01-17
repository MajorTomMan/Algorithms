package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.visualization.base.BaseMazeVisualizer;

import javafx.scene.paint.Color;

/**
 * 矩形网格迷宫可视化器
 */
public class SquareMazeVisualizer extends BaseMazeVisualizer<BaseMaze<int[][]>> {

    @Override
    protected void drawMaze(BaseMaze<int[][]> mazeEntity, Object a, Object b, double cellW, double cellH) {
        int[][] grid = mazeEntity.getData();
        if (grid == null)
            return;

        for (int r = 0; r < mazeEntity.getRows(); r++) {
            for (int c = 0; c < mazeEntity.getCols(); c++) {
                renderRanCell(r, c, cellW, cellH, grid[r][c]);
            }
        }
    }

    private void renderRanCell(int r, int c, double w, double h, int type) {
        double x = getX(c, w);
        double y = getY(r, h);

        // 墙体逻辑：太郎红，硬边缘
        if (type == MazeConstant.WALL) {
            gc.setFill(RAN_RED);
            gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
            return;
        }

        // 路径逻辑：采用 switch 映射色彩体系
        Color targetColor = switch (type) {
            case MazeConstant.PATH -> RAN_BLUE; // 次郎蓝
            case MazeConstant.BACKTRACK -> RAN_GOLD; // 暮金/回溯
            case MazeConstant.START, MazeConstant.END -> RAN_WHITE; // 骨白
            default -> Color.TRANSPARENT;
        };

        if (targetColor != Color.TRANSPARENT) {
            gc.setFill(targetColor);
            // 内部填充略微收缩，留出网格感
            gc.fillRect(x + 1, y + 1, w - 2, h - 2);
        }
    }

    @Override
    protected void drawFocus(Object a, Object b, double w, double h) {
        if (a instanceof Integer r && b instanceof Integer c) {
            double x = getX(c, w);
            double y = getY(r, h);

            // 蓝色强对比线框，代表当前的“主将”指针
            gc.setStroke(RAN_BLUE);
            gc.setLineWidth(2.5);
            gc.strokeRect(x + 1, y + 1, w - 2, h - 2);
        }
    }
}