package com.majortom.algorithms.core.visualization.impl.panel;

import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.core.visualization.BasePanel;
import java.awt.*;
import java.awt.image.BufferedImage;

public class MazePanel extends BasePanel<int[][]> {
    // --- 颜色常量 ---
    private static final Color BG_COLOR = new Color(12, 12, 16);
    private static final Color WALL_COLOR = new Color(28, 30, 38);
    private static final Color WALL_BORDER = new Color(56, 62, 74);
    private static final Color PATH_COLOR = new Color(220, 225, 235);
    private static final Color GLOW_BLUE = new Color(0, 197, 255);
    private static final Color GLOW_RED = new Color(255, 46, 99);
    private static final Color GLOW_GREEN = new Color(57, 255, 20);
    private static final Color COLOR_DEADEND = new Color(120, 130, 145);
    private static final Color PATH_DECOR_COLOR = new Color(0, 0, 0, 15);
    private static final Color CRYSTAL_WHITE = new Color(255, 255, 255, 150);
    private static final Color GLOW_PURPLE = new Color(191, 0, 255); // 极致紫
    // 或者
    private static final Color GLOW_AMBER = new Color(255, 179, 0); // 琥珀金
    // --- 绘图数值常量 ---
    private static final int ARC_DEFAULT = 4; // 默认圆角
    private static final int ARC_GLOW = 6; // 发光层圆角
    private static final int ARC_CRYSTAL = 2; // 中心晶体圆角
    private static final int GLOW_LAYERS = 3; // 发光层数
    private static final int OFFSET_SMALL = 1; // 1像素偏移
    private static final int OFFSET_DOUBLE = 2; // 2像素偏移（用于宽高缩减）
    private static final int GLOW_ALPHA_BASE = 40; // 发光基础透明度

    private BufferedImage mazeCache;
    private boolean needsFullRedraw = true;
    private final int initialCellSize;

    public MazePanel(int[][] data, int cellSize) {
        super(data);
        this.initialCellSize = cellSize;
        this.cellSize = cellSize;
        if (data != null) {
            int rows = data.length;
            int cols = data[0].length;
            Dimension prefSize = new Dimension(
                    cols * cellSize + padding * 2,
                    rows * cellSize + padding * 2);
            this.setPreferredSize(prefSize);
            this.setMinimumSize(prefSize);
        }
        setDoubleBuffered(true);
    }

    @Override
    public void updateData(int[][] data, Object a, Object b) {
        this.data = data;
        this.activeA = a;
        this.activeB = b;
        this.needsFullRedraw = true;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(BG_COLOR);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        if (data == null)
            return;
        calculateScale();
        updateMazeCache();

        if (mazeCache != null) {
            g2d.drawImage(mazeCache, padding, padding, null);
        }

        if (activeA instanceof Integer r && activeB instanceof Integer c) {
            drawGlowCell(g2d, c * cellSize + padding, r * cellSize + padding, GLOW_RED);
        }
    }

    private void updateMazeCache() {
        int rows = data.length, cols = data[0].length;
        int width = cols * cellSize, height = rows * cellSize;

        if (mazeCache == null || mazeCache.getWidth() != width || mazeCache.getHeight() != height) {
            mazeCache = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
            needsFullRedraw = true;
        }

        if (needsFullRedraw) {
            Graphics2D g = mazeCache.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    int cellType = data[i][j];
                    int x = j * cellSize, y = i * cellSize;

                    switch (cellType) {
                        case MazeConstant.WALL -> drawWall(g, x, y);
                        case MazeConstant.PATH -> drawGlowCell(g, x, y, GLOW_BLUE);
                        case MazeConstant.START -> drawGlowCell(g, x, y, GLOW_GREEN);
                        case MazeConstant.END -> drawGlowCell(g, x, y, GLOW_RED);
                        case MazeConstant.DEADEND -> {
                            g.setColor(COLOR_DEADEND);
                            g.fillRoundRect(x + OFFSET_SMALL, y + OFFSET_SMALL,
                                    cellSize - OFFSET_DOUBLE, cellSize - OFFSET_DOUBLE,
                                    ARC_DEFAULT, ARC_DEFAULT);
                        }
                        case MazeConstant.BACKTRACK -> drawGlowCell(g, x, y, GLOW_AMBER);
                        default -> {
                            g.setColor(PATH_COLOR);
                            g.fillRect(x, y, cellSize, cellSize);
                            g.setColor(PATH_DECOR_COLOR);
                            g.drawRect(x, y, cellSize, cellSize);
                        }
                    }
                }
            }
            g.dispose();
            needsFullRedraw = false;
        }
    }

    private void drawGlowCell(Graphics2D g, int x, int y, Color color) {
        for (int i = GLOW_LAYERS; i > 0; i--) {
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), GLOW_ALPHA_BASE / i));
            g.fillRoundRect(x - i, y - i, cellSize + i * 2, cellSize + i * 2, ARC_GLOW, ARC_GLOW);
        }
        g.setColor(color);
        g.fillRoundRect(x + OFFSET_SMALL, y + OFFSET_SMALL,
                cellSize - OFFSET_DOUBLE, cellSize - OFFSET_DOUBLE,
                ARC_DEFAULT, ARC_DEFAULT);

        g.setColor(CRYSTAL_WHITE);
        g.fillRoundRect(x + cellSize / 4, y + cellSize / 4, cellSize / 2, cellSize / 2, ARC_CRYSTAL, ARC_CRYSTAL);
    }

    private void drawWall(Graphics2D g, int x, int y) {
        g.setColor(WALL_COLOR);
        g.fillRect(x, y, cellSize, cellSize);
        g.setColor(WALL_BORDER);
        g.drawRect(x, y, cellSize - OFFSET_SMALL, cellSize - OFFSET_SMALL);
    }

    @Override
    protected void render(Graphics2D g) {
    }

    @Override
    protected void calculateScale() {
        int availableW = getWidth() - 2 * padding;
        int availableH = getHeight() - 2 * padding;

        if (availableW > 0 && availableH > 0) {
            int scaleW = availableW / data[0].length;
            int scaleH = availableH / data.length;
            int newCellSize = Math.max(1, Math.min(scaleW, scaleH));

            if (this.cellSize != newCellSize) {
                this.cellSize = newCellSize;
                this.needsFullRedraw = true;
            }
        } else {
            if (this.cellSize <= 0) {
                this.cellSize = initialCellSize;
            }
        }
    }
}