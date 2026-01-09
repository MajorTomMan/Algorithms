package com.majortom.algorithms.core.visualization.impl.frame;

import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.visualization.BaseFrame;
import com.majortom.algorithms.core.visualization.impl.panel.TreePanel;
import com.majortom.algorithms.utils.AlgorithmsUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

/**
 * 树算法可视化窗体实现（Tree Algorithm Visualization Frame）
 * 职责：
 * 1. 架构粘合：将 BaseTree 算法逻辑与 TreePanel 绘图组件进行绑定。
 * 2. 状态重置：实现 handleDataReset 以清空树结构。
 */
public class TreeFrame extends BaseFrame<BaseTree<Integer>> {
    /** 绘图画布实例 */
    private final TreePanel<Integer> canvas;
    /** 树算法实例 */
    private final BaseTree<Integer> algorithm;
    /** 用户数据输入框 */
    private JTextField inputField;

    public TreeFrame(BaseTree<Integer> algorithm) {
        super("树算法实验室 - " + algorithm.getClass().getSimpleName());
        this.algorithm = algorithm;

        // 1. 初始化画布
        this.canvas = new TreePanel<>(null);
        add(canvas, BorderLayout.CENTER);

        // 2. 注册监听器
        this.algorithm.setListener(this);

        // 3. 扩展控制组件
        setupTreeButtons();

        // 4. 启动窗体
        initAndLaunch();
    }

    /**
     * 配置树算法特有的交互按钮与输入框
     */
    private void setupTreeButtons() {
        inputField = new JTextField("", 10);
        controlPanel.add(new JLabel("<html><font color='white'> 数据: </font></html>"));
        controlPanel.add(inputField);

        startBtn.setText("执行插入");

        // 增加“随机生成”功能：仅生成数据到输入框，不立即执行
        JButton randomBtn = new JButton("随机生成");
        randomBtn.addActionListener(e -> {
            Integer[] vals = AlgorithmsUtils.randomArray(5, 100);
            inputField.setText(java.util.Arrays.toString(vals).replaceAll("[\\[\\]]", ""));
        });
        controlPanel.add(randomBtn);

        // 覆盖基类的任务触发逻辑：确保点击“开始”时解析当前输入框的内容
        startBtn.addActionListener(e -> {
            Integer[] dataToInsert = parseInput();
            if (dataToInsert.length > 0) {
                this.setTask(() -> {
                    for (Integer v : dataToInsert) {
                        algorithm.put(v);
                    }
                });
            }
        });
    }

    /**
     * 实现重置逻辑：清空树的所有节点
     */
    @Override
    protected void handleDataReset() {
        // 1. 调用算法层的重置（假设 BaseTree 有清除根节点的逻辑）
        if (algorithm != null) {
            algorithm.clear(); // 需确保 BaseTree 实现或有此方法
            algorithm.resetStatistics();
        }

        // 2. 清空画布
        canvas.updateData(null, null, null);

        // 3. 清空侧边栏
        dataListArea.setText("中序遍历: \n[]");

        // 4. 清空输入框（可选，建议保留以便用户微调后再次运行）
        // inputField.setText("");
    }

    /**
     * 实现 refresh 接口：同步树的 Root 节点到画布
     */
    @Override
    protected void refresh(BaseTree<Integer> treeData, Object activeNode, Object secondaryNode) {
        if (treeData == null)
            return;

        // TreePanel 接收的是 Root Node 进行递归绘制
        canvas.updateData(treeData.getRoot(), activeNode, secondaryNode);

        // 更新中序遍历序列
        List<Integer> list = treeData.toList();
        dataListArea.setText("中序遍历: \n" + list.toString());
    }

    /**
     * 解析输入框：支持逗号分隔或空格分隔
     */
    private Integer[] parseInput() {
        String text = inputField.getText();
        if (text == null || text.trim().isEmpty())
            return new Integer[0];
        try {
            // 支持中文逗号、英文逗号和空格
            String[] parts = text.replace("，", ",").split("[,\\s]+");
            List<Integer> list = new ArrayList<>();
            for (String s : parts) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty())
                    list.add(Integer.parseInt(trimmed));
            }
            return list.toArray(new Integer[0]);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "数据格式错误，请输入数字序列（如: 10, 20, 15）");
            return new Integer[0];
        }
    }

    /**
     * 静态启动入口
     */
    public static void launch(BaseTree<Integer> algorithm, Integer[] initialData) {
        SwingUtilities.invokeLater(() -> {
            TreeFrame frame = new TreeFrame(algorithm);
            if (initialData != null && initialData.length > 0) {
                String preview = java.util.Arrays.toString(initialData).replaceAll("[\\[\\]]", "");
                frame.inputField.setText(preview);

                // 默认装载初始任务
                frame.setTask(() -> {
                    for (Integer val : initialData) {
                        algorithm.put(val);
                    }
                });
            }
        });
    }
}