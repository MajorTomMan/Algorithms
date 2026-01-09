package com.majortom.algorithms.core.visualization.impl.frame;

import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.visualization.BaseFrame;
import com.majortom.algorithms.core.visualization.impl.panel.TreePanel;
import com.majortom.algorithms.utils.AlgorithmsUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 树算法可视化窗体实现（Tree Algorithm Visualization Frame）
 * * 职责：
 * 1. 架构粘合：将 BaseTree 算法逻辑与 TreePanel 绘图组件进行绑定。
 * 2. 交互控制：提供文本输入、随机生成等手段来控制树的构建过程。
 * 3. 状态投影：将算法执行中的中序遍历结果、节点焦点实时反馈至 UI。
 */
public class TreeFrame extends BaseFrame<BaseTree<Integer>> {
    /** 绘图画布实例，负责具体的树形渲染 */
    private final TreePanel<Integer> canvas;
    /** 被监听的树算法实例（如 AVLTree, BSTree） */
    private final BaseTree<Integer> algorithm;
    /** 用户数据输入框，用于接收自定义插入序列 */
    private JTextField inputField;

    /**
     * 构造函数：初始化 UI 并绑定算法监听
     * @param algorithm 要可视化的树算法实例
     */
    public TreeFrame(BaseTree<Integer> algorithm) {
        super("树算法实验室 - " + algorithm.getClass().getSimpleName());
        this.algorithm = algorithm;

        // 1. 初始化画布：默认数据为空，占据窗口中央区域
        this.canvas = new TreePanel<>(null);
        add(canvas, BorderLayout.CENTER);

        // 2. 注册监听器：将算法的 SyncListener 回调指向当前窗体，
        // 使得算法执行过程中产生的状态变化能实时通知给 UI。
        this.algorithm.setListener(this);

        // 3. 扩展控制组件：添加树算法特有的交互控件
        setupTreeButtons();

        // 4. 执行基类方法：完成组件校验、窗口居中并显示
        initAndLaunch();
    }

    /**
     * 配置树算法特有的交互按钮与输入框
     */
    private void setupTreeButtons() {
        inputField = new JTextField("", 10);
        controlPanel.add(new JLabel("<html><font color='white'> 数据: </font></html>"));
        controlPanel.add(inputField);
        
        // 修改基类通用的“开始”按钮文本，使其符合树的操作语境
        startBtn.setText("插入");
        
        // 增加“随机插入”功能：模拟批量数据录入
        JButton randomBtn = new JButton("随机插入");
        randomBtn.addActionListener(e -> {
            // 生成随机数组并预览至输入框
            Integer[] vals = AlgorithmsUtils.randomArray(5, 100);
            inputField.setText(java.util.Arrays.toString(vals).replaceAll("[\\[\\]]", ""));
            
            // 预装填任务：定义“发射”逻辑，但并不立即执行，需等待用户点击“插入”按钮
            this.setTask(() -> {
                for (Integer v : vals)
                    algorithm.put(v);
            });
        });
        controlPanel.add(randomBtn);
    }

    /**
     * 实现 refresh 接口：将算法同步过来的快照数据投影到 UI 界面
     * 此方法由父类 BaseFrame 在 Swing 事件分发线程（EDT）中安全调用。
     * * @param treeData      当前整棵树的算法对象（包含 Root 引用）
     * @param activeNode    正在操作的主节点索引/引用（如正在探测的节点）
     * @param secondaryNode 正在操作的副节点引用（如旋转时的对比节点）
     */
    @Override
    protected void refresh(BaseTree<Integer> treeData, Object activeNode, Object secondaryNode) {
        if (treeData == null)
            return;
        
        // 1. 同步数据至画布：TreePanel 内部会计算坐标并触发插值动画
        canvas.updateData(treeData.getRoot(), activeNode, secondaryNode);
        
        // 2. 逻辑验证：在侧边栏打印当前树的中序遍历序列，方便用户核对排序属性
        List<Integer> list = treeData.toList();
        dataListArea.setText("中序遍历: \n" + list.toString());
    }

    /**
     * 工具方法：解析输入框中的字符串序列为 Integer 数组
     */
    private Integer[] parseInput() {
        String text = inputField.getText();
        if (text == null || text.trim().isEmpty())
            return new Integer[0];
        try {
            String[] parts = text.split(",");
            java.util.List<Integer> list = new java.util.ArrayList<>();
            for (String s : parts) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty())
                    list.add(Integer.parseInt(trimmed));
            }
            return list.toArray(new Integer[0]);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "请输入逗号分隔的数字！");
            return new Integer[0];
        }
    }

    /**
     * 静态工厂启动入口
     * * @param algorithm   具体算法实现（如 BinarySearchTree）
     * @param initialData 初始准备演示的数据
     */
    public static void launch(BaseTree<Integer> algorithm, Integer[] initialData) {
        SwingUtilities.invokeLater(() -> {
            TreeFrame frame = new TreeFrame(algorithm);

            // 如果存在预设数据，将其装载进“任务弹膛”
            if (initialData != null && initialData.length > 0) {
                // 1. 将数据回填至输入框展示
                String preview = java.util.Arrays.toString(initialData).replaceAll("[\\[\\]]", "");
                frame.inputField.setText(preview);

                // 2. 设置 PendingTask：通过 Lambda 闭包定义算法线程的具体执行逻辑
                frame.setTask(() -> {
                    algorithm.resetStatistics(); // 重置计数器
                    for (Integer val : initialData) {
                        algorithm.put(val); // 逐步插入并触发同步
                    }
                });
            }

            frame.initAndLaunch();
        });
    }
}