package com.majortom.algorithms.core.visualization;

import javax.swing.*;
import com.majortom.algorithms.core.base.listener.SyncListener;
import java.awt.*;

/**
 * 通用可视化窗体基座（Abstract Base Frame for Visualization）
 * 职责：
 * 1. 界面布局：提供标准的控制栏（南区）和统计侧边栏（东区）。
 * 2. 线程管理：通过独立子线程运行算法，提供强力重置与中断机制。
 * 3. 状态同步：实现 SyncListener 接口，将算法数据实时映射到 UI。
 * 4. 交互控制：通过 JSlider 实现步进延迟调节。
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
        // 设置跨平台外观
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

        startBtn = new JButton("开始执行");
        resetBtn = new JButton("重置");

        // 延迟控制：0ms - 500ms
        speedSlider = new JSlider(0, 500, 50);
        speedSlider.setBackground(new Color(45, 49, 58));

        controlPanel.add(startBtn);
        controlPanel.add(resetBtn);
        JLabel delayLabel = new JLabel(" 延迟(ms): ");
        delayLabel.setForeground(Color.WHITE);
        controlPanel.add(delayLabel);
        controlPanel.add(speedSlider);
        add(controlPanel, BorderLayout.SOUTH);

        // --- 2. 初始化侧边统计面板 ---
        // 预定义颜色以匹配深色主题
        timeLabel = createInfoLabel("耗时: 0.000s", new Color(152, 195, 121)); // 绿色
        compareLabel = createInfoLabel("比较: 0", new Color(97, 175, 239)); // 蓝色
        actionLabel = createInfoLabel("操作: 0", new Color(224, 108, 117)); // 红色

        initSidePanel();
        add(sidePanel, BorderLayout.EAST);

        // --- 3. 绑定核心交互事件 ---
        setupActions();
    }

    /**
     * 初始化侧边栏布局
     */
    private void initSidePanel() {
        sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(new Color(33, 37, 43));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        sidePanel.setPreferredSize(new Dimension(200, 0));

        // 统计区块
        sidePanel.add(createSectionTitle("算法统计"));
        sidePanel.add(Box.createVerticalStrut(15));
        sidePanel.add(timeLabel);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(compareLabel);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(actionLabel);

        sidePanel.add(Box.createVerticalStrut(25));

        // 实时数据区块
        sidePanel.add(createSectionTitle("实时序列"));
        sidePanel.add(Box.createVerticalStrut(10));

        dataListArea = new JTextArea();
        dataListArea.setEditable(false);
        dataListArea.setLineWrap(true);
        dataListArea.setWrapStyleWord(true);
        dataListArea.setBackground(new Color(40, 44, 52));
        dataListArea.setForeground(new Color(171, 178, 191));
        dataListArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        dataListArea.setText("等待执行...");

        JScrollPane scrollPane = new JScrollPane(dataListArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(61, 68, 83)));
        scrollPane.setPreferredSize(new Dimension(170, 300));
        sidePanel.add(scrollPane);
    }

    /**
     * 绑定基础按钮逻辑
     */
    private void setupActions() {
        startBtn.addActionListener(e -> {
            if (pendingTask != null) {
                this.executeTask();
            }
        });

        resetBtn.addActionListener(e -> {
            this.stopAlgorithmThread(); // 强行杀掉运行中的线程
            this.resetExecutionState(); // 恢复 UI 和数据状态
        });
    }

    /**
     * 强行中断当前正在执行的算法线程
     */
    private void stopAlgorithmThread() {
        if (algorithmThread != null && algorithmThread.isAlive()) {
            algorithmThread.interrupt(); // 触发线程的中断标记
            algorithmThread = null; // 销毁引用
        }
    }

    /**
     * 恢复执行前的初始状态
     */
    protected void resetExecutionState() {
        this.startTime = 0;
        SwingUtilities.invokeLater(() -> {
            timeLabel.setText("耗时: 0.000s");
            compareLabel.setText("比较: 0");
            actionLabel.setText("操作: 0");
            dataListArea.setText("已重置，等待执行...");

            startBtn.setEnabled(true); // 恢复开始按钮

            // 调用具体的业务重置逻辑（如清空图的访问标记、还原排序数组等）
            handleDataReset();

            repaint();
        });
    }

    /**
     * 修改任务启动逻辑，确保每次开始都是干净的
     */
    protected void executeTask() {
        stopAlgorithmThread();

        algorithmThread = new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> startBtn.setEnabled(false));

                // 算法开始计时
                this.startTime = System.currentTimeMillis();

                if (pendingTask != null) {
                    pendingTask.run();
                }
            } catch (Exception e) {
                // 捕获由重置触发的运行时异常，不做错误处理，因为这是正常中断
                System.out.println("算法任务已停止。");
            } finally {
                SwingUtilities.invokeLater(() -> startBtn.setEnabled(true));
            }
        }, "Algorithm-Thread");

        algorithmThread.start();
    }

    /**
     * 核心同步回调
     */
    @Override
    public void onSync(T data, Object a, Object b, int compareCount, int actionCount) {
        // 1. 即时中断检查：这是保证“点击重置即停止”的关键
        if (Thread.currentThread().isInterrupted()) {
            // 抛出异常以彻底终结算法线程的递归或循环
            throw new RuntimeException("Algorithm Terminated by User");
        }

        // 2. 计算耗时
        long now = System.currentTimeMillis();
        double duration = (startTime == 0) ? 0 : (now - this.startTime) / 1000.0;

        // 3. UI 实时同步
        int userDelay = speedSlider.getValue();

        if (userDelay > 0 || (now - lastRefreshTime) > 16) {
            lastRefreshTime = now;

            SwingUtilities.invokeLater(() -> {
                timeLabel.setText(String.format("耗时: %.3fs", duration));
                compareLabel.setText("比较: " + compareCount);
                actionLabel.setText("操作: " + actionCount);

                // 刷新画布
                refresh(data, a, b);
            });
        }

        // 4. 线程节流
        try {
            if (userDelay > 0) {
                Thread.sleep(userDelay);
            } else {
                // 即便延迟为 0，每隔一段时间也要强制让出 CPU 权限给 UI 线程
                if (actionCount % 500 == 0) {
                    Thread.sleep(1);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // --- 抽象方法：由具体子类（如 GraphFrame）实现 ---

    /**
     * 子类需实现：具体的画布刷新逻辑
     */
    protected abstract void refresh(T data, Object a, Object b);

    /**
     * 子类需实现：具体的数据结构重置逻辑（如 graph.resetNodes()）
     */
    protected abstract void handleDataReset();

    // --- 辅助方法 ---

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
        label.setForeground(new Color(92, 99, 112));
        label.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        return label;
    }

    protected void initAndLaunch() {
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }
}