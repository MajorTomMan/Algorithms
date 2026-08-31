package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.library.sort.model.IntegerSortInput;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.impl.visualizer.HistogramSortVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.runtime.sort.IntegerSortEventReducer;
import com.majortom.algorithms.visualization.runtime.sort.IntegerSortViewState;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

public final class SortController extends BaseModuleController<IntegerSortViewState> {

    private static final List<String> ALGORITHM_IDS = List.of(
            "insertion-sort", "selection-sort", "quick-sort", "heap-sort");

    private final Random random = new Random();
    private List<Integer> sourceData = List.of();
    private int currentSize = 20;

    @FXML private Label structureLabel;
    @FXML private Label algorithmLabel;
    @FXML private ComboBox<String> structureSelector;
    @FXML private Button sortBtn;
    @FXML private Button operationBtn;
    @FXML private ComboBox<String> algorithmSelector;

    public SortController() {
        super(new HistogramSortVisualizer(), "/fxml/SortControls.fxml");
        generateData();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bindStructureSelector();
        bindAlgorithmSelector();
        EffectUtils.applyDynamicEffect(sortBtn, operationBtn);
        renderSource();
    }

    @FXML
    private void handleGenerate() {
        invalidateExecutionForInputChange();
        generateData();
        renderSource();
        dispatchVisualizerAction(
                com.majortom.algorithms.visualization.VisualizationActionType.SORT_GENERATE,
                java.util.Map.of("size", currentSize));
        logI18n("message.sort.generated", currentSize);
    }

    private void generateData() {
        List<Integer> values = new ArrayList<>(currentSize);
        for (int index = 0; index < currentSize; index++) {
            values.add(random.nextInt(100) + 1);
        }
        sourceData = List.copyOf(values);
    }

    private void renderSource() {
        renderViewState(IntegerSortViewState.source(sourceData));
    }

    @FXML
    private void openSortOperationDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(I18N.text("dialog.sort.title"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Label valueLabel = new Label(String.valueOf(currentSize));
        OperationDialogTheme.addClasses(valueLabel, "size-value-highlight");
        Slider sizeSlider = new Slider(5, 100, currentSize);
        sizeSlider.setShowTickLabels(true);
        sizeSlider.setShowTickMarks(true);
        sizeSlider.setMajorTickUnit(25);
        sizeSlider.setPrefWidth(420);
        sizeSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                valueLabel.setText(String.valueOf(newValue.intValue())));
        Button generateButton = new Button(I18N.text("action.sort.generate"));
        OperationDialogTheme.addClasses(generateButton, "btn-ran-gold", "compact-button");
        generateButton.setOnAction(event -> {
            currentSize = (int) sizeSlider.getValue();
            handleGenerate();
        });
        Label sectionTitle = new Label(I18N.text("label.sort.size"));
        OperationDialogTheme.addClasses(sectionTitle, "dialog-section-title");
        VBox content = new VBox(12,
                sectionTitle,
                new HBox(12, sizeSlider, valueLabel),
                new HBox(10, generateButton));
        OperationDialogTheme.addClasses(content, "dialog-form-section");
        dialog.getDialogPane().setContent(content);
        OperationDialogTheme.apply(dialog, 560.0d);
        dialog.showAndWait();
    }

    @Override
    public void handleAlgorithmStart() {
        if (isRunning() || sourceData.isEmpty()) {
            return;
        }
        String algorithmId = selectedAlgorithmId();
        dispatchVisualizerAction(
                com.majortom.algorithms.visualization.VisualizationActionType.SORT_RUN,
                java.util.Map.of("algorithmId", algorithmId, "size", sourceData.size()));
        startAlgorithm(algorithmId, new IntegerSortInput(sourceData), IntegerSortEventReducer::new);
    }

    @FXML
    public void handleSort() {
        handleAlgorithmStart();
    }

    @Override
    protected String formatStatsMessage() {
        return String.format("%s | %s | %s | %s",
                I18N.text("stats.size", sourceData.size()),
                formatMetric("stats.action", stats.metric("writes")),
                formatMetric("stats.compare", stats.metric("comparisons")),
                I18N.text("stats.frames", stats.visualFrameCount()));
    }

    @Override
    protected void onResetData() {
        generateData();
        renderSource();
    }

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

    @Override
    protected String moduleId() {
        return "sort";
    }

    private String selectedAlgorithmId() {
        int index = 0;
        if (algorithmSelector != null && algorithmSelector.getSelectionModel().getSelectedIndex() >= 0) {
            index = algorithmSelector.getSelectionModel().getSelectedIndex();
        }
        return ALGORITHM_IDS.get(Math.min(index, ALGORITHM_IDS.size() - 1));
    }

    private void bindAlgorithmSelector() {
        algorithmSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            javafx.collections.ObservableList<String> labels = FXCollections.observableArrayList();
            for (String id : ALGORITHM_IDS) {
                labels.add(I18N.text(AlgorithmLabels.key(id)));
            }
            return labels;
        }, I18N.localeProperty()));
        Platform.runLater(() -> algorithmSelector.getSelectionModel().selectFirst());
    }

    private void bindStructureSelector() {
        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> FXCollections.observableArrayList(I18N.text("label.sort.structure.array")),
                I18N.localeProperty()));
        Platform.runLater(() -> structureSelector.getSelectionModel().selectFirst());
    }
}
