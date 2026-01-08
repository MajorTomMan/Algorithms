package com.majortom.algorithms.app.visualization.impl.panel;

import com.majortom.algorithms.app.visualization.impl.frame.MazeFrame;
import com.majortom.algorithms.core.maze.BaseMaze;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class MazePanel extends JPanel {
    private final BaseMaze maze;
    private final int cellSize;

    public MazePanel(BaseMaze maze, int cellSize) {
        this.maze = maze;
        this.cellSize = cellSize;
        setBackground(new Color(33, 37, 43)); // 配合 BaseFrame 的深色系
        setPreferredSize(new Dimension(maze.getCols() * cellSize, maze.getRows() * cellSize));

        // 算法每一步都会调用 repaint
        this.maze.setStepListener(this::repaint);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        int[][] map = maze.getMap();

        for (int r = 0; r < maze.getRows(); r++) {
            for (int c = 0; c < maze.getCols(); c++) {
                drawCell(g2d, r, c, map[r][c]);
            }
        }
    }

    private void drawCell(Graphics2D g, int r, int c, int type) {
        switch (type) {
            case BaseMaze.WALL -> g.setColor(new Color(44, 62, 80)); // 墙色
            case BaseMaze.PATH -> g.setColor(Color.WHITE); // 路色
            case BaseMaze.START -> g.setColor(new Color(46, 204, 113)); // 绿
            case BaseMaze.END -> g.setColor(new Color(231, 76, 60)); // 红
        }
        // 画格子，留 1 像素间隙更有网格感
        g.fillRect(c * cellSize, r * cellSize, cellSize - 1, cellSize - 1);
    }

    public static void launch(BaseMaze maze, int cellSize) {
        SwingUtilities.invokeLater(() -> {
            // 只需要创建对象，构造函数里已经处理好了布局
            new MazeFrame(maze, cellSize);
        });
    }
}