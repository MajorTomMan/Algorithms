package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.visualization.base.BaseMazeVisualizer;
import javafx.scene.paint.Color;
import javafx.scene.effect.Glow;

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
        double cx = x + w / 2;
        double cy = y + h / 2;
        double monSize = Math.min(w, h) * 0.6;

        switch (type) {
            case MazeConstant.WALL -> {
                gc.setFill(RAN_BLACK);
                gc.fillRect(x, y, w, h);
            }
            case MazeConstant.START -> {
                gc.setFill(RAN_WHITE);
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize, RAN_WHITE, RAN_BLACK);
            }
            case MazeConstant.END -> {
                gc.setFill(RAN_VIOLET.saturate());
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize, RAN_VIOLET, RAN_WHITE);
            }
            case MazeConstant.ROAD -> {
                // 二郎蓝：足迹保持，一文字家纹
                gc.setFill(RAN_BLUE.deriveColor(0, 1.2, 1.0, 0.45));
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize, RAN_BLUE, RAN_BLUE.saturate());
            }
            case MazeConstant.PATH -> {
                // 大郎红：正统路径，圆形家纹
                gc.setFill(RAN_RED.saturate());
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize, RAN_RED, Color.rgb(40, 0, 0, 0.95));
            }
            case MazeConstant.BACKTRACK, MazeConstant.DEADEND -> {
                // 三郎黄：回溯火场，三角家纹
                gc.setFill(RAN_YELLOW.saturate());
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize, RAN_YELLOW, Color.rgb(80, 50, 0, 0.9));
            }
        }
    }

    @Override
    protected void drawFocus(Object a, Object b, double w, double h) {
        if (a instanceof Integer r && b instanceof Integer c) {
            double x = getX(c, w);
            double y = getY(r, h);
            gc.save();
            gc.setEffect(new Glow(0.95));
            gc.setStroke(RAN_WHITE);
            gc.setLineWidth(4.5);
            gc.strokeRect(x + 0.5, y + 0.5, w - 1, h - 1);
            gc.setStroke(RAN_BLUE.saturate());
            gc.setLineWidth(2.5);
            gc.strokeRect(x + 1.5, y + 1.5, w - 3, h - 3);
            gc.restore();
        }
    }
}