package com.majortom.algorithms.core.visualization.impl.panel;

import com.majortom.algorithms.core.visualization.BasePanel;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 迷宫渲染画布 - 高性能修复版
 */
public class MazePanel extends BasePanel<int[][]> {
    private static final Color COLOR_WALL = new Color(40, 44, 52);
    private static final Color COLOR_PATH = Color.WHITE;
    private static final Color COLOR_EXPLORING = new Color(97, 175, 239);
    private static final Color COLOR_DEADEND = new Color(171, 178, 191, 100);
    private static final Color COLOR_ACTIVE = new Color(224, 108, 117);

    private BufferedImage mazeCache;
    private boolean needsFullRedraw = true;
    private final int initialCellSize; // 记录构造时传入的初始值

    public MazePanel(int[][] data, int cellSize) {
        super(data);
        this.initialCellSize = cellSize;
        this.cellSize = cellSize;
        
        // 修复 1：在显示之前，给组件一个基于初始 cellSize 的物理尺寸
        if (data != null) {
            int rows = data.length;
            int cols = data[0].length;
            Dimension prefSize = new Dimension(
                cols * cellSize + padding * 2, 
                rows * cellSize + padding * 2
            );
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
        
        // 背景擦除
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());

        if (data == null) return;

        // 修复 2：只有当组件真的显示出来（宽度 > 0）时，才触发动态缩放
        // 否则保持构造函数传入的 initialCellSize
        calculateScale();

        updateMazeCache();

        if (mazeCache != null) {
            g2d.drawImage(mazeCache, padding, padding, null);
        }

        if (activeA instanceof Integer r && activeB instanceof Integer c) {
            g2d.setColor(COLOR_ACTIVE);
            g2d.fillRect(c * cellSize + padding, r * cellSize + padding, cellSize, cellSize);
        }
    }

    private void updateMazeCache() {
        int rows = data.length;
        int cols = data[0].length;
        int width = cols * cellSize;
        int height = rows * cellSize;

        if (mazeCache == null || mazeCache.getWidth() != width || mazeCache.getHeight() != height) {
            mazeCache = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
            needsFullRedraw = true;
        }

        if (needsFullRedraw) {
            Graphics2D g = mazeCache.createGraphics();
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    int type = data[i][j];
                    g.setColor(switch (type) {
                        case 1 -> COLOR_WALL;
                        case 2 -> COLOR_EXPLORING;
                        case 4 -> COLOR_DEADEND;
                        default -> COLOR_PATH;
                    });
                    g.fillRect(j * cellSize, i * cellSize, cellSize, cellSize);
                }
            }
            g.dispose();
            needsFullRedraw = false;
        }
    }

    @Override
    protected void render(Graphics2D g) {
        // 抽象方法实现已合并
    }

    @Override
    protected void calculateScale() {
        int availableW = getWidth() - 2 * padding;
        int availableH = getHeight() - 2 * padding;

        // 只有当窗口被真正渲染且有尺寸时，才进行动态比例覆盖
        if (availableW > 0 && availableH > 0) {
            int scaleW = availableW / data[0].length;
            int scaleH = availableH / data.length;
            int newCellSize = Math.max(1, Math.min(scaleW, scaleH));
            
            if (this.cellSize != newCellSize) {
                this.cellSize = newCellSize;
                this.needsFullRedraw = true;
            }
        } else {
            // 如果 getWidth() 还是 0，确保 cellSize 维持初始设定的数值，不被归零
            if (this.cellSize <= 0) {
                this.cellSize = initialCellSize;
            }
        }
    }
}