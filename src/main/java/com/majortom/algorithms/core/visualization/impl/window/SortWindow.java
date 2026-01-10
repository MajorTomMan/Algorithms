package com.majortom.algorithms.core.visualization.impl.window;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.visualization.BaseWindow;
import com.majortom.algorithms.core.visualization.impl.visualizer.SortVisualizer;
import com.majortom.algorithms.utils.AlgorithmsUtils;
import javafx.application.Platform;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 排序可视化实验室窗口
 * 职责：适配 FXML 架构，管理排序算法状态与 Canvas 渲染逻辑
 */
public class SortWindow extends BaseWindow<int[]> {

    private SortVisualizer sortVisualizer;
    private final int[] data;
    private final int[] originalData;
    private final BaseSort sortAlgorithm;

    public SortWindow(int[] data, BaseSort sortAlgorithm) {
        this.data = data;
        this.originalData = data.clone();
        this.sortAlgorithm = sortAlgorithm;
    }

    @Override
    protected Region createCenterComponent() {
        // 创建 Canvas 渲染器并挂载到基类的 canvasContainer
        this.sortVisualizer = new SortVisualizer(data);
        return sortVisualizer;
    }

    @Override
    protected void setupActions() {
        // 1. 设置算法监听器，绑定到基类的 onSync 接口
        this.sortAlgorithm.setListener((data, a, b, comp, act) -> {
            this.onSync(data, a, b, comp, act);
        });

        // 2. 设置算法任务
        this.setTask(() -> {
            sortAlgorithm.sort(this.data);
        });
        
        // 3. 可以在此处修改开始按钮的文字
        startBtn.setText("开始排序");
    }

    @Override
    protected void handleDataReset() {
        // 1. 恢复原始数据
        System.arraycopy(originalData, 0, this.data, 0, originalData.length);
        this.sortAlgorithm.resetStatistics();

        // 2. 更新 UI 状态
        this.sortVisualizer.updateState(this.data, -1, -1);
        updateSideAreaText();
    }

    @Override
    protected void refresh(int[] data, Object a, Object b) {
        // 1. 更新 Canvas 柱状图
        sortVisualizer.updateState(data, a, b);

        // 2. 更新右侧 logArea 文字序列
        updateSideAreaText();
    }

    /**
     * 更新侧边栏的数据文本预览 (适配 XML 中的 logArea)
     */
    private void updateSideAreaText() {
        if (data.length > 500) {
            logArea.setText(String.format(
                    "状态: 正在排序...\n总元素: %d\n(由于数据量过大，已禁用实时序列预览)",
                    data.length));
            return;
        }

        String sequence = Arrays.stream(data)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", "));

        logArea.setText("当前序列:\n" + sequence);
    }

    /**
     * 静态启动入口
     */
    public static void launch(BaseSort algorithm, int count) {
        Integer[] temp = AlgorithmsUtils.randomArray(count, 500);
        int[] data = AlgorithmsUtils.toPrimitive(temp);
        launch(algorithm, data);
    }

    public static void launch(BaseSort algorithm, int[] data) {
        // 确保由 AlgorithmLab 统一调度或直接在 FX 线程运行
        Platform.runLater(() -> {
            try {
                SortWindow window = new SortWindow(data, algorithm);
                Stage stage = new Stage();
                window.start(stage);
                stage.setTitle("排序实验室 - " + algorithm.getClass().getSimpleName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}