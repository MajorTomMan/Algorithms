package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.library.sort.model.IntegerSortInput;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.impl.visualizer.HistogramSortVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.runtime.sort.IntegerSortEventReducer;
import com.majortom.algorithms.visualization.runtime.sort.IntegerSortViewState;
import com.majortom.algorithms.visualization.structure.StructureSnapshot;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;

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
    @FXML private Label inputSectionLabel;
    @FXML private Label operationsSectionLabel;
    @FXML private Label executionSectionLabel;
    @FXML private Label sizeLabel;
    @FXML private ComboBox<String> structureSelector;
    @FXML private Slider sizeSlider;
    @FXML private Label sizeValueLabel;
    @FXML private Button generateBtn;
    @FXML private Button sortBtn;
    @FXML private ComboBox<String> algorithmSelector;
    @FXML private TextField elementValueField;
    @FXML private TextField elementIndexField;
    @FXML private TextField updateValueField;
    @FXML private TextField updateIndexField;
    @FXML private Button addElementBtn;
    @FXML private Button deleteElementBtn;
    @FXML private Button findElementBtn;
    @FXML private Button updateElementBtn;

    public SortController() {
        super(new HistogramSortVisualizer(), "/fxml/SortControls.fxml");
        generateData();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bindStructureSelector();
        bindAlgorithmSelector();
        sizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentSize = newValue.intValue();
            sizeValueLabel.setText(String.valueOf(currentSize));
        });
        sizeValueLabel.setText(String.valueOf(currentSize));
        EffectUtils.applyDynamicEffect(
                generateBtn, sortBtn, addElementBtn, deleteElementBtn,
                findElementBtn, updateElementBtn);
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
        renderStructureState(IntegerSortViewState.source(sourceData));
    }

    @Override
    public StructureSnapshot<IntegerSortViewState> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), IntegerSortViewState.source(sourceData));
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<IntegerSortViewState> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        sourceData = List.copyOf(snapshot.state().values());
        currentSize = sourceData.size();
        invalidateExecutionForInputChange();
        if (sizeSlider != null) {
            double sliderValue = Math.max(sizeSlider.getMin(), Math.min(sizeSlider.getMax(), currentSize));
            sizeSlider.setValue(sliderValue);
        }
        if (sizeValueLabel != null) {
            sizeValueLabel.setText(String.valueOf(currentSize));
        }
        renderSource();
        refreshStatsDisplay();
    }

    @Override
    public String describeStructureSnapshot(IntegerSortViewState state) {
        return I18N.text("snapshot.sort.detail", state.values().size());
    }

    @FXML
    private void handleAddElement() {
        Integer value = parseInteger(elementValueField, "message.error.invalid_sort_value");
        if (value == null) {
            return;
        }
        Integer index = parseOptionalIndex(elementIndexField, sourceData.size());
        if (index == null && !elementIndexField.getText().isBlank()) {
            return;
        }
        List<Integer> next = new ArrayList<>(sourceData);
        int insertedIndex = next.size();
        if (index == null) {
            next.add(value);
        } else {
            next.add(index, value);
            insertedIndex = index;
        }
        replaceSourceData(next);
        logI18n("message.sort.added", value, insertedIndex);
    }

    @FXML
    private void handleDeleteElement() {
        String indexText = elementIndexField.getText().trim();
        Integer index = parseOptionalIndex(elementIndexField, sourceData.size() - 1);
        if (index == null && !indexText.isBlank()) {
            return;
        }
        if (index == null) {
            Integer value = parseInteger(elementValueField, "message.error.invalid_sort_value");
            if (value == null) {
                return;
            }
            index = sourceData.indexOf(value);
            if (index < 0) {
                logI18n("message.sort.not_found", value);
                return;
            }
        }
        List<Integer> next = new ArrayList<>(sourceData);
        int removed = next.remove((int) index);
        replaceSourceData(next);
        logI18n("message.sort.deleted", removed, index);
    }

    @FXML
    private void handleFindElement() {
        Integer value = parseInteger(elementValueField, "message.error.invalid_sort_value");
        if (value == null) {
            return;
        }
        int index = sourceData.indexOf(value);
        if (index < 0) {
            logI18n("message.sort.not_found", value);
            return;
        }
        logI18n("message.sort.found", value, index);
    }

    @FXML
    private void handleUpdateElement() {
        Integer index = parseOptionalIndex(updateIndexField, sourceData.size() - 1);
        Integer value = parseInteger(updateValueField, "message.error.invalid_sort_value");
        if (index == null || value == null) {
            return;
        }
        List<Integer> next = new ArrayList<>(sourceData);
        int previous = next.set(index, value);
        replaceSourceData(next);
        logI18n("message.sort.updated", index, previous, value);
    }

    private void replaceSourceData(List<Integer> next) {
        sourceData = List.copyOf(next);
        invalidateExecutionForInputChange();
        renderSource();
        refreshStatsDisplay();
    }

    private Integer parseInteger(TextField field, String errorKey) {
        try {
            return Integer.valueOf(field.getText().trim());
        } catch (RuntimeException exception) {
            logI18n(errorKey);
            return null;
        }
    }

    private Integer parseOptionalIndex(TextField field, int maximum) {
        String text = field.getText().trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            int index = Integer.parseInt(text);
            if (index < 0 || index > maximum) {
                logI18n("message.error.invalid_sort_index");
                return null;
            }
            return index;
        } catch (NumberFormatException exception) {
            logI18n("message.error.invalid_sort_index");
            return null;
        }
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
        if (inputSectionLabel != null) {
            inputSectionLabel.textProperty().bind(I18N.createStringBinding("label.sort.input"));
        }
        if (sizeLabel != null) {
            sizeLabel.textProperty().bind(I18N.createStringBinding("label.sort.size"));
        }
        if (operationsSectionLabel != null) {
            operationsSectionLabel.textProperty().bind(I18N.createStringBinding("label.sort.operations"));
        }
        if (executionSectionLabel != null) {
            executionSectionLabel.textProperty().bind(I18N.createStringBinding("label.panel.execution"));
        }
        if (generateBtn != null) {
            generateBtn.textProperty().bind(I18N.createStringBinding("action.sort.generate"));
        }
        if (sortBtn != null) {
            sortBtn.textProperty().bind(I18N.createStringBinding("action.sort.run"));
        }
        if (elementValueField != null) {
            elementValueField.promptTextProperty().bind(I18N.createStringBinding("prompt.sort.value"));
        }
        if (elementIndexField != null) {
            elementIndexField.promptTextProperty().bind(I18N.createStringBinding("prompt.sort.index"));
        }
        if (updateIndexField != null) {
            updateIndexField.promptTextProperty().bind(I18N.createStringBinding("prompt.sort.index"));
        }
        if (updateValueField != null) {
            updateValueField.promptTextProperty().bind(I18N.createStringBinding("prompt.sort.new_value"));
        }
        bindButton(addElementBtn, "action.sort.add");
        bindButton(deleteElementBtn, "action.sort.delete");
        bindButton(findElementBtn, "action.sort.find");
        bindButton(updateElementBtn, "action.sort.update");
    }

    private void bindButton(Button button, String key) {
        if (button != null) {
            button.textProperty().bind(I18N.createStringBinding(key));
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
