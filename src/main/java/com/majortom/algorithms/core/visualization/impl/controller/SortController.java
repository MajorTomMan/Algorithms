package com.majortom.algorithms.core.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.sort.BaseSortAlgorithms;
import com.majortom.algorithms.core.sort.impl.ArraySortEntity;
import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import com.majortom.algorithms.core.visualization.international.I18N;
import com.majortom.algorithms.core.visualization.manager.AlgorithmThreadManager;
import com.majortom.algorithms.utils.AlgorithmsUtils;
import com.majortom.algorithms.utils.EffectUtils;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * 排序算法控制器
 * 职责：连接排序逻辑与柱状图渲染，支持数据生成与状态回滚。
 */
public class SortController<T extends Comparable<T>> extends BaseController<BaseSort<T>> {

    private final BaseSortAlgorithms<T> algorithm;
    private BaseSort<T> sortData;
    private Node customControlPane;

    @FXML
    private Label sizeLabel;
    @FXML
    private Button genBtn, sortBtn;
    @FXML
    private Slider sizeSlider;

    public SortController(BaseSortAlgorithms<T> algorithm, BaseVisualizer<BaseSort<T>> visualizer) {
        super(visualizer);
        this.algorithm = algorithm;
        loadFXMLControls();
    }

    private void loadFXMLControls() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SortControls.fxml"));
            loader.setResources(I18N.getBundle());
            loader.setController(this);
            this.customControlPane = loader.load();
        } catch (IOException e) {
            System.err.println("[Critical] Failed to load SortControls.fxml: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        setupI18n();

        if (sizeSlider != null) {
            sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!AlgorithmThreadManager.isRunning()) {
                    handleGenerate();
                }
            });
        }

        handleGenerate(); // 初始化随机数据
        EffectUtils.applyDynamicEffect(genBtn);
        EffectUtils.applyDynamicEffect(sortBtn);
    }

    // --- 实现 BaseController 核心动作接口 ---

    @Override
    public void handleStartAction() {
        if (sortData != null && algorithm != null) {
            startAlgorithm(algorithm, sortData);
        }
    }

    @Override
    public void handleResetAction() {
        stopAlgorithm();

        // 优先从父类持有的 originalData 快照恢复
        if (originalData != null) {
            BaseSort<T> restored = (BaseSort<T>) originalData.copy();
            updateCurrentDataReference(restored);
            visualizer.render(sortData, null, null);
            updateUIComponents(0, 0);

            if (logArea != null) {
                logArea.appendText("System: Sort data restored to initial state.\n");
            }
        } else {
            // 如果从未开始过算法（无快照），则重新生成当前规模的数据
            handleGenerate();
        }
    }

    // --- 业务逻辑 ---

    @FXML
    @SuppressWarnings("unchecked")
    private void handleGenerate() {
        stopAlgorithm();
        int size = (sizeSlider != null) ? (int) sizeSlider.getValue() : 50;

        T[] array = (T[]) AlgorithmsUtils.randomArray(size, 100);
        this.sortData = new ArraySortEntity<>(array);

        if (visualizer != null) {
            visualizer.render(sortData, null, null);
        }

        if (logArea != null) {
            logArea.appendText(String.format("System: Generated %d new elements.\n", size));
        }

        // 生成新数据后，清除旧的快照，确保 Reset 会停留在当前这组新数据上
        this.originalData = null;
    }

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseSort<T>> alg, BaseSort<T> entity) {
        if (alg instanceof BaseSortAlgorithms) {
            ((BaseSortAlgorithms<T>) alg).sort(entity);
        }
    }

    @Override
    protected void updateCurrentDataReference(BaseSort<T> restoredData) {
        this.sortData = restoredData;
    }

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        if (statsLabel != null && sortData != null) {
            statsLabel.setText(String.format("Size: %d\nCompares: %d\nSwaps: %d",
                    sortData.size(), compareCount, actionCount));
        }
    }

    @Override
    protected void onAlgorithmFinished() {
        super.onAlgorithmFinished();
        if (sortData != null) {
            sortData.resetAll(); // 清除渲染高亮状态
            visualizer.render(sortData, null, null);
        }
    }

    @Override
    public List<Node> getCustomControls() {
        return Collections.singletonList(customControlPane);
    }

    @Override
    protected void setupI18n() {
        if (sizeLabel != null)
            sizeLabel.textProperty().bind(I18N.createStringBinding("ctrl.sort.size"));
        if (genBtn != null)
            genBtn.textProperty().bind(I18N.createStringBinding("btn.sort.gen"));
        if (sortBtn != null)
            sortBtn.textProperty().bind(I18N.createStringBinding("btn.sort.run"));
    }

    // 兼容原 FXML 调用
    @FXML
    private void handleSort() {
        handleStartAction();
    }
}