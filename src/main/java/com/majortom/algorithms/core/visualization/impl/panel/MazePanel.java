package com.majortom.algorithms.core.visualization.impl.panel;

import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.core.visualization.BasePanel;
import java.awt.*;
import java.awt.geom.*;

/**
 * 三原色霓虹艺术版 - 压暗背景，保留红蓝黄核心，叠加发光特效
 */
public class MazePanel extends BasePanel<int[][]> {

    // --- 色彩重构：暗底三原色 ---
    private static final Color BG_DEEP = new Color(10, 10, 14); // 极深背景，衬托发光

    // 三原色霓虹化
    private static final Color NEON_RED = new Color(255, 30, 50); // 墙体 (红)
    private static final Color NEON_BLUE = new Color(0, 160, 255); // 路径 (蓝)
    private static final Color NEON_GOLD = new Color(255, 200, 0); // 回溯/起点 (黄)
    private static final Color NEON_PINK = new Color(255, 0, 150); // 终点 (粉/红延伸)
    private static final Color START_VIOLET = new Color(160, 80, 255);
    private static final Color CRYSTAL_WHITE = new Color(255, 255, 255, 200);

    public MazePanel(int[][] data, int cellSize) {
        super(data);
        setDoubleBuffered(true);
    }

    @Override
    protected void render(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth(), h = getHeight();
        if (data == null)
            return;
        int rows = data.length, cols = data[0].length;

        // 全屏自适应
        int size = Math.min(w / cols, h / rows);
        this.cellSize = Math.max(4, size);
        int offsetX = (w - cols * cellSize) / 2;
        int offsetY = (h - rows * cellSize) / 2;

        // 1. 绘制深色背景
        g2.setColor(BG_DEEP);
        g2.fillRect(0, 0, w, h);

        // 2. 绘制墙体 (红色框架感)
        drawRedNeonWalls(g2, offsetX, offsetY, rows, cols);

        // 3. 绘制路径与回溯 (蓝/黄发光)
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int type = data[i][j];
                if (type != MazeConstant.WALL) {
                    renderNeonArtNode(g2, offsetX + j * cellSize, offsetY + i * cellSize, type);
                }
            }
        }

        // 4. 实时焦点
        if (activeA instanceof Integer r && activeB instanceof Integer c) {
            drawArtFocus(g2, offsetX + c * cellSize, offsetY + r * cellSize);
        }
    }

    private void drawRedNeonWalls(Graphics2D g2, int ox, int oy, int rows, int cols) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (data[i][j] == MazeConstant.WALL) {
                    float x = ox + j * cellSize;
                    float y = oy + i * cellSize;

                    // 红色墙体：深红色填充 + 鲜红色边缘
                    g2.setColor(new Color(100, 10, 20)); // 深红底
                    g2.fill(new RoundRectangle2D.Float(x + 1, y + 1, cellSize - 2, cellSize - 2, 2, 2));

                    g2.setColor(NEON_RED);
                    g2.setStroke(new BasicStroke(0.8f));
                    g2.draw(new RoundRectangle2D.Float(x + 1, y + 1, cellSize - 2, cellSize - 2, 2, 2));
                }
            }
        }
    }

    private void renderNeonArtNode(Graphics2D g2, int x, int y, int type) {
        Color base = switch (type) {
            case MazeConstant.PATH -> NEON_BLUE;
            case MazeConstant.START -> START_VIOLET;
            case MazeConstant.BACKTRACK -> NEON_GOLD;
            case MazeConstant.END -> NEON_PINK;
            default -> Color.WHITE;
        };

        // A. 径向发光特效 (三原色扩散)
        float radius = cellSize * 2.0f;
        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(x + cellSize / 2f, y + cellSize / 2f), radius,
                new float[] { 0.0f, 0.7f },
                new Color[] { new Color(base.getRed(), base.getGreen(), base.getBlue(), 80), new Color(0, 0, 0, 0) });
        g2.setPaint(glow);
        g2.fill(new Ellipse2D.Float(x - cellSize / 2f, y - cellSize / 2f, cellSize * 2, cellSize * 2));

        // B. 核心块
        g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 210));
        g2.fill(new RoundRectangle2D.Float(x + 2, y + 2, cellSize - 4, cellSize - 4, 4, 4));

        // C. 中心晶体点
        g2.setColor(CRYSTAL_WHITE);
        float cs = cellSize / 4f;
        g2.fill(new RoundRectangle2D.Float(x + (cellSize - cs) / 2, y + (cellSize - cs) / 2, cs, cs, 1, 1));
    }

    private void drawArtFocus(Graphics2D g2, int x, int y) {
        float b = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.15 + 1.05);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2.0f));
        float s = cellSize * b;
        g2.draw(new RoundRectangle2D.Float(x - (s - cellSize) / 2, y - (s - cellSize) / 2, s, s, 5, 5));
    }

    @Override
    protected void calculateScale() {
    }
}