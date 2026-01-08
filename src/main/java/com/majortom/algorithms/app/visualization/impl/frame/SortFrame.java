package com.majortom.algorithms.app.visualization.impl.frame;

import com.majortom.algorithms.app.visualization.BaseFrame;
import com.majortom.algorithms.app.visualization.impl.panel.SortPanel;
import com.majortom.algorithms.core.sort.BaseSort;

import java.awt.BorderLayout;

import javax.swing.SwingUtilities;

public class SortFrame extends BaseFrame<int[]> {
    private SortPanel panel;
    private int[] data;
    private int[] originalData; // 新增：保存一份原始数据的副本用于重置
    private BaseSort sortAlgorithm;

    public SortFrame(int[] data, BaseSort sortAlgorithm) {
        super("排序可视化");
        this.data = data;
        // 备份原始数据，这样点重置时才能变回乱序
        this.originalData = data.clone();

        // 1. 建立连接
        this.sortAlgorithm = sortAlgorithm.setListener(this);

        // 2. 组装组件
        panel = new SortPanel(data);
        add(panel, BorderLayout.CENTER);

        // 3. 按钮逻辑
        startBtn.addActionListener(e -> {
            new Thread(() -> {
                startBtn.setEnabled(false);
                resetBtn.setEnabled(false); // 运行期间不允许重置

                // 核心：在排序开始前，初始化统计数据和时间
                this.startTime = System.currentTimeMillis();
                sortAlgorithm.resetStatistics();

                sortAlgorithm.sort(this.data);

                startBtn.setEnabled(true);
                resetBtn.setEnabled(true);
            }).start();
        });

        // 4. 重置逻辑
        resetBtn.addActionListener(e -> {
            // 恢复数据
            System.arraycopy(originalData, 0, this.data, 0, originalData.length);
            // 清零统计显示
            this.startTime = 0;
            timeLabel.setText("耗时: 0.000s");
            compareLabel.setText("比较: 0");
            swapLabel.setText("交换: 0");
            // 刷新画面
            refresh(this.data, -1, -1);
        });

        // 5. 优雅启动
        SwingUtilities.invokeLater(() -> {
            setSize(1000, 600); // 稍微宽一点，给右侧看板留点空间
            initAndLaunch();
        });
    }

    @Override
    protected void refresh(int[] data, int a, int b) {
        // 将最新的数据和高亮索引传递给 Panel
        panel.updateData(data, a, b);
        // 触发 Panel 重绘
        panel.repaint();
    }
}