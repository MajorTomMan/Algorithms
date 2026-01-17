package com.majortom.algorithms.core.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.sort.BaseSortAlgorithms;
import com.majortom.algorithms.core.sort.impl.ArraySortEntity;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import com.majortom.algorithms.core.visualization.international.I18N;
import com.majortom.algorithms.core.visualization.manager.AlgorithmThreadManager;
import com.majortom.algorithms.utils.AlgorithmsUtils;
import com.majortom.algorithms.utils.EffectUtils;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 排序算法模块控制器
 * 职责：连接排序逻辑与可视化渲染，管理随机数据的生成与统计反馈。
 */
public class SortController<T extends Comparable<T>> extends BaseModuleController<BaseSort<T>> {

    private final BaseSortAlgorithms<T> algorithm;
    private BaseSort<T> sortData;

    @FXML
    private Label sizeLabel;
    @FXML
    private Button genBtn;
    @FXML
    private Button sortBtn;
    @FXML
    private Slider sizeSlider;

    public SortController(BaseSortAlgorithms<T> algorithm, BaseVisualizer<BaseSort<T>> visualizer) {
        // 自动完成 FXML 挂载
        super(visualizer, "/fxml/SortControls.fxml");
        this.algorithm = algorithm;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        // 监听滑块：仅在算法空闲时自动调整数组规模
        if (sizeSlider != null) {
            sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!AlgorithmThreadManager.isRunning()) {
                    handleGenerate();
                }
            });
        }

        EffectUtils.applyDynamicEffect(genBtn, sortBtn);

        // 首次载入时初始化数据
        handleGenerate();
    }

    /**
     * UI 事件：生成新的随机数据
     */
    @FXML
    @SuppressWarnings("unchecked")
    private void handleGenerate() {
        stopAlgorithm();

        int size = (sizeSlider != null) ? (int) sizeSlider.getValue() : 50;

        // 生成随机数组并注入实体
        Integer[] array = AlgorithmsUtils.randomArray(size, 100);
        this.sortData = new ArraySortEntity<>((T[]) array);

        // 静态渲染初始状态
        if (visualizer != null) {
            visualizer.render(sortData, null, null);
        }

        logI18n("System: Data initialized with size %d", size);
    }

    /**
     * 实现基类钩子：响应全局 Start 按钮
     */
    @Override
    public void handleAlgorithmStart() {
        if (sortData != null && algorithm != null) {
            startAlgorithm(algorithm, sortData);
        }
    }

    /**
     * 算法核心执行路径
     */
    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseSort<T>> alg, BaseSort<T> entity) {
        if (alg instanceof BaseSortAlgorithms) {
            ((BaseSortAlgorithms<T>) alg).sort(entity);
        }
    }

    /**
     * 格式化统计信息：显示规模、交换/移动次数及比较次数
     */
    @Override
    protected String formatStatsMessage() {
        if (sortData == null)
            return "Size: 0";

        String sK = I18N.getBundle().getString("stats.action"); // 交换/赋值次数
        String cK = I18N.getBundle().getString("stats.compare"); // 比较次数

        return String.format("Size: %d | %s: %d | %s: %d",
                sortData.size(), sK, stats.actionCount, cK, stats.compareCount);
    }

    @Override
    protected void onAlgorithmFinished() {
        super.onAlgorithmFinished();
        if (sortData != null) {
            // 清理渲染残留的高亮状态（如比较色、交换色）
            sortData.resetAll();
            visualizer.render(sortData, null, null);
        }
        logI18n("side.finished");
    }

    /**
     * 实现基类的重置钩子：对应全局 Reset 按钮
     */
    @Override
    protected void onResetData() {
        handleGenerate();
    }

    @Override
    protected void setupI18n() {
        if (sizeLabel != null) {
            sizeLabel.textProperty().bind(I18N.createStringBinding("ctrl.sort.size"));
        }
        if (genBtn != null) {
            genBtn.textProperty().bind(I18N.createStringBinding("btn.sort.gen"));
        }
        if (sortBtn != null) {
            sortBtn.textProperty().bind(I18N.createStringBinding("btn.sort.run"));
        }
    }

    @FXML
    public void handleSort() {
        handleAlgorithmStart();
    }
}