package com.majortom.algorithms.app.visualization.impl.frame;

import javax.swing.SwingUtilities;

import com.majortom.algorithms.app.visualization.BaseFrame;
import com.majortom.algorithms.app.visualization.impl.panel.MazePanel;
import com.majortom.algorithms.core.maze.BaseMaze;
import java.awt.BorderLayout;
import javax.swing.JOptionPane;
import javax.swing.Timer;

public class MazeFrame extends BaseFrame<int[][]> {
    private final BaseMaze maze;
    private final int cellSize;
    private final MazePanel mazePanel;
    private Timer driverTimer; // 驱动算法 nextStep 的定时器

    public MazeFrame(BaseMaze maze, int cellSize) {
        super("迷宫生成实验室 - " + maze.getClass().getSimpleName());
        this.maze = maze;
        this.cellSize = cellSize;

        // 1. 组装迷宫面板（放入 BorderLayout.CENTER）
        this.mazePanel = new MazePanel(maze, cellSize);
        add(mazePanel, BorderLayout.CENTER);

        // 2. 绑定按钮逻辑
        setupActions();

        // 3. 启动窗口
        SwingUtilities.invokeLater(() -> {
            initAndLaunch(); // 调用基类方法，会自动 pack() 并居中
        });
    }

    private void setupActions() {
        startBtn.addActionListener(e -> {
            // 1. 防抖处理：一旦开始，禁用开始按钮，直到生成结束或重置
            startBtn.setEnabled(false);
            resetBtn.setEnabled(false);

            // 2. 真正的时间起点：按下按钮才开始计时
            this.startTime = System.currentTimeMillis();
            maze.resetStatistics(); // 重置计数器

            // 3. 启动后台算法线程
            new Thread(() -> {
                maze.generate(); // 内部会触发 setCell -> waitForSignal 阻塞

                // 算法跑完后的收尾
                if (driverTimer != null)
                    driverTimer.stop();
                SwingUtilities.invokeLater(() -> {
                    resetBtn.setEnabled(true);
                    // JOptionPane.showMessageDialog(this, "迷宫生成完成！");
                });
            }).start();

            // 4. 启动驱动定时器：每隔一段时间去敲一下算法的门
            if (driverTimer != null)
                driverTimer.stop();
            driverTimer = new Timer(speedSlider.getValue(), ex -> {
                maze.nextStep(); // 唤醒算法走下一步
                updateLabels(); // 更新侧边栏耗时和计数
                mazePanel.repaint();
            });
            driverTimer.start();
        });

        // 重置按钮：把迷宫变回全墙状态
        resetBtn.addActionListener(e -> {
            if (driverTimer != null)
                driverTimer.stop();
            maze.initial(); // 回到初始 WALL 状态
            startTime = 0;
            updateLabels();
            startBtn.setEnabled(true);
            mazePanel.repaint();
        });
    }

    // 提取一个更新标签的小工具
    private void updateLabels() {
        long now = System.currentTimeMillis();
        double duration = (startTime == 0) ? 0 : (now - startTime) / 1000.0;
        timeLabel.setText(String.format("耗时: %.3fs", duration));
        compareLabel.setText("探测: " + maze.getCheckCount());
        swapLabel.setText("拆墙: " + maze.getBreakCount());
    }

    @Override
    protected void refresh(int[][] data, int r, int c) {
        mazePanel.repaint();
    }
}