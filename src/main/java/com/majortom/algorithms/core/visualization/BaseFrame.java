package com.majortom.algorithms.core.visualization;

import com.formdev.flatlaf.FlatDarkLaf;
import com.majortom.algorithms.core.base.listener.SyncListener;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * 通用可视化窗体基座 (MigLayout + FlatLaf 版)
 * 职责：
 * 1. 界面布局：利用 MigLayout 实现自适应侧边栏和底部控制栏。
 * 2. 视觉美化：集成 FlatLaf 暗黑皮肤，移除冗余的颜色硬编码。
 * 3. 线程安全：严密的算法线程中断与同步机制。
 */
public abstract class BaseFrame<T> extends JFrame implements SyncListener<T> {

    // --- UI 组件 ---
    protected JPanel controlPanel, sidePanel;
    protected JButton startBtn, resetBtn;
    protected JSlider speedSlider;
    protected JTextArea dataListArea;

    // --- 统计标签 ---
    protected JLabel timeLabel, compareLabel, actionLabel;

    // --- 运行状态变量 ---
    protected long startTime = 0;
    protected Runnable pendingTask;
    private long lastRefreshTime = 0;

    /** 核心线程句柄：用于控制算法的启动与强制停止 */
    private volatile Thread algorithmThread = null;

    public BaseFrame(String title) {
        // 1. 初始化皮肤：在组件创建前调用
        FlatDarkLaf.setup();

        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 2. 整体布局设置
        // "fill" 让内容填满, "ins 0" 边距为0
        // [grow][] 表示第一列自动拉伸（放画布），第二列按需分配（放侧边栏）
        // [grow][] 表示第一行自动拉伸（放主体），第二行按需分配（放底部栏）
        setLayout(new MigLayout("fill, ins 0", "[grow][]", "[grow][]"));

        // --- 初始化各个区域 ---
        initControlPanel();
        initSidePanel();

        // 绑定事件
        setupActions();
    }

    /**
     * 初始化底部控制栏 (South Area)
     */
    private void initControlPanel() {
        // "ins 10 20 10 20" 分别是上左下右的边距
        controlPanel = new JPanel(new MigLayout("insets 10 20 10 20, align center", "[]40[]20[]", "[]"));

        startBtn = new JButton("开始执行");
        // FlatLaf 特性：圆角按钮
        startBtn.putClientProperty("JButton.buttonType", "roundRect");

        resetBtn = new JButton("重置系统");
        resetBtn.putClientProperty("JButton.buttonType", "roundRect");

        speedSlider = new JSlider(0, 500, 50);
        JLabel delayLabel = new JLabel("执行延迟 (ms):");

        controlPanel.add(startBtn);
        controlPanel.add(resetBtn);
        controlPanel.add(delayLabel, "gapleft 40");
        controlPanel.add(speedSlider, "width 150!"); // 固定宽度

        // 将控制栏添加到窗口底部，跨越两列
        add(controlPanel, "south, span, growx");
    }

    /**
     * 初始化侧边统计面板 (East Area)
     */
    private void initSidePanel() {
        // "flowy" 强制垂直排列，"ins 20" 内边距
        sidePanel = new JPanel(new MigLayout("flowy, ins 20, fillx", "[fill]", "[]15[]10[]10[]30[]10[grow]"));
        sidePanel.setPreferredSize(new Dimension(240, 0));

        // 1. 统计标题
        sidePanel.add(createSectionTitle("ALGORITHM STATS"));

        // 2. 标签初始化 (颜色保持你之前的逻辑，但在暗色背景下会更亮)
        timeLabel = createInfoLabel("耗时: 0.000s", new Color(152, 195, 121));
        compareLabel = createInfoLabel("比较次数: 0", new Color(97, 175, 239));
        actionLabel = createInfoLabel("步进操作: 0", new Color(224, 108, 117));

        sidePanel.add(timeLabel);
        sidePanel.add(compareLabel);
        sidePanel.add(actionLabel);

        // 3. 实时序列标题
        sidePanel.add(createSectionTitle("REAL-TIME LOGS"));

        // 4. 数据展示区
        dataListArea = new JTextArea();
        dataListArea.setEditable(false);
        dataListArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        dataListArea.setBackground(UIManager.getColor("TextArea.background")); // 自动跟随主题

        JScrollPane scrollPane = new JScrollPane(dataListArea);
        // FlatLaf 的滚动条已经很美观，无需额外边框
        sidePanel.add(scrollPane, "growy, pushy");

        add(sidePanel, "east, growy");
    }

    private void setupActions() {
        startBtn.addActionListener(e -> {
            if (pendingTask != null)
                executeTask();
        });

        resetBtn.addActionListener(e -> {
            stopAlgorithmThread();
            resetExecutionState();
        });
    }

    private void stopAlgorithmThread() {
        if (algorithmThread != null && algorithmThread.isAlive()) {
            algorithmThread.interrupt();
            algorithmThread = null;
        }
    }

    protected void resetExecutionState() {
        this.startTime = 0;
        SwingUtilities.invokeLater(() -> {
            timeLabel.setText("耗时: 0.000s");
            compareLabel.setText("比较次数: 0");
            actionLabel.setText("步进操作: 0");
            dataListArea.setText("已重置...");
            startBtn.setEnabled(true);
            handleDataReset();
            repaint();
        });
    }

    protected void executeTask() {
        stopAlgorithmThread();
        algorithmThread = new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> startBtn.setEnabled(false));
                this.startTime = System.currentTimeMillis();
                if (pendingTask != null)
                    pendingTask.run();
            } catch (Exception e) {
                System.out.println("任务被中断: " + e.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> startBtn.setEnabled(true));
            }
        }, "Algorithm-Thread");
        algorithmThread.start();
    }

    @Override
    public void onSync(T data, Object a, Object b, int compareCount, int actionCount) {
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeException("Algorithm Terminated");
        }

        long now = System.currentTimeMillis();
        double duration = (startTime == 0) ? 0 : (now - this.startTime) / 1000.0;
        int userDelay = speedSlider.getValue();

        // 16ms 刷新率控制（约 60FPS）
        if (userDelay > 0 || (now - lastRefreshTime) > 16) {
            lastRefreshTime = now;
            SwingUtilities.invokeLater(() -> {
                timeLabel.setText(String.format("耗时: %.3fs", duration));
                compareLabel.setText("比较次数: " + compareCount);
                actionLabel.setText("步进操作: " + actionCount);
                refresh(data, a, b);
            });
        }

        try {
            if (userDelay > 0)
                Thread.sleep(userDelay);
            else if (actionCount % 500 == 0)
                Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // --- 抽象与辅助 ---

    protected abstract void refresh(T data, Object a, Object b);

    protected abstract void handleDataReset();

    public void setTask(Runnable task) {
        this.pendingTask = task;
    }

    private JLabel createInfoLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Monospaced", Font.BOLD, 14));
        label.setForeground(color);
        return label;
    }

    private JLabel createSectionTitle(String title) {
        JLabel label = new JLabel(title);
        // 使用 FlatLaf 预定义的辅助色
        label.setForeground(UIManager.getColor("Label.disabledForeground"));
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        return label;
    }

    protected void initAndLaunch() {
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

}