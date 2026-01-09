package com.majortom.algorithms.core.visualization;

import javax.swing.*;
import com.majortom.algorithms.core.base.listener.SyncListener;
import java.awt.*;

/**
 * 通用可视化窗体基座（Abstract Base Frame for Visualization）
 * * 职责：
 * 1. 界面布局：提供标准的控制栏（南区）和统计侧边栏（东区）。
 * 2. 线程管理：通过独立子线程运行算法，避免阻塞 Swing 事件分发线程 (EDT)。
 * 3. 状态同步：实现 SyncListener 接口，将算法执行过程中的数据快照实时映射到 UI 元素上。
 * 4. 交互控制：通过 JSlider 控制算法步进延迟，实现“倍速”功能。
 * * @param <T> 数据快照类型（如 int[], BaseTree, BaseGraph 等）
 */
public abstract class BaseFrame<T> extends JFrame implements SyncListener<T> {
    // UI 组件：控制区与侧边栏
    protected JPanel controlPanel, sidePanel;
    protected JButton startBtn, resetBtn;
    protected JSlider speedSlider;
    protected JTextArea dataListArea; // 实时数据显示区
    
    // 算法统计状态标签
    protected JLabel timeLabel, compareLabel, actionLabel;
    
    // 运行状态变量
    protected long startTime = 0;
    protected Runnable pendingTask; // 待执行的算法任务逻辑

    public BaseFrame(String title) {
        // 设置跨平台外观（确保不同操作系统下 UI 风格一致）
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- 1. 初始化底部控制栏 ---
        controlPanel = new JPanel();
        controlPanel.setBackground(new Color(45, 49, 58));

        startBtn = new JButton("开始");
        resetBtn = new JButton("重置");
        speedSlider = new JSlider(0, 500, 50); // 滑动条控制 0-500ms 的延迟

        controlPanel.add(startBtn);
        controlPanel.add(resetBtn);
        controlPanel.add(new JLabel("<html><font color='white'> 延迟(ms): </font></html>"));
        controlPanel.add(speedSlider);
        add(controlPanel, BorderLayout.SOUTH);

        // 初始化信息标签
        timeLabel = createInfoLabel("耗时: 0.000s", new Color(152, 195, 121));
        compareLabel = createInfoLabel("比较: 0", new Color(97, 175, 239));
        actionLabel = createInfoLabel("操作: 0", new Color(224, 108, 117));

        // --- 2. 组装侧边统计面板 ---
        initSidePanel();
        add(sidePanel, BorderLayout.EAST);

        // 绑定开始按钮事件
        startBtn.addActionListener(e -> {
            if (pendingTask != null) {
                this.executeTask();
            }
        });
    }

    /**
     * 初始化侧边栏布局：包含算法统计数值和实时序列显示
     */
    private void initSidePanel() {
        sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(new Color(33, 37, 43));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        sidePanel.setPreferredSize(new Dimension(180, 0));
        
        // 统计区块
        sidePanel.add(createSectionTitle("算法统计"));
        sidePanel.add(Box.createVerticalStrut(15));
        sidePanel.add(timeLabel);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(compareLabel);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(actionLabel);
        
        sidePanel.add(Box.createVerticalStrut(20));
        
        // 数据序列显示区块
        sidePanel.add(createSectionTitle("实时序列"));
        sidePanel.add(Box.createVerticalStrut(10));

        dataListArea = new JTextArea();
        dataListArea.setEditable(false);
        dataListArea.setLineWrap(true);
        dataListArea.setWrapStyleWord(true);
        dataListArea.setBackground(new Color(33, 37, 43));
        dataListArea.setForeground(new Color(171, 178, 191));
        dataListArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        // 滚动面板支持
        JScrollPane scrollPane = new JScrollPane(dataListArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(61, 68, 83)));
        scrollPane.setPreferredSize(new Dimension(150, 250));
        sidePanel.add(scrollPane);
    }

    /**
     * 注册待执行任务（装载算法但不立即启动）
     */
    public void setTask(Runnable task) {
        this.pendingTask = task;
    }

    // UI 样式辅助方法
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

    /**
     * 刷新并显示窗体
     */
    protected void initAndLaunch() {
        this.getContentPane().revalidate();
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.repaint();
    }

    /**
     * 核心监听回调：算法执行过程中数据发生变动时触发
     * @param data 数据状态快照
     * @param a 活跃焦点 A
     * @param b 活跃焦点 B
     * @param compareCount 当前累计比较次数
     * @param actionCount 当前累计操作（交换/赋值）次数
     */
    @Override
    public void onSync(T data, Object a, Object b, int compareCount, int actionCount) {
        long now = System.currentTimeMillis();
        double duration = (startTime == 0) ? 0 : (now - this.startTime) / 1000.0;

        // 使用 SwingUtilities.invokeLater 确保 UI 更新操作在 EDT 线程执行
        SwingUtilities.invokeLater(() -> {
            timeLabel.setText(String.format("耗时: %.3fs", duration));
            compareLabel.setText("比较: " + compareCount);
            actionLabel.setText("操作: " + actionCount);

            // 调用子类实现的具体刷新画布逻辑
            refresh(data, a, b);
        });

        // 线程节流：根据 speedSlider 的值控制算法运行速度
        try {
            int delay = speedSlider.getValue();
            if (delay > 0)
                Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 抽象刷新逻辑：需在子类中调用 BasePanel 的 updateData 方法
     */
    protected abstract void refresh(T data, Object a, Object b);

    /**
     * 任务执行入口：创建独立线程运行算法逻辑
     */
    private void executeTask() {
        new Thread(() -> {
            try {
                // 执行前禁用按钮，防止算法运行中重复点击引发冲突
                SwingUtilities.invokeLater(() -> {
                    startBtn.setEnabled(false);
                    resetBtn.setEnabled(false);
                });

                this.startTime = System.currentTimeMillis();
                if (pendingTask != null) {
                    pendingTask.run(); // 正式开始算法流程
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 任务结束（正常或异常）均恢复按钮状态
                SwingUtilities.invokeLater(() -> {
                    startBtn.setEnabled(true);
                    resetBtn.setEnabled(true);
                });
            }
        }, "Algorithm-Thread").start();
    }
}