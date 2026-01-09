package com.majortom.algorithms.core.visualization.impl.frame;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.visualization.BaseFrame;
import com.majortom.algorithms.core.visualization.impl.panel.SortPanel;
import com.majortom.algorithms.utils.AlgorithmsUtils;

import java.awt.BorderLayout;
import javax.swing.SwingUtilities;

/**
 * 排序可视化窗体实现（Sort Visualization Frame）
 * 职责：
 * 1. 状态管理：维护原始数据备份，通过 handleDataReset 恢复初始状态。
 * 2. 桥接渲染：接收算法层发出的索引信息，并将其透传给 SortPanel。
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
     * * @param data 待排序的初始数据
     * 
     * @param sortAlgorithm 排序算法实例
     */
    public SortFrame(int[] data, BaseSort sortAlgorithm) {
        super("排序可视化实验室 - " + sortAlgorithm.getClass().getSimpleName());

        // 1. 初始化数据及其备份
        this.data = data;
        this.originalData = data.clone();
        this.sortAlgorithm = sortAlgorithm;

        // 2. 建立监听连接：将算法同步监听绑定至当前窗体
        this.sortAlgorithm.setListener(this);

        // 3. 初始化画布
        this.panel = new SortPanel(data);
        add(this.panel, BorderLayout.CENTER);

        // 4. 配置任务逻辑
        setupActions();

        // 5. 启动窗体
        initAndLaunch();
    }

    /**
     * 配置交互动作
     */
    private void setupActions() {
        // 注册待执行任务：点击“开始”时运行算法
        this.setTask(() -> {
            sortAlgorithm.sort(this.data);
        });
    }

    /**
     * 实现 BaseFrame 要求的重置逻辑
     * 修复了之前传入对象类型不匹配的问题
     */
    @Override
    protected void handleDataReset() {
        // 1. 将当前数组物理还原为初始无序状态
        System.arraycopy(originalData, 0, this.data, 0, originalData.length);

        // 2. 归零算法内部的计数器（比较数、操作数）
        this.sortAlgorithm.resetStatistics();

        // 3. 更新画布：传入数组 data 而非算法对象，并重置焦点索引为 -1
        this.panel.updateData(this.data, -1, -1);

        // 4. 更新侧边栏文本显示
        updateSideAreaText();
    }

    /**
     * 实现 refresh 接口：将算法进度实时投影至 UI
     */
    @Override
    protected void refresh(int[] data, Object a, Object b) {
        // 更新图形画布
        panel.updateData(data, a, b);
        // 更新文本序列
        updateSideAreaText();
    }

    /**
     * 私有辅助方法：更新右侧实时文本序列
     */
    private void updateSideAreaText() {
        // 核心优化：大数据量下强行停止文本拼接，否则 StringBuilder 会撑爆内存
        if (data.length > 500) {
            dataListArea.setText(String.format("状态: 正在排序...\n总元素: %d\n(数据量过大，已禁用实时文本预览)", data.length));
            return;
        }

        StringBuilder sb = new StringBuilder("当前序列:\n");
        for (int i = 0; i < data.length; i++) {
            sb.append(data[i]);
            if (i < data.length - 1)
                sb.append(", ");
        }
        dataListArea.setText(sb.toString());
    }

    /**
     * 静态启动方法：生成指定长度的随机数组并开启实验室
     */
    public static void launch(BaseSort algorithm, int count) {
        Integer[] temp = AlgorithmsUtils.randomArray(count, 500);
        int[] data = AlgorithmsUtils.toPrimitive(temp);
        SwingUtilities.invokeLater(() -> {
            SortFrame frame = new SortFrame(data, algorithm);
            frame.initAndLaunch();
        });
    }

    /**
     * 静态启动方法：使用指定数据开启实验室
     */
    public static void launch(BaseSort algorithm, int[] data) {
        SwingUtilities.invokeLater(() -> {
            SortFrame frame = new SortFrame(data, algorithm);
            frame.initAndLaunch();
        });
    }
}