package com.majortom.algorithms.core.visualization.impl.frame;

import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.visualization.BaseFrame;
import com.majortom.algorithms.core.visualization.impl.panel.TreePanel;
import com.majortom.algorithms.utils.AlgorithmsUtils;

import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class TreeFrame extends BaseFrame<BaseTree<Integer>> {
    private final TreePanel<Integer> canvas;
    private final BaseTree<Integer> algorithm;
    private JTextField inputField;

    public TreeFrame(BaseTree<Integer> algorithm) {
        super("树算法实验室 - " + algorithm.getClass().getSimpleName());
        this.algorithm = algorithm;

        // 1. 初始化画布并使用 MigLayout 填充
        this.canvas = new TreePanel<>(null);
        add(canvas, "center, grow");

        // 2. 注册监听并配置 UI
        this.algorithm.setListener(this);
        setupTreeActions();

        initAndLaunch();
    }

    private void setupTreeActions() {
        // 1. 创建组件
        JLabel label = new JLabel("数据:");
        inputField = new JTextField(12);
        inputField.putClientProperty("JTextField.placeholderText", "例如: 50, 30, 70");

        JButton randomBtn = new JButton("随机生成");
        randomBtn.putClientProperty("JButton.buttonType", "roundRect");

        // 2. 移除旧的错误添加逻辑，改为按顺序注入到 controlPanel
        // 注意：BaseFrame 可能已经预置了 startBtn 和 resetBtn
        // 为了保证顺序，我们可以先移除所有组件再重新按顺序添加，或者利用索引插入

        // 推荐做法：按逻辑顺序插入到 controlPanel 的开头
        controlPanel.add(label, "gapleft 10", 0); // 索引 0
        controlPanel.add(inputField, "growx", 1); // 索引 1
        // startBtn 默认可能在位置 0，我们需要调整它

        // 简单的做法是直接利用 MigLayout 的流特性在特定位置添加：
        startBtn.setText("执行插入");

        randomBtn.addActionListener(e -> {
            Integer[] vals = AlgorithmsUtils.randomArray(5, 100);
            inputField.setText(Arrays.toString(vals).replaceAll("[\\[\\]]", ""));
        });

        // 将随机按钮放在“开始”按钮后面
        controlPanel.add(randomBtn, "gapleft 5");

        // 3. 绑定任务逻辑
        startBtn.addActionListener(e -> {
            Integer[] dataToInsert = parseInput();
            if (dataToInsert.length > 0) {
                this.setTask(() -> {
                    for (Integer v : dataToInsert) {
                        algorithm.put(v);
                    }
                });
                this.executeTask();
            }
        });
    }

    @Override
    protected void handleDataReset() {
        if (algorithm != null) {
            algorithm.clear();
            algorithm.resetStatistics();
        }
        canvas.updateData(null, null, null);
        dataListArea.setText("中序遍历: \n[]");
    }

    @Override
    protected void refresh(BaseTree<Integer> treeData, Object activeNode, Object secondaryNode) {
        if (treeData == null)
            return;

        // 渲染树结构动画
        canvas.updateData(treeData.getRoot(), activeNode, secondaryNode);

        // 更新侧边栏数据序列
        List<Integer> list = treeData.toList();
        dataListArea.setText("中序遍历: \n" + list.toString());
    }

    private Integer[] parseInput() {
        String text = inputField.getText();
        if (text == null || text.trim().isEmpty())
            return new Integer[0];
        try {
            String[] parts = text.replace("，", ",").split("[,\\s]+");
            List<Integer> list = new ArrayList<>();
            for (String s : parts) {
                if (!s.trim().isEmpty())
                    list.add(Integer.parseInt(s.trim()));
            }
            return list.toArray(new Integer[0]);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "格式错误：请输入数字序列", "输入提示", JOptionPane.WARNING_MESSAGE);
            return new Integer[0];
        }
    }

    public static void launch(BaseTree<Integer> algorithm, Integer[] initialData) {
        SwingUtilities.invokeLater(() -> {
            TreeFrame frame = new TreeFrame(algorithm);
            if (initialData != null && initialData.length > 0) {
                frame.inputField.setText(Arrays.toString(initialData).replaceAll("[\\[\\]]", ""));
                frame.setTask(() -> {
                    for (Integer val : initialData)
                        algorithm.put(val);
                });
            }
        });
    }
}