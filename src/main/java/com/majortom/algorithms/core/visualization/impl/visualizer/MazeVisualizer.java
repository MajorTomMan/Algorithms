package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * 迷宫可视化 - 霓虹艺术版 (JavaFX 实现)
 */
public class MazeVisualizer extends BaseVisualizer<int[][]> {

    // --- 色彩重构 ---
    private static final Color BG_DEEP = Color.web("#0A0A0E");
    private static final Color NEON_RED = Color.web("#FF1E32");
    private static final Color NEON_BLUE = Color.web("#00A0FF");
    private static final Color NEON_GOLD = Color.web("#FFC800");
    private static final Color NEON_PINK = Color.web("#FF0096");
    private static final Color START_VIOLET = Color.web("#A050FF");
    private static final Color CRYSTAL_WHITE = Color.rgb(255, 255, 255, 0.8);

    private double currentCellSize;

    public MazeVisualizer(int[][] data) {
        super(data);
    }

    @Override
    protected void onMeasure(double width, double height) {
        int[][] maze = data.get();
        if (maze == null)
            return;

        int rows = maze.length;
        int cols = maze[0].length;

        // 计算自适应尺寸
        double size = Math.min(width / cols, height / rows);
        this.currentCellSize = Math.max(4, size);
    }

    @Override
    protected void draw(GraphicsContext gc, double width, double height) {
        int[][] maze = data.get();
        if (maze == null)
            return;

        int rows = maze.length;
        int cols = maze[0].length;

        double offsetX = (width - cols * currentCellSize) / 2;
        double offsetY = (height - rows * currentCellSize) / 2;

        // 1. 绘制背景
        gc.setFill(BG_DEEP);
        gc.fillRect(0, 0, width, height);

        // 2. 绘制墙体
        drawWalls(gc, maze, offsetX, offsetY, rows, cols);

        // 3. 绘制路径与特殊节点
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int type = maze[i][j];
                if (type != MazeConstant.WALL) {
                    renderNeonNode(gc, offsetX + j * currentCellSize, offsetY + i * currentCellSize, type);
                }
            }
        }

        // 4. 实时焦点 (activeA=row, activeB=col)
        if (activeA.get() instanceof Integer r && activeB.get() instanceof Integer c) {
            drawArtFocus(gc, offsetX + c * currentCellSize, offsetY + r * currentCellSize);
        }
    }

    private void drawWalls(GraphicsContext gc, int[][] maze, double ox, double oy, int rows, int cols) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (maze[i][j] == MazeConstant.WALL) {
                    double x = ox + j * currentCellSize;
                    double y = oy + i * currentCellSize;

                    // 深红底
                    gc.setFill(Color.web("#640A14"));
                    gc.fillRoundRect(x + 1, y + 1, currentCellSize - 2, currentCellSize - 2, 4, 4);

                    // 鲜红边缘
                    gc.setStroke(NEON_RED);
                    gc.setLineWidth(0.8);
                    gc.strokeRoundRect(x + 1, y + 1, currentCellSize - 2, currentCellSize - 2, 4, 4);
                }
            }
        }
    }

    private void renderNeonNode(GraphicsContext gc, double x, double y, int type) {
        Color base = switch (type) {
            case MazeConstant.PATH -> NEON_BLUE;
            case MazeConstant.START -> START_VIOLET;
            case MazeConstant.BACKTRACK -> NEON_GOLD;
            case MazeConstant.END -> NEON_PINK;
            default -> Color.WHITE;
        };

        // A. 径向发光特效
        double centerX = x + currentCellSize / 2.0;
        double centerY = y + currentCellSize / 2.0;
        double radius = currentCellSize * 1.5;

        RadialGradient glow = new RadialGradient(
                0, 0, centerX, centerY, radius, false, CycleMethod.NO_CYCLE,
                new Stop(0, base.deriveColor(0, 1, 1, 0.4)),
                new Stop(1, Color.TRANSPARENT));

        gc.setFill(glow);
        gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        // B. 核心块
        gc.setFill(base.deriveColor(0, 1, 1, 0.8));
        gc.fillRoundRect(x + 2, y + 2, currentCellSize - 4, currentCellSize - 4, 6, 6);

        // C. 中心晶体
        gc.setFill(CRYSTAL_WHITE);
        double cs = currentCellSize / 4.0;
        gc.fillRoundRect(x + (currentCellSize - cs) / 2, y + (currentCellSize - cs) / 2, cs, cs, 2, 2);
    }

    private void drawArtFocus(GraphicsContext gc, double x, double y) {
        // 呼吸效果计算
        double pulse = Math.sin(System.currentTimeMillis() / 200.0) * 0.15 + 1.05;
        double s = currentCellSize * pulse;
        double offset = (s - currentCellSize) / 2.0;

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2.0);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        gc.strokeRoundRect(x - offset, y - offset, s, s, 8, 8);
    }
}