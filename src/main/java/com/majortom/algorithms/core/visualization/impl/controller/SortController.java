package com.majortom.algorithms.core.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.sort.BaseSortAlgorithms;
import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import com.majortom.algorithms.utils.AlgorithmsUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * 排序算法控制器
 * 职责：管理排序生命周期，通过依赖注入实现算法逻辑与视觉表现的动态绑定。
 */
public class SortController<T extends Comparable<T>> extends BaseController<BaseSort<T>> {

    private final BaseSortAlgorithms<T> algorithm;
    private final BaseVisualizer<BaseSort<T>> externalVisualizer; // 外部注入的 Visualizer
    private BaseSort<T> sortData;
    private Node customControlPane;

    private Label sideStatsLabel;
    private TextArea sideLogArea;

    @FXML
    private Slider sizeSlider;

    /**
     * 构造函数：实现算法逻辑与渲染器的注入
     */
    public SortController(BaseSortAlgorithms<T> algorithm, BaseVisualizer<BaseSort<T>> visualizer) {
        super(algorithm, visualizer);
        this.algorithm = algorithm;
        this.externalVisualizer = visualizer;
        loadFXMLControls();
    }

    private void loadFXMLControls() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SortControls.fxml"));
            loader.setController(this);
            this.customControlPane = loader.load();
        } catch (IOException e) {
            System.err.println("[Error] SortControls.fxml load failed: " + e.getMessage());
        }
    }

    public void setUIReferences(Label statsLabel, TextArea logArea) {
        this.sideStatsLabel = statsLabel;
        this.sideLogArea = logArea;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 挂载注入的可视化器
        this.visualizer = this.externalVisualizer;
        // 初始生成数据
        handleGenerate();
    }

    @Override
    public List<Node> getCustomControls() {
        return Collections.singletonList(customControlPane);
    }

    // --- 交互事件 ---

    @FXML
    @SuppressWarnings("unchecked")
    private void handleGenerate() {
        int size = (int) sizeSlider.getValue();

        // 使用工具类生成随机数组（适配 Integer 类型）
        Integer[] array = AlgorithmsUtils.randomArray(size, 100);

        // 封装为排序实体
        this.sortData = new BaseSort<>((T[]) array);

        // 使用统一的 render 接口进行初始绘制，不再强制强转子类
        if (visualizer != null) {
            visualizer.render(sortData, null, null);
        }

        if (sideLogArea != null) {
            sideLogArea.appendText(String.format("System: Created %d elements for sorting.\n", size));
        }
    }

    @FXML
    private void handleSort() {
        // 检查状态，防止重复启动
        startAlgorithm(algorithm, sortData);
    }

    // --- 核心调度适配 ---

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseSort<T>> alg, BaseSort<T> entity) {
        // 这里的逻辑已经通过 BaseController 的泛型 T (即 BaseSort<T>) 锁定了安全性
        if (alg instanceof BaseSortAlgorithms) {
            ((BaseSortAlgorithms<T>) alg).sort(entity);
        }
    }

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        if (sideStatsLabel != null) {
            Platform.runLater(() -> sideStatsLabel.setText(
                    String.format("Comparisons: %d\nSwaps/Actions: %d\nSize: %d",
                            compareCount, actionCount, sortData.size())));
        }
    }

    @Override
    protected void onAlgorithmFinished() {
        Platform.runLater(() -> {
            if (sideLogArea != null)
                sideLogArea.appendText("System: Sorting completed.\n");

            // 清理算法留下的高亮索引（i, j）
            sortData.clearStatus();

            // 最终刷新：确保最后一帧不留焦点颜色
            if (visualizer != null) {
                visualizer.render(sortData, null, null);
            }
        });
    }

    @Override
    public void handleAlgorithmStart() {
        // TODO Auto-generated method stub
        startAlgorithm(algorithm, sortData);
    }
}