package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.sort.BaseSortAlgorithms;
import com.majortom.algorithms.core.sort.impl.ArraySortEntity;
import com.majortom.algorithms.utils.AlgorithmsUtils;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.algorithm.AlgorithmDescriptor;
import com.majortom.algorithms.visualization.algorithm.AlgorithmFamily;
import com.majortom.algorithms.visualization.algorithm.AlgorithmRegistry;
import com.majortom.algorithms.visualization.algorithm.AlgorithmStructure;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 排序模块控制器。
 *
 * <p>它负责生成随机数组、选择排序算法、启动执行上下文，并把排序统计展示到主界面。</p>
 *
 * @param <T> 排序元素类型
 */
public class SortController<T extends Comparable<T>> extends BaseModuleController<BaseSort<T>> {

    /**
     * 当前正在展示或排序的数据结构。
     */
    private BaseSort<T> sortData;

    /**
     * 原始输入数组，用于每次运行前重新克隆，保证不同算法使用同一输入。
     */
    private T[] sourceData;

    /**
     * 当前排序数据规模。
     */
    private int currentSize = 20;

    @FXML
    private Label structureLabel;
    @FXML
    private Label algorithmLabel;
    @FXML
    private ComboBox<String> structureSelector;
    @FXML
    private Button sortBtn;
    @FXML
    private Button operationBtn;
    @FXML
    private ComboBox<String> algorithmSelector;

    /**
     * 创建排序模块控制器。
     *
     * @param visualizer 排序可视化器
     */
    public SortController(BaseVisualizer<BaseSort<T>> visualizer) {
        super(visualizer, "/fxml/SortControls.fxml");
    }

    /**
     * 初始化排序模块控件。
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        bindStructureSelector();
        bindAlgorithmSelector();
        EffectUtils.applyDynamicEffect(sortBtn, operationBtn);
        handleGenerate();
    }

    /**
     * 生成新的随机数组并渲染初始状态。
     */
    @FXML
    @SuppressWarnings("unchecked")
    private void handleGenerate() {
        stopAlgorithm();

        Integer[] array = AlgorithmsUtils.randomArray(currentSize, 100);
        this.sourceData = (T[]) array;
        this.sortData = new ArraySortEntity<>((T[]) array.clone());

        if (visualizer != null) {
            visualizer.render(sortData, null, null);
        }

        logI18n("message.sort.generated", currentSize);
    }

    /**
     * 打开排序操作弹窗。
     */
    @FXML
    private void openSortOperationDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(I18N.text("dialog.sort.title"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label valueLabel = new Label(String.valueOf(currentSize));
        valueLabel.getStyleClass().add("size-value-highlight");

        Slider sizeSlider = new Slider(5, 100, currentSize);
        sizeSlider.setShowTickLabels(true);
        sizeSlider.setShowTickMarks(true);
        sizeSlider.setMajorTickUnit(25);
        sizeSlider.setPrefWidth(420);
        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                valueLabel.setText(String.valueOf(newVal.intValue())));

        Button generateButton = new Button(I18N.text("action.sort.generate"));
        generateButton.getStyleClass().addAll("btn-ran-gold", "compact-button");
        generateButton.setOnAction(event -> {
            currentSize = (int) sizeSlider.getValue();
            handleGenerate();
        });

        HBox sizeRow = new HBox(12, sizeSlider, valueLabel);
        VBox content = new VBox(12,
                new Label(I18N.text("label.sort.size")),
                sizeRow,
                new HBox(10, generateButton));
        content.getStyleClass().add("operation-dialog-content");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style/ui_theme.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("operation-dialog-pane");
        dialog.getDialogPane().setPrefWidth(560);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    /**
     * 启动当前选中的排序算法。
     */
    @Override
    public void handleAlgorithmStart() {
        if (sourceData == null || AlgorithmThreadManager.isRunning()) {
            return;
        }

        this.sortData = new ArraySortEntity<>(sourceData.clone());
        visualizer.render(sortData, null, null);
        startAlgorithm(selectedAlgorithm(), sortData);
    }

    /**
     * 执行排序算法。
     */
    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseSort<T>> alg, BaseSort<T> entity) {
        ((BaseSortAlgorithms<T>) alg).sort(entity);
    }

    /**
     * 格式化排序模块统计信息。
     */
    @Override
    protected String formatStatsMessage() {
        if (sortData == null) {
            return I18N.text("stats.sort.empty");
        }

        return String.format("%s | %s | %s | %s",
                I18N.text("stats.size", sortData.size()),
                formatMetric("stats.action", stats.actionCount()),
                formatMetric("stats.compare", stats.compareCount()),
                I18N.text("stats.frames", stats.frameCount()));
    }

    /**
     * 排序结束后重置高亮并渲染最终有序数组。
     */
    @Override
    protected void onAlgorithmFinished() {
        super.onAlgorithmFinished();
        if (sortData != null) {
            sortData.resetStatistics();
            visualizer.render(sortData, null, null);
        }
        logI18n("message.execution.finished");
    }

    /**
     * 重置排序数据。
     */
    @Override
    protected void onResetData() {
        handleGenerate();
    }

    /**
     * 绑定排序模块国际化文案。
     */
    @Override
    protected void setupI18n() {
        if (structureLabel != null) {
            structureLabel.textProperty().bind(I18N.createStringBinding("label.common.structure"));
        }
        if (algorithmLabel != null) {
            algorithmLabel.textProperty().bind(I18N.createStringBinding("label.common.algorithm"));
        }
        if (sortBtn != null) {
            sortBtn.textProperty().bind(I18N.createStringBinding("action.sort.run"));
        }
        if (operationBtn != null) {
            operationBtn.textProperty().bind(I18N.createStringBinding("action.sort.operation"));
        }
    }

    /**
     * 获取模块 ID。
     */
    @Override
    protected String moduleId() {
        return "sort";
    }

    /**
     * 获取当前下拉选中的排序算法 ID。
     */
    @Override
    protected String executionAlgorithmId(BaseAlgorithms<BaseSort<T>> algorithm) {
        List<AlgorithmDescriptor> options = sortOptions();
        int index = algorithmSelector == null ? 0 : algorithmSelector.getSelectionModel().getSelectedIndex();
        if (index >= 0 && index < options.size()) {
            return options.get(index).id();
        }
        return super.executionAlgorithmId(algorithm);
    }

    /**
     * 使用原始数组作为排序输入签名。
     */
    @Override
    protected String executionInputSignature(BaseSort<T> data) {
        return Arrays.toString(sourceData);
    }

    /**
     * FXML 排序按钮入口。
     */
    @FXML
    public void handleSort() {
        handleAlgorithmStart();
    }

    /**
     * 创建当前选中的排序算法实例。
     */
    @SuppressWarnings("unchecked")
    private BaseSortAlgorithms<T> selectedAlgorithm() {
        List<AlgorithmDescriptor> options = sortOptions();
        int index = (algorithmSelector == null) ? 0 : algorithmSelector.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= options.size()) {
            index = 0;
        }
        return (BaseSortAlgorithms<T>) options.get(index).create();
    }

    /**
     * 绑定算法下拉框选项。
     */
    private void bindAlgorithmSelector() {
        if (algorithmSelector == null) {
            return;
        }

        algorithmSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> labels = FXCollections.observableArrayList();
            sortOptions().forEach(option -> labels.add(I18N.text(option.labelKey())));
            return labels;
        }, I18N.localeProperty()));

        Platform.runLater(() -> algorithmSelector.getSelectionModel().selectFirst());
    }

    /**
     * 绑定结构下拉框。
     */
    private void bindStructureSelector() {
        if (structureSelector == null) {
            return;
        }

        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> labels = FXCollections.observableArrayList();
            labels.add(I18N.text("label.sort.structure.array"));
            return labels;
        }, I18N.localeProperty()));

        Platform.runLater(() -> structureSelector.getSelectionModel().selectFirst());
    }

    /**
     * 构建可选排序算法列表。
     */
    private List<AlgorithmDescriptor> sortOptions() {
        return AlgorithmRegistry.find(moduleId(), AlgorithmFamily.SORT, AlgorithmStructure.ARRAY_SORT);
    }
}
