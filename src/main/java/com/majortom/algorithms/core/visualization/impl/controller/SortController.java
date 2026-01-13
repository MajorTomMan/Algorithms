package com.majortom.algorithms.core.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.sort.BaseSortAlgorithms;
import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import com.majortom.algorithms.core.visualization.international.I18N;
import com.majortom.algorithms.core.visualization.manager.AlgorithmThreadManager;
import com.majortom.algorithms.utils.AlgorithmsUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * 排序算法控制器
 * 职责：连接排序逻辑与柱状图渲染。
 */
public class SortController<T extends Comparable<T>> extends BaseController<BaseSort<T>> {

    private final BaseSortAlgorithms<T> algorithm;
    private BaseSort<T> sortData;
    private Node customControlPane;
    @FXML
    private Label sizeLabel;
    @FXML
    private Button genBtn;
    @FXML
    private Button sortBtn;
    @FXML
    private Slider sizeSlider;

    public SortController(BaseSortAlgorithms<T> algorithm, BaseVisualizer<BaseSort<T>> visualizer) {
        // 🚩 修正：现在基类构造函数只接收 visualizer。
        // Algorithm 会在 startAlgorithm 时被注入。
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
            setupI18n();

        } catch (IOException e) {
            System.err.println("[Critical] Failed to load SortControls.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        if (sizeSlider != null) {
            sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                // 只有当算法没在运行时才自动生成，避免冲突
                if (!AlgorithmThreadManager.isRunning()) {
                    handleGenerate();
                }
            });
        }
        // 初始化时生成第一组随机数据
        handleGenerate();
    }

    @Override
    public List<Node> getCustomControls() {
        return Collections.singletonList(customControlPane);
    }

    @FXML
    @SuppressWarnings("unchecked")
    private void handleGenerate() {
        stopAlgorithm();

        int size = (sizeSlider != null) ? (int) sizeSlider.getValue() : 50;

        // 🚩 这里通过 Utils 生成 Integer 数组并强转，适配 BaseSort 的泛型 T
        Integer[] array = AlgorithmsUtils.randomArray(size, 100);
        this.sortData = new BaseSort<>((T[]) array);

        // 静态渲染首帧
        if (visualizer != null) {
            visualizer.render(sortData, null, null);
        }

        if (logArea != null) {
            logArea.appendText(String.format("System: Created %d elements.\n", size));
        }
    }

    @FXML
    private void handleSort() {
        handleAlgorithmStart();
    }

    @Override
    public void handleAlgorithmStart() {
        if (sortData == null)
            return;
        // 🚩 直接将当前生成的 sortData 扔进引擎
        startAlgorithm(algorithm, sortData);
    }

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseSort<T>> alg, BaseSort<T> entity) {
        // 🚩 类型窄化：由于 S 已经被约束为 BaseSort<T>，这里可以直接强转调用
        if (alg instanceof BaseSortAlgorithms) {
            ((BaseSortAlgorithms<T>) alg).sort(entity);
        }
    }

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        // 🚩 解决 Bound Value 报错的终点：
        // 只要 statsLabel 没在 FXML 里 bind 过 text 属性，setText 就是安全的。
        if (statsLabel != null && sortData != null) {
            statsLabel.setText(String.format("Size: %d\nCompares: %d\nSwaps: %d",
                    sortData.size(), compareCount, actionCount));
        }
    }

    @Override
    protected void onAlgorithmFinished() {
        super.onAlgorithmFinished();
        if (sortData != null) {
            sortData.reset(); // 清除最后的红色/紫色高亮
            visualizer.render(sortData, null, null);
        }
    }

    @Override
    protected void setupI18n() {
        // TODO Auto-generated method stub
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
}