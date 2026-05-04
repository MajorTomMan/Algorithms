package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.sort.BaseSortAlgorithms;
import com.majortom.algorithms.core.sort.alg.HeapSort;
import com.majortom.algorithms.core.sort.alg.InsertionSort;
import com.majortom.algorithms.core.sort.alg.QuickSort;
import com.majortom.algorithms.core.sort.alg.SelectionSort;
import com.majortom.algorithms.core.sort.impl.ArraySortEntity;
import com.majortom.algorithms.utils.AlgorithmsUtils;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.VisualizationActionType;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;

public class SortController<T extends Comparable<T>> extends BaseModuleController<BaseSort<T>> {

    private record SortOption<T extends Comparable<T>>(
            String algorithmId,
            String labelKey,
            Supplier<BaseSortAlgorithms<T>> factory) {
    }

    private BaseSort<T> sortData;
    private T[] sourceData;

    @FXML
    private Label sizeLabel;
    @FXML
    private Button genBtn;
    @FXML
    private Button sortBtn;
    @FXML
    private Slider sizeSlider;
    @FXML
    private ComboBox<String> algorithmSelector;

    public SortController(BaseVisualizer<BaseSort<T>> visualizer) {
        super(visualizer, "/fxml/SortControls.fxml");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        if (sizeSlider != null) {
            sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!AlgorithmThreadManager.isRunning()) {
                    handleGenerate();
                }
            });
        }

        bindAlgorithmSelector();
        EffectUtils.applyDynamicEffect(genBtn, sortBtn);
        handleGenerate();
    }

    @FXML
    @SuppressWarnings("unchecked")
    private void handleGenerate() {
        int size = (sizeSlider != null) ? (int) sizeSlider.getValue() : 50;
        dispatchVisualizerAction(VisualizationActionType.SORT_GENERATE, Map.of(
                "size", size,
                "algorithmId", selectedAlgorithmId()));
        stopAlgorithm();

        Integer[] array = AlgorithmsUtils.randomArray(size, 100);
        this.sourceData = (T[]) array;
        this.sortData = new ArraySortEntity<>((T[]) array.clone());

        if (visualizer != null) {
            visualizer.render(sortData, null, null);
        }

        logI18n("message.sort.generated", size);
    }

    @Override
    public void handleAlgorithmStart() {
        if (sourceData == null || AlgorithmThreadManager.isRunning()) {
            return;
        }

        this.sortData = new ArraySortEntity<>(sourceData.clone());
        visualizer.render(sortData, null, null);
        startAlgorithm(selectedAlgorithm(), sortData);
    }

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseSort<T>> alg, BaseSort<T> entity) {
        ((BaseSortAlgorithms<T>) alg).sort(entity);
    }

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

    @Override
    protected void onAlgorithmFinished() {
        super.onAlgorithmFinished();
        if (sortData != null) {
            sortData.resetStatistics();
            visualizer.render(sortData, null, null);
        }
        logI18n("message.execution.finished");
    }

    @Override
    protected void onResetData() {
        handleGenerate();
    }

    @Override
    protected void setupI18n() {
        if (sizeLabel != null) {
            sizeLabel.textProperty().bind(I18N.createStringBinding("label.sort.size"));
        }
        if (genBtn != null) {
            genBtn.textProperty().bind(I18N.createStringBinding("action.sort.generate"));
        }
        if (sortBtn != null) {
            sortBtn.textProperty().bind(I18N.createStringBinding("action.sort.run"));
        }
    }

    @Override
    protected String moduleId() {
        return "sort";
    }

    @Override
    protected String executionAlgorithmId(BaseAlgorithms<BaseSort<T>> algorithm) {
        List<SortOption<T>> options = sortOptions();
        int index = algorithmSelector == null ? 0 : algorithmSelector.getSelectionModel().getSelectedIndex();
        if (index >= 0 && index < options.size()) {
            return options.get(index).algorithmId();
        }
        return super.executionAlgorithmId(algorithm);
    }

    @Override
    protected String executionInputSignature(BaseSort<T> data) {
        return Arrays.toString(sourceData);
    }

    @FXML
    public void handleSort() {
        dispatchVisualizerAction(VisualizationActionType.SORT_RUN, Map.of(
                "size", sourceData == null ? 0 : sourceData.length,
                "algorithmId", selectedAlgorithmId()));
        handleAlgorithmStart();
    }

    private BaseSortAlgorithms<T> selectedAlgorithm() {
        List<SortOption<T>> options = sortOptions();
        int index = (algorithmSelector == null) ? 0 : algorithmSelector.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= options.size()) {
            index = 0;
        }
        return options.get(index).factory().get();
    }

    private String selectedAlgorithmId() {
        List<SortOption<T>> options = sortOptions();
        int index = (algorithmSelector == null) ? 0 : algorithmSelector.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= options.size()) {
            index = 0;
        }
        return options.get(index).algorithmId();
    }

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

    private List<SortOption<T>> sortOptions() {
        return List.of(
                new SortOption<>("insertion-sort", "algorithm.sort.insertion", InsertionSort::new),
                new SortOption<>("selection-sort", "algorithm.sort.selection", SelectionSort::new),
                new SortOption<>("quick-sort", "algorithm.sort.quick", QuickSort::new),
                new SortOption<>("heap-sort", "algorithm.sort.heap", HeapSort::new));
    }
}
