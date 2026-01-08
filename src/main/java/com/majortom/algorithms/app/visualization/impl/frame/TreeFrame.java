package com.majortom.algorithms.app.visualization.impl.frame;

import com.majortom.algorithms.app.visualization.BaseFrame;
import com.majortom.algorithms.app.visualization.impl.panel.TreePanel;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.impl.AVLTree;
import com.majortom.algorithms.core.tree.node.TreeNode;

import javax.swing.*;
import java.awt.*;

public class TreeFrame extends BaseFrame<TreeNode<Integer>> {
    private final TreePanel<Integer> canvas;
    private final BaseTree<Integer> algorithm; // 骨架里装着算法内核
    private JTextField inputField; // 补上这一行声明

    public TreeFrame(BaseTree<Integer> algorithm) {
        super("AVL Tree 可视化");
        this.algorithm = algorithm;
        this.canvas = new TreePanel<>(null);

        // --- 修复方案开始 ---

        // 1. 撑大骨架：给画布一个默认大小，防止它初始化时坍缩成 0x0
        canvas.setPreferredSize(new Dimension(1000, 600));

        // 2. 预先绘制：不要等算法动了才画，现在的空状态（或初始数据）也要马上画出来
        // 这样窗口一弹出来，背景网格、文字或者空的根节点就能被看到了
        canvas.display(algorithm.getRoot());

        // --- 修复方案结束 ---

        setupTreeButtons();

        // ... (中间的各种 label 设置和 listener 绑定保持不变) ...
        this.algorithm.setStepListener(() -> {
            // 关键：必须调用父类的 onStep，它会根据滑动条的速度进行 Thread.sleep
            this.onStep(null, 0, 0, algorithm.getCompareCount(), algorithm.getActionCount());
        });
        add(canvas, BorderLayout.CENTER);
        this.revalidate();
        initAndLaunch();
    }

    private void setupTreeButtons() {
        // A. 确保输入框已经实例化
        if (inputField == null) {
            inputField = new JTextField("40, 20, 10, 30, 50, 60, 70", 20);
        }

        // B. 【核心修复】夺舍父类的 startBtn，而不是创建新按钮
        startBtn.setText("插入");
        resetBtn.setText("重置");

        // C. 清空旧监听器，防止它跑父类的逻辑
        for (var al : startBtn.getActionListeners())
            startBtn.removeActionListener(al);
        for (var al : resetBtn.getActionListeners())
            resetBtn.removeActionListener(al);

        // D. 重新绑定：点击时不仅要跑算法，还要打印日志确认启动
        startBtn.addActionListener(e -> {
            System.out.println(">>> 插入任务启动！内容: " + inputField.getText());
            launchTask(true);
        });

        resetBtn.addActionListener(e -> {
            algorithm.clear(); // 假设你有这个方法
            refresh(null, 0, 0);
        });

        // E. 重新装配底栏顺序
        controlPanel.removeAll();
        controlPanel.add(startBtn); // 现在它是“插入”
        controlPanel.add(resetBtn); // 现在它是“重置”
        controlPanel.add(new JLabel("<html><font color='white'>速度(ms): </font></html>"));
        controlPanel.add(speedSlider); // 别忘了把父类的滑块加回来
        controlPanel.add(new JLabel("<html><font color='white'>输入: </font></html>"));
        controlPanel.add(inputField);

        controlPanel.revalidate();
        controlPanel.repaint();
    }

    private void launchTask(boolean isInsert) {
        Integer[] vals = parseInput();
        if (vals.length == 0)
            return;

        new Thread(() -> {
            try {
                this.startTime = System.currentTimeMillis();
                for (Integer val : vals) {
                    if (isInsert)
                        algorithm.put(val);
                    else
                        algorithm.remove(val);

                    // 关键：给算法一点喘息时间，触发 refresh
                    Thread.sleep(100);
                }
            } catch (Exception ex) {
                ex.printStackTrace(); // 如果报错了，这里能看到原因
            }
        }, "Algorithm-Thread").start();
    }

    @Override
    protected void refresh(TreeNode<Integer> data, int a, int b) {
        // 框架逻辑：每一帧去算法内核里“抓取”最新的树快照
        canvas.display(algorithm.getRoot());
        // 更新统计看板
        swapLabel.setText("旋转: " + algorithm.getActionCount());
    }

    private Integer[] parseInput() {
        String text = inputField.getText();
        if (text == null || text.trim().isEmpty()) {
            return new Integer[0];
        }

        // 按逗号分割，并利用 Java 8 流处理去除空格和解析整数
        try {
            String[] parts = text.split(",");
            java.util.List<Integer> list = new java.util.ArrayList<>();
            for (String s : parts) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    list.add(Integer.parseInt(trimmed));
                }
            }
            return list.toArray(new Integer[0]);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "输入格式错误，请输入逗号分隔的数字！");
            return new Integer[0];
        }
    }
}