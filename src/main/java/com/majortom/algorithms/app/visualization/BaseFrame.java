package com.majortom.algorithms.app.visualization;

import com.majortom.algorithms.app.visualization.listener.SortListener;

import javax.swing.*;
import java.awt.*;

public abstract class BaseFrame<T> extends JFrame implements SortListener {
    protected JPanel controlPanel, sidePanel;
    protected JButton startBtn, resetBtn;
    protected JSlider speedSlider;

    // 状态标签
    protected JLabel timeLabel, compareLabel, swapLabel;
    protected long startTime = 0;

    public BaseFrame(String title) {
        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- 1. 底部控制栏 ---
        controlPanel = new JPanel();
        controlPanel.setBackground(new Color(45, 49, 58));

        startBtn = new JButton("开始");
        resetBtn = new JButton("重置");
        speedSlider = new JSlider(0, 500, 50);

        controlPanel.add(startBtn);
        controlPanel.add(resetBtn);
        controlPanel.add(new JLabel("<html><font color='white'> 速度(ms): </font></html>"));
        controlPanel.add(speedSlider);
        add(controlPanel, BorderLayout.SOUTH);

        // --- 2. 右侧数据看板 ---
        sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(new Color(33, 37, 43));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        sidePanel.setPreferredSize(new Dimension(180, 0));

        // 初始化显示标签
        timeLabel = createInfoLabel("耗时: 0.000s", new Color(152, 195, 121)); // 绿
        compareLabel = createInfoLabel("比较: 0", new Color(97, 175, 239)); // 蓝
        swapLabel = createInfoLabel("交换: 0", new Color(224, 108, 117)); // 红

        sidePanel.add(createSectionTitle("算法统计"));
        sidePanel.add(Box.createVerticalStrut(15)); // 间距
        sidePanel.add(timeLabel);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(compareLabel);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(swapLabel);

        add(sidePanel, BorderLayout.EAST);
    }

    // 辅助方法：创建统一风格的标签
    private JLabel createInfoLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Monospaced", Font.BOLD, 14));
        label.setForeground(color);
        return label;
    }

    private JLabel createSectionTitle(String title) {
        JLabel label = new JLabel(title);
        label.setForeground(Color.GRAY);
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        return label;
    }

    protected void initAndLaunch() {
        this.getContentPane().revalidate();
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.repaint();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onStep(int[] data, int a, int b, int compareCount, int swapCount) {
        long now = System.currentTimeMillis();
        double duration = (startTime == 0) ? 0 : (now - this.startTime) / 1000.0;

        SwingUtilities.invokeLater(() -> {
            timeLabel.setText(String.format("耗时: %.3fs", duration));
            compareLabel.setText("比较: " + compareCount);
            swapLabel.setText("交换: " + swapCount);

            // 强转为泛型 T，适配不同算法的数据格式
            // 这里之所以可以强转，是因为子类会确保 T 的类型与算法一致
            refresh((T) data, a, b);
        });

        try {
            int delay = speedSlider.getValue();
            if (delay > 0)
                Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected abstract void refresh(T data, int a, int b);
}