package com.majortom.algorithms.app.visualization;

import com.majortom.algorithms.core.maze.BaseMaze;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

public class MazePanel extends JPanel {
    private final BaseMaze maze;
    private final int cellSize;
    private Timer timer;

    // 构造函数设为 private，强制使用静态方法启动，或者保持 public 增加灵活性
    public MazePanel(BaseMaze maze, int cellSize) {
        this.maze = maze;
        this.cellSize = cellSize;

        int height = maze.getRows() * cellSize;
        int width = maze.getCols() * cellSize;
        setPreferredSize(new Dimension(width, height));
        setBackground(new Color(44, 62, 80));

        this.maze.setStepListener(this::repaint);
    }

    /**
     * 静态封装：一键创建窗口并启动演示
     * 
     * @param maze     迷宫算法实例
     * @param cellSize 格子大小
     * @param delayMs  步进延迟
     */
    public static void launch(BaseMaze maze, int cellSize, int delayMs) {
        // 在 Swing 事件分发线程中创建 UI 是标准做法
        SwingUtilities.invokeLater(() -> {
            MazePanel panel = new MazePanel(maze, cellSize);

            JFrame frame = new JFrame("Maze Master - " + maze.getClass().getSimpleName());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null); // 居中显示
            frame.setVisible(true);

            // 启动算法驱动逻辑
            panel.startAnimation(delayMs);
        });
    }

    public void startAnimation(int delayMs) {
        new Thread(() -> {
            maze.generate();
            if (timer != null)
                timer.stop();
        }).start();

        if (timer != null)
            timer.stop();
        timer = new Timer(delayMs, e -> maze.nextStep());
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int[][] map = maze.getMap();
        for (int r = 0; r < maze.getRows(); r++) {
            for (int c = 0; c < maze.getCols(); c++) {
                drawCell(g, r, c, map[r][c]);
            }
        }
    }

    private void drawCell(Graphics g, int r, int c, int type) {
        switch (type) {
            case BaseMaze.WALL -> g.setColor(new Color(44, 62, 80));
            case BaseMaze.PATH -> g.setColor(Color.WHITE);
            case BaseMaze.START -> g.setColor(new Color(46, 204, 113));
            case BaseMaze.END -> g.setColor(new Color(231, 76, 60));
        }
        g.fillRect(c * cellSize, r * cellSize, cellSize, cellSize);
    }
}