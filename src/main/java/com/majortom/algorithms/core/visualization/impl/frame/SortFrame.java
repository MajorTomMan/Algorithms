package com.majortom.algorithms.core.visualization.impl.frame;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.visualization.BaseFrame;
import com.majortom.algorithms.core.visualization.impl.panel.SortPanel;
import com.majortom.algorithms.utils.AlgorithmsUtils;
import javax.swing.SwingUtilities;

public class SortFrame extends BaseFrame<int[]> {
    private final SortPanel panel;
    private final int[] data;
    private final int[] originalData;
    private final BaseSort sortAlgorithm;

    public SortFrame(int[] data, BaseSort sortAlgorithm) {
        super("排序可视化实验室 - " + sortAlgorithm.getClass().getSimpleName());

        this.data = data;
        this.originalData = data.clone();
        this.sortAlgorithm = sortAlgorithm;
        this.sortAlgorithm.setListener(this);

        this.panel = new SortPanel(data);

        // 改造：使用 MigLayout 中心填充约束
        add(this.panel, "center, grow");

        setupActions();
    }

    private void setupActions() {
        this.setTask(() -> {
            sortAlgorithm.sort(this.data);
        });
    }

    @Override
    protected void handleDataReset() {
        // 恢复初始数据状态
        System.arraycopy(originalData, 0, this.data, 0, originalData.length);
        this.sortAlgorithm.resetStatistics();

        // 同步 UI
        this.panel.updateData(this.data, -1, -1);
        updateSideAreaText();
    }

    @Override
    protected void refresh(int[] data, Object a, Object b) {
        panel.updateData(data, a, b);
        updateSideAreaText();
    }

    private void updateSideAreaText() {
        // 性能保护逻辑：防止 StringBuilder 撑爆内存
        if (data.length > 500) {
            dataListArea.setText(String.format("状态: 正在排序...\n总元素: %d\n(数据量过大，已禁用实时序列预览)", data.length));
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

    public static void launch(BaseSort algorithm, int count) {
        Integer[] temp = AlgorithmsUtils.randomArray(count, 500);
        int[] data = AlgorithmsUtils.toPrimitive(temp);
        launch(algorithm, data);
    }

    public static void launch(BaseSort algorithm, int[] data) {
        SwingUtilities.invokeLater(() -> {
            SortFrame frame = new SortFrame(data, algorithm);
            frame.initAndLaunch();
        });
    }
}