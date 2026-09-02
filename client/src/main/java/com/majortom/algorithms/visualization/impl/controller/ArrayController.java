package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.library.sort.AbstractIntegerSort;
import com.majortom.algorithms.library.structure.MutableArray;
import com.majortom.algorithms.library.sort.model.IntegerSortInput;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.impl.visualizer.HistogramSortVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.runtime.sort.IntegerSortEventReducer;
import com.majortom.algorithms.visualization.runtime.sort.IntegerSortViewState;
import com.majortom.algorithms.core.snapshot.SequenceSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import com.majortom.algorithms.visualization.structure.SnapshotAlgorithmInputSupport;
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

public final class ArrayController extends BaseModuleController<IntegerSortViewState>
        implements AlgorithmSelectionSupport, StructureSnapshotSupport<SequenceSnapshot<Integer>>, SnapshotAlgorithmInputSupport<SequenceSnapshot<Integer>> {

    private final Random random = new Random();
    private final List<String> algorithmIds;
    private final MutableArray<Integer> sourceArray;
    private StructureSnapshot<SequenceSnapshot<Integer>> algorithmInputSnapshot;
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

    @SuppressWarnings("unchecked")
    public ArrayController() {
        super(new HistogramSortVisualizer(), "/fxml/ArrayControls.fxml");
        algorithmIds = registeredAlgorithmIds("array", "Integer");
        sourceArray = module("structure.array.Integer", MutableArray.class);
        replaceArrayContents(randomValues());
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
        List<Integer> values = randomValues();
        if (executeStructureOperation("generate", () -> {
            replaceArrayContents(values);
            return null;
        })) {
            renderSource();
            refreshStatsDisplay();
            logI18n("message.sort.generated", currentSize);
        }
    }

    private List<Integer> randomValues() {
        List<Integer> values = new ArrayList<>(currentSize);
        for (int index = 0; index < currentSize; index++) {
            values.add(random.nextInt(100) + 1);
        }
        return List.copyOf(values);
    }

    private void replaceArrayContents(List<Integer> values) {
        while (sourceArray.size() > 0) {
            sourceArray.remove(sourceArray.size() - 1);
        }
        for (int index = 0; index < values.size(); index++) {
            sourceArray.insert(index, values.get(index));
        }
    }

    private List<Integer> sourceValues() {
        return List.copyOf(sourceArray.raw());
    }

    private void renderSource() {
        renderStructureState(IntegerSortViewState.source(sourceValues()));
    }

    @Override
    public StructureSnapshot<SequenceSnapshot<Integer>> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), new SequenceSnapshot<>(sourceValues()));
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<SequenceSnapshot<Integer>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        replaceArrayContents(snapshot.state().values());
        currentSize = sourceArray.size();
        invalidateExecutionForStructureChange();
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
    public void useSnapshotAsAlgorithmInput(StructureSnapshot<SequenceSnapshot<Integer>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        algorithmInputSnapshot = snapshot;
        invalidateExecutionForInputChange();
    }

    @Override
    public void useCurrentStructureAsAlgorithmInput() {
        algorithmInputSnapshot = null;
        invalidateExecutionForInputChange();
    }

    @Override
    public String algorithmInputSnapshotId() {
        return algorithmInputSnapshot == null ? null : algorithmInputSnapshot.id();
    }

    @Override
    protected boolean algorithmInputTracksCurrentStructure() {
        return algorithmInputSnapshot == null;
    }


    @Override
    protected void restoreAlgorithmState() {
        if (latestViewState() != null) {
            super.restoreAlgorithmState();
            return;
        }
        List<Integer> values = algorithmInputSnapshot == null
                ? sourceValues() : algorithmInputSnapshot.state().values();
        renderViewState(IntegerSortViewState.source(values));
    }


    @Override
    public String describeStructureSnapshot(SequenceSnapshot<Integer> state) {
        return I18N.text("snapshot.sort.detail", state.values().size());
    }

    @FXML
    private void handleAddElement() {
        Integer value = parseInteger(elementValueField, "message.error.invalid_sort_value");
        if (value == null) {
            return;
        }
        Integer index = parseOptionalIndex(elementIndexField, sourceArray.size());
        if (index == null && !elementIndexField.getText().isBlank()) {
            return;
        }
        int insertedIndex = index == null ? sourceArray.size() : index;
        if (executeStructureOperation("insert", () -> {
            sourceArray.insert(insertedIndex, value);
            return null;
        })) {
            renderSource();
            refreshStatsDisplay();
            logI18n("message.sort.added", value, insertedIndex);
        }
    }

    @FXML
    private void handleDeleteElement() {
        String indexText = elementIndexField.getText().trim();
        Integer index = parseOptionalIndex(elementIndexField, sourceArray.size() - 1);
        if (index == null && !indexText.isBlank()) {
            return;
        }
        if (index == null) {
            Integer value = parseInteger(elementValueField, "message.error.invalid_sort_value");
            if (value == null) {
                return;
            }
            index = sourceArray.raw().indexOf(value);
            if (index < 0) {
                logI18n("message.sort.not_found", value);
                return;
            }
        }
        int removed = sourceArray.get(index);
        int removedIndex = index;
        if (executeStructureOperation("remove", () -> {
            sourceArray.remove(removedIndex);
            return null;
        })) {
            renderSource();
            refreshStatsDisplay();
            logI18n("message.sort.deleted", removed, removedIndex);
        }
    }

    @FXML
    private void handleFindElement() {
        Integer value = parseInteger(elementValueField, "message.error.invalid_sort_value");
        if (value == null) {
            return;
        }
        int index = sourceArray.raw().indexOf(value);
        if (index < 0) {
            logI18n("message.sort.not_found", value);
            return;
        }
        logI18n("message.sort.found", value, index);
    }

    @FXML
    private void handleUpdateElement() {
        Integer index = parseOptionalIndex(updateIndexField, sourceArray.size() - 1);
        Integer value = parseInteger(updateValueField, "message.error.invalid_sort_value");
        if (index == null || value == null) {
            return;
        }
        int previous = sourceArray.get(index);
        int updateIndex = index;
        if (executeStructureOperation("update", () -> {
            sourceArray.set(updateIndex, value);
            return null;
        })) {
            renderSource();
            refreshStatsDisplay();
            logI18n("message.sort.updated", updateIndex, previous, value);
        }
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
        if (isRunning()) return;
        StructureSnapshot<SequenceSnapshot<Integer>> inputSnapshot =
                algorithmInputSnapshot == null ? captureStructureSnapshot() : algorithmInputSnapshot;
        List<Integer> values = inputSnapshot.state().values();
        if (values.isEmpty()) return;
        String algorithmId = selectedAlgorithmId();
        IntegerSortInput input = new IntegerSortInput(values);
        AbstractIntegerSort algorithm = module("algorithm.array.Integer." + algorithmId, AbstractIntegerSort.class);
        startAlgorithm(algorithmId, input, () -> algorithm.sort(input), IntegerSortEventReducer::new);
    }

    @Override
    public boolean selectAlgorithm(String algorithmId) {
        int index = algorithmIds.indexOf(algorithmId);
        if (index < 0) {
            return false;
        }
        if (algorithmSelector != null) {
            algorithmSelector.getSelectionModel().select(index);
        }
        return true;
    }

    @FXML
    public void handleSort() {
        handleAlgorithmStart();
    }

    @Override
    protected String formatStatsMessage() {
        return String.format("%s | %s | %s | %s",
                I18N.text("stats.size", sourceArray.size()),
                formatMetric("stats.action", stats.metric("writes")),
                formatMetric("stats.compare", stats.metric("comparisons")),
                I18N.text("stats.frames", visualFrameCount()));
    }

    @Override
    protected void onResetData() {
        replaceArrayContents(randomValues());
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
        return "array";
    }

    private String selectedAlgorithmId() {
        int index = 0;
        if (algorithmSelector != null && algorithmSelector.getSelectionModel().getSelectedIndex() >= 0) {
            index = algorithmSelector.getSelectionModel().getSelectedIndex();
        }
        return algorithmIds.get(Math.min(index, algorithmIds.size() - 1));
    }

    private void bindAlgorithmSelector() {
        algorithmSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            javafx.collections.ObservableList<String> labels = FXCollections.observableArrayList();
            for (String id : algorithmIds) {
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
