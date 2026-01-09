package com.majortom.algorithms.core.visualization.impl.frame;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.visualization.BaseFrame;
import com.majortom.algorithms.core.visualization.impl.panel.SortPanel;
import com.majortom.algorithms.utils.AlgorithmsUtils;

import java.awt.BorderLayout;
import javax.swing.SwingUtilities;

/**
 * 排序可视化窗体实现（Sort Visualization Frame）
 * * 职责：
 * 1. 状态管理：维护原始数据备份，支持“重置”功能回到初始无序状态。
 * 2. 桥接渲染：接收算法层发出的索引信息（a, b），并将其透传给 SortPanel 进行颜色高亮。
 * 3. 统计反馈：实时在侧边栏更新数组序列的字符串表示及排序性能数据。
 */
public class SortFrame extends BaseFrame<int[]> {
    /** 负责绘制柱状图的画布 */
    private final SortPanel panel;
    /** 当前正在被排序的数组引用 */
    private final int[] data;
    /** 原始数据的克隆备份，用于重置功能 */
    private final int[] originalData;
    /** 执行排序逻辑的算法对象 */
    private final BaseSort sortAlgorithm;

    /**
     * 构造函数
     * 
     * @param data          待排序的初始数据
     * @param sortAlgorithm 排序算法实例（如 BubbleSort, QuickSort）
     */
    public SortFrame(int[] data, BaseSort sortAlgorithm) {
        super("排序可视化实验室 - " + sortAlgorithm.getClass().getSimpleName());
        this.data = data;
        this.originalData = data.clone();
        this.sortAlgorithm = sortAlgorithm;

        // 1. 建立监听连接：将算法的 SyncListener 绑定至当前窗体
        this.sortAlgorithm.setListener(this);

        // 2. 组装组件：初始化排序画布并放置于中央区域
        this.panel = new SortPanel(data);
        add(this.panel, BorderLayout.CENTER);

        // 3. 事件绑定：配置开始与重置按钮的交互逻辑
        setupActions();

        // 4. 启动窗体：执行 pack 和 setVisible
        initAndLaunch();
    }

    /**
     * 配置交互动作
     * 定义排序任务的生命周期管理：包括任务装载、数据恢复及状态重置。
     */
    private void setupActions() {
        // 1. 初次装载任务：定义点击“开始”按钮时在子线程执行的算法闭包
        this.setTask(() -> {
            sortAlgorithm.resetStatistics(); // 初始化统计指标（比较与交换次数）
            sortAlgorithm.sort(this.data); // 执行排序核心逻辑
        });

        // 配置重置按钮逻辑
        resetBtn.addActionListener(e -> {
            // 将当前数组还原为备份的原始无序状态
            System.arraycopy(originalData, 0, this.data, 0, originalData.length);

            // 归零耗时统计与算法内部计数器
            this.startTime = 0;
            sortAlgorithm.resetStatistics();

            // 立即触发一帧重绘
            // 传递 -1 索引作为焦点，告知 SortPanel 移除所有柱体的高亮颜色
            refresh(this.data, -1, -1);
            // 由于某些算法执行后可能会改变内部状态或闭包引用，
            // 此处重新调用 setTask 确保下一次点击“开始”时，执行的是针对已重置数据的任务。
            this.setTask(() -> {
                sortAlgorithm.resetStatistics();
                sortAlgorithm.sort(this.data);
            });

            // 恢复 UI 初始可用状态
            startBtn.setEnabled(true);
        });
    }

    /**
     * 实现 refresh 接口：将算法进度投影至 UI
     * 
     * @param data 最新数组状态
     * @param a    当前正在操作的索引 A（标记为活跃色）
     * @param b    当前正在操作的索引 B（标记为对比色）
     */
    @Override
    protected void refresh(int[] data, Object a, Object b) {
        // 1. 更新图形画布
        panel.updateData(data, a, b);

        // 2. 更新右侧侧边栏的数据序列文本区
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            sb.append(data[i]).append(i == data.length - 1 ? "" : ", ");
        }
        dataListArea.setText(sb.toString());
    }

    /**
     * 静态启动方法：生成指定长度的随机数组并开启实验室
     * 
     * @param algorithm 算法实例
     * @param count     数组元素数量
     */
    public static void launch(BaseSort algorithm, int count) {
        Integer[] temp = AlgorithmsUtils.randomArray(count, 500);
        int[] data = AlgorithmsUtils.toPrimitive(temp);
        SwingUtilities.invokeLater(() -> {
            SortFrame frame = new SortFrame(data, algorithm);
            frame.initAndLaunch();

            // 装载闭包任务：将排序算法逻辑预存到任务池
            frame.setTask(() -> algorithm.sort(data));
        });
    }

    /**
     * 静态启动方法：使用指定数据开启实验室
     */
    public static void launch(BaseSort algorithm, int[] data) {
        SwingUtilities.invokeLater(() -> {
            SortFrame frame = new SortFrame(data, algorithm);
            frame.initAndLaunch();

            // 装载闭包任务
            frame.setTask(() -> algorithm.sort(data));
        });
    }
}