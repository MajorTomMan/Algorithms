package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.algorithm.array.sort.Sort;
import com.majortom.algorithms.structure.array.Array;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.impl.visualizer.ArrayVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.array.ArrayEventReducer;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
import com.majortom.algorithms.core.event.structure.ArrayStructureEvent;
import com.majortom.algorithms.core.snapshot.SequenceSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import com.majortom.algorithms.visualization.structure.SnapshotAlgorithmInputSupport;
import com.majortom.algorithms.visualization.structure.RuntimeValueTypeSupport;
import com.majortom.algorithms.visualization.structure.StructureCatalog;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapter;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public final class ArrayController extends BaseModuleController<ArrayViewState>
        implements AlgorithmSelectionSupport, StructureSnapshotSupport<SequenceSnapshot<Object>>,
        SnapshotAlgorithmInputSupport<SequenceSnapshot<Object>>, RuntimeValueTypeSupport {

    private final Random random = new Random();
    private final Array<Object> sourceArray;
    private StructureSnapshot<SequenceSnapshot<Object>> algorithmInputSnapshot;
    private Class<?> runtimeValueType = Integer.class;
    private ValueAdapter<Object> valueAdapter = ValueAdapters.requireObjectAdapter(Integer.class);
    private final LongProperty valueTypeRevision = new SimpleLongProperty();
    private int currentSize = 20;
    private boolean structureSelectionEnabled = true;
    private int algorithmSelectedIndex = -1;
    private Consumer<IndexSelection> selectionListener = ignored -> { };
    private Consumer<String> algorithmSelectionListener = ignored -> { };

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
    @FXML private Button updateElementBtn;

    @SuppressWarnings("unchecked")
    public ArrayController() {
        super(new ArrayVisualizer(), "/fxml/ArrayControls.fxml");
        sourceArray = structure("array", Array.class);
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
                generateBtn, sortBtn, addElementBtn, deleteElementBtn, updateElementBtn);
        arrayVisualizer().setOnIndexSelected(this::handleArraySelection);
        renderSource();
    }

    @FXML
    private void handleGenerate() {
        clearArraySelection();
        List<Object> values = randomValues();
        if (executeStructureOperation("generate", () -> {
            replaceArrayContents(values);
            return null;
        })) {
            renderSource();
            refreshStatsDisplay();
            logI18n("message.sort.generated", currentSize);
        }
    }

    private List<Object> randomValues() {
        List<Object> values = new ArrayList<>(currentSize);
        for (int index = 0; index < currentSize; index++) {
            if (runtimeValueType == Integer.class) {
                values.add(random.nextInt(100) + 1);
            } else if (runtimeValueType == String.class) {
                values.add("V" + (random.nextInt(100) + 1));
            } else {
                throw new IllegalStateException("No random generator for " + runtimeValueType.getName());
            }
        }
        return List.copyOf(values);
    }

    @Override
    protected boolean supportsDataTools() {
        return true;
    }

    @Override
    protected boolean showRandomDataTool() {
        return false;
    }

    @Override
    protected void applyBulkData(String input) {
        List<Object> values = parseBatchInput(input, valueAdapter);
        if (values == null) {
            return;
        }
        clearArraySelection();
        if (!executeStructureOperation("bulk-replace", () -> {
            replaceArrayContents(values);
            return null;
        })) {
            return;
        }
        renderSource();
        refreshStatsDisplay();
        if (!values.isEmpty()) {
            arrayVisualizer().selectIndex(0);
        }
        logI18n("message.data.bulk_applied", values.size());
    }

    private void replaceArrayContents(List<?> values) {
        sourceArray.initialize(values);
    }

    private List<Object> sourceValues() {
        return sourceValues(sourceArray);
    }

    private List<Object> sourceValues(com.majortom.algorithms.structure.array.ArrayStructure<?> array) {
        List<Object> values = new ArrayList<>(array.size());
        for (Object value : array) {
            values.add(value);
        }
        return List.copyOf(values);
    }

    private void renderSource() {
        renderStructureState(ArrayViewState.source(sourceValues()));
    }

    /** Projects the latest factual Array StructureEvent into the Structure presentation state. */
    private void renderLatestStructureMutation() {
        ArrayViewState.Mutation mutation = latestArrayMutation();
        renderStructureState(ArrayViewState.source(sourceValues(), mutation));
    }

    private ArrayViewState.Mutation latestArrayMutation() {
        List<com.majortom.algorithms.core.runtime.EventEnvelope> events = structureEvents();
        for (int index = events.size() - 1; index >= 0; index--) {
            Object event = events.get(index).event();
            if (event instanceof ArrayStructureEvent.Inserted inserted) {
                return ArrayViewState.Mutation.inserted(inserted.index());
            }
            if (event instanceof ArrayStructureEvent.Removed removed) {
                return ArrayViewState.Mutation.removed(removed.index());
            }
            if (event instanceof ArrayStructureEvent.Updated updated) {
                return ArrayViewState.Mutation.updated(updated.index());
            }
            if (event instanceof ArrayStructureEvent.Swapped swapped) {
                return ArrayViewState.Mutation.swapped(swapped.leftIndex(), swapped.rightIndex());
            }
        }
        return ArrayViewState.Mutation.none();
    }

    @Override
    public StructureSnapshot<SequenceSnapshot<Object>> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), runtimeValueType, new SequenceSnapshot<>(sourceValues()));
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<SequenceSnapshot<Object>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        snapshot.requireValueType(runtimeValueType);
        clearArraySelection();
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
    public void useSnapshotAsAlgorithmInput(StructureSnapshot<SequenceSnapshot<Object>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        snapshot.requireValueType(runtimeValueType);
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
        if (algorithmInputSnapshot == null) {
            return null;
        } else {
            return algorithmInputSnapshot.id();
        }
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
        List<Object> values;
        if (algorithmInputSnapshot == null) {
            values = sourceValues();
        } else {
            values = algorithmInputSnapshot.state().values();
        }
        renderViewState(ArrayViewState.source(values));
    }


    @Override
    public void previewStructureSnapshot(StructureSnapshot<SequenceSnapshot<Object>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        snapshot.requireValueType(runtimeValueType);
        clearArraySelection();
        renderPreviewState(ArrayViewState.source(snapshot.state().values()));
    }

    @Override
    public String describeStructureSnapshot(SequenceSnapshot<Object> state) {
        return I18N.text("snapshot.sort.detail", state.values().size());
    }

    @Override
    public String snapshotPrimaryCount(SequenceSnapshot<Object> state) {
        return Integer.toString(state.values().size());
    }

    @FXML
    private void handleAddElement() {
        clearArraySelection();
        Object value = parseValue(elementValueField, "message.error.invalid_sort_value");
        if (value == null) {
            return;
        }
        Integer index = parseOptionalIndex(elementIndexField, sourceArray.size());
        if (index == null && !elementIndexField.getText().isBlank()) {
            return;
        }
        int insertedIndex;
        if (index == null) {
            insertedIndex = sourceArray.size();
        } else {
            insertedIndex = index;
        }
        if (executeStructureOperation("insert", () -> {
            sourceArray.insert(insertedIndex, value);
            return null;
        })) {
            renderLatestStructureMutation();
            arrayVisualizer().selectIndex(insertedIndex);
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
            Object value = parseValue(elementValueField, "message.error.invalid_sort_value");
            if (value == null) {
                return;
            }
            index = indexOf(value);
            if (index < 0) {
                logI18n("message.sort.not_found", value);
                return;
            }
        }
        Object removed = sourceArray.get(index);
        int removedIndex = index;
        if (executeStructureOperation("remove", () -> {
            sourceArray.remove(removedIndex);
            return null;
        })) {
            renderLatestStructureMutation();
            selectArrayAfterRemoval(removedIndex);
            refreshStatsDisplay();
            logI18n("message.sort.deleted", removed, removedIndex);
        }
    }

    private void selectArrayAfterRemoval(int removedIndex) {
        if (sourceArray.size() == 0) {
            clearArraySelection();
            elementIndexField.clear();
            updateIndexField.clear();
            elementValueField.clear();
            updateValueField.clear();
            return;
        }
        int nextIndex = removedIndex;
        if (nextIndex >= sourceArray.size()) {
            nextIndex = sourceArray.size() - 1;
        }
        arrayVisualizer().selectIndex(nextIndex);
    }

    @FXML
    private void handleUpdateElement() {
        Integer index = parseOptionalIndex(updateIndexField, sourceArray.size() - 1);
        Object value = parseValue(updateValueField, "message.error.invalid_sort_value");
        if (index == null || value == null) {
            return;
        }
        Object previous = sourceArray.get(index);
        int updateIndex = index;
        if (executeStructureOperation("update", () -> {
            sourceArray.set(updateIndex, value);
            return null;
        })) {
            renderLatestStructureMutation();
            arrayVisualizer().selectIndex(updateIndex);
            refreshStatsDisplay();
            logI18n("message.sort.updated", updateIndex, previous, value);
        }
    }

    public void setSelectionListener(Consumer<IndexSelection> selectionListener) {
        if (selectionListener == null) {
            this.selectionListener = ignored -> { };
        } else {
            this.selectionListener = selectionListener;
        }
    }

    public void setStructureSelectionEnabled(boolean enabled) {
        if (structureSelectionEnabled != enabled) {
            clearArraySelection();
        }
        structureSelectionEnabled = enabled;
    }

    private void handleArraySelection(int index) {
        if (structureSelectionEnabled) {
            handleStructureArraySelection(index);
            return;
        }
        ArrayViewState state = latestViewState();
        if (state == null || index < 0 || index >= state.values().size()) {
            return;
        }
        algorithmSelectedIndex = index;
        selectionListener.accept(new IndexSelection(index, state.values().get(index), state.values().size()));
    }

    @Override
    protected void onPresentationStateChanged(ArrayViewState state) {
        if (structureSelectionEnabled || algorithmSelectedIndex < 0) {
            return;
        }
        if (algorithmSelectedIndex >= state.values().size()) {
            clearArraySelection();
            return;
        }
        arrayVisualizer().showSelection(algorithmSelectedIndex);
        selectionListener.accept(new IndexSelection(
                algorithmSelectedIndex,
                state.values().get(algorithmSelectedIndex),
                state.values().size()));
    }

    private void handleStructureArraySelection(int index) {
        if (index < 0 || index >= sourceArray.size()) {
            return;
        }
        Object value = sourceArray.get(index);
        if (elementIndexField != null) elementIndexField.setText(Integer.toString(index));
        if (updateIndexField != null) updateIndexField.setText(Integer.toString(index));
        if (elementValueField != null) elementValueField.setText(valueAdapter.format(value));
        if (updateValueField != null) updateValueField.setText(valueAdapter.format(value));
        selectionListener.accept(new IndexSelection(index, VisualValue.of(value), sourceArray.size()));
    }

    private void clearArraySelection() {
        algorithmSelectedIndex = -1;
        arrayVisualizer().clearSelection();
        selectionListener.accept(null);
    }

    private ArrayVisualizer arrayVisualizer() {
        return (ArrayVisualizer) visualizer;
    }

    public record IndexSelection(int index, VisualValue value, int size) {
    }

    private Object parseValue(TextField field, String errorKey) {
        try {
            return valueAdapter.parse(field.getText());
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
        StructureSnapshot<SequenceSnapshot<Object>> inputSnapshot;
        if (algorithmInputSnapshot == null) {
            inputSnapshot = captureStructureSnapshot();
        } else {
            inputSnapshot = algorithmInputSnapshot;
        }
        List<Object> values = inputSnapshot.state().values();
        if (values.isEmpty()) return;
        String algorithmId = selectedAlgorithmId();
        if (algorithmId == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        Sort<Object> algorithm = (Sort<Object>) algorithm(algorithmId, runtimeValueType, Sort.class);
        Array<Object> runtimeArray = new Array<>(values);
        startAlgorithm(algorithmId, values, () -> {
            algorithm.sort(runtimeArray);
            return sourceValues(runtimeArray);
        }, () -> new ArrayEventReducer(values));
    }

    @Override
    public boolean selectAlgorithm(String algorithmId) {
        List<String> algorithmIds = algorithmIds();
        int index = algorithmIds.indexOf(algorithmId);
        if (index < 0) {
            return false;
        }
        if (algorithmSelector != null) {
            algorithmSelector.getSelectionModel().select(index);
        }
        notifyAlgorithmSelection();
        return true;
    }

    @Override
    public List<String> algorithmIds() {
        return AlgorithmCatalog.arraySorts(runtimeValueType);
    }

    @Override
    public void setAlgorithmSelectionListener(Consumer<String> listener) {
        if (listener == null) {
            algorithmSelectionListener = ignored -> { };
        } else {
            algorithmSelectionListener = listener;
        }
        notifyAlgorithmSelection();
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
        clearArraySelection();
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

    @Override
    public String selectedAlgorithmId() {
        int index = 0;
        if (algorithmSelector != null && algorithmSelector.getSelectionModel().getSelectedIndex() >= 0) {
            index = algorithmSelector.getSelectionModel().getSelectedIndex();
        }
        List<String> algorithmIds = algorithmIds();
        if (algorithmIds.isEmpty()) {
            return null;
        } else {
            return algorithmIds.get(Math.min(index, algorithmIds.size() - 1));
        }
    }

    private void bindAlgorithmSelector() {
        algorithmSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            javafx.collections.ObservableList<String> labels = FXCollections.observableArrayList();
            for (String id : algorithmIds()) {
                labels.add(AlgorithmCatalog.name(id));
            }
            return labels;
        }, valueTypeRevision));
        algorithmSelector.getSelectionModel().selectedIndexProperty().addListener(
                (observable, previous, current) -> notifyAlgorithmSelection());
        Platform.runLater(() -> {
            algorithmSelector.getSelectionModel().selectFirst();
            notifyAlgorithmSelection();
        });
    }

    private void notifyAlgorithmSelection() {
        algorithmSelectionListener.accept(selectedAlgorithmId());
    }

    private void bindStructureSelector() {
        structureSelector.setItems(FXCollections.observableArrayList(StructureCatalog.name("array")));
        Platform.runLater(() -> structureSelector.getSelectionModel().selectFirst());
    }
    private int indexOf(Object value) {
        for (int index = 0; index < sourceArray.size(); index++) {
            if (Objects.equals(sourceArray.get(index), value)) return index;
        }
        return -1;
    }

    @Override
    public Class<?> runtimeValueType() {
        return runtimeValueType;
    }

    @Override
    public List<Class<?>> supportedValueTypes() {
        return ValueAdapters.supportedTypes();
    }

    @Override
    public void setRuntimeValueType(Class<?> valueType) {
        if (!supportedValueTypes().contains(valueType)) {
            throw new IllegalArgumentException("Unsupported Array value type: " + valueType.getName());
        }
        if (runtimeValueType.equals(valueType)) {
            return;
        }
        runtimeValueType = valueType;
        valueAdapter = ValueAdapters.requireObjectAdapter(valueType);
        valueTypeRevision.set(valueTypeRevision.get() + 1L);
        algorithmInputSnapshot = null;
        clearArraySelection();
        sourceArray.initialize(randomValues());
        invalidateExecutionForStructureChange();
        if (algorithmSelector != null) {
            algorithmSelector.getSelectionModel().clearSelection();
            algorithmSelector.getSelectionModel().selectFirst();
            notifyAlgorithmSelection();
        }
        if (controlPanel != null) {
            renderSource();
            refreshStatsDisplay();
        }
    }

}
