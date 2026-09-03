package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.StringSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.library.string.StringSearch;
import com.majortom.algorithms.library.structure.StringStructure;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.impl.visualizer.StringVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.runtime.string.StringEventReducer;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import com.majortom.algorithms.visualization.structure.SnapshotAlgorithmInputSupport;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public final class StringController extends BaseModuleController<StringViewState>
        implements AlgorithmSelectionSupport, StructureSnapshotSupport<StringSnapshot>, SnapshotAlgorithmInputSupport<StringSnapshot> {

    private final List<String> algorithmIds = AlgorithmCatalog.stringSearches();
    private final StringStructure source;
    private StructureSnapshot<StringSnapshot> algorithmInputSnapshot;

    @FXML private Label structureLabel;
    @FXML private ComboBox<String> structureSelector;
    @FXML private Label algorithmLabel;
    @FXML private ComboBox<String> algorithmSelector;
    @FXML private Label editSectionLabel;
    @FXML private Label searchSectionLabel;
    @FXML private TextField valueField;
    @FXML private TextField indexField;
    @FXML private TextField lengthField;
    @FXML private TextField characterField;
    @FXML private TextField patternField;
    @FXML private Button replaceBtn;
    @FXML private Button insertBtn;
    @FXML private Button removeBtn;
    @FXML private Button updateBtn;
    @FXML private Button runBtn;

    public StringController() {
        super(new StringVisualizer(), "/fxml/StringControls.fxml");
        source = module("structure.string.String", com.majortom.algorithms.library.basic.String.class);
        source.replace(0, source.length(), "ABABDABACDABABCABAB");
        renderSource();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bindSelectors();
        valueField.setText(source.value());
        patternField.setText("ABABCABAB");
        renderSource();
    }

    @FXML
    private void handleReplace() {
        String value = valueField.getText();
        if (executeStructureOperation("replace", () -> {
            source.replace(0, source.length(), value);
            return null;
        })) renderSource();
    }

    @FXML
    private void handleInsert() {
        Integer index = parseIndex(indexField, true);
        if (index == null) return;
        String value = valueField.getText();
        if (value.isEmpty()) return;
        if (executeStructureOperation("insert", () -> {
            source.insert(index, value);
            return null;
        })) renderSource();
    }

    @FXML
    private void handleRemove() {
        Integer index = parseIndex(indexField, false);
        Integer length = parsePositive(lengthField);
        if (index == null || length == null || index + length > source.length()) {
            logI18n("message.string.invalid_range");
            return;
        }
        if (executeStructureOperation("remove", () -> {
            source.remove(index, length);
            return null;
        })) renderSource();
    }

    @FXML
    private void handleUpdate() {
        Integer index = parseIndex(indexField, false);
        String value = characterField.getText();
        if (index == null || value.length() != 1) {
            logI18n("message.string.invalid_character");
            return;
        }
        char character = value.charAt(0);
        if (executeStructureOperation("update", () -> {
            source.set(index, character);
            return null;
        })) renderSource();
    }

    @Override
    @FXML
    public void handleAlgorithmStart() {
        if (isRunning()) return;
        String pattern = patternField.getText();
        if (pattern == null || pattern.isEmpty()) {
            logI18n("message.string.pattern_required");
            return;
        }
        StructureSnapshot<StringSnapshot> inputSnapshot =
                algorithmInputSnapshot == null ? captureStructureSnapshot() : algorithmInputSnapshot;
        String target = inputSnapshot.state().value();
        String algorithmId = selectedAlgorithmId();
        if (algorithmId == null) {
            return;
        }
        StringStructure input = new com.majortom.algorithms.library.basic.String(target);
        @SuppressWarnings("unchecked")
        StringSearch<List<Integer>> algorithm = (StringSearch<List<Integer>>)
                module("algorithm.string.String." + algorithmId, StringSearch.class);
        startAlgorithm(algorithmId, Map.of("target", target, "pattern", pattern),
                () -> algorithm.search(input, pattern), () -> new StringEventReducer(target));
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

    @Override
    public StructureSnapshot<StringSnapshot> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), new StringSnapshot(source.value()));
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<StringSnapshot> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        source.replace(0, source.length(), snapshot.state().value());
        invalidateExecutionForStructureChange();
        if (valueField != null) valueField.setText(source.value());
        renderSource();
    }

    @Override
    public void useSnapshotAsAlgorithmInput(StructureSnapshot<StringSnapshot> snapshot) {
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
        String value = algorithmInputSnapshot == null
                ? source.value() : algorithmInputSnapshot.state().value();
        renderViewState(StringViewState.source(value));
    }


    @Override
    public String describeStructureSnapshot(StringSnapshot state) {
        String value = state.value();
        String preview = value.length() <= 18 ? value : value.substring(0, 18) + "…";
        return I18N.text("snapshot.string.detail", value.length(), preview);
    }

    @Override
    protected String formatStatsMessage() {
        return String.format("%s | %s | %s", I18N.text("stats.size", source.length()),
                formatMetric("stats.compare", stats.metric("comparisons")),
                I18N.text("stats.frames", visualFrameCount()));
    }

    @Override
    protected void onResetData() {
        source.replace(0, source.length(), "ABABDABACDABABCABAB");
        if (valueField != null) valueField.setText(source.value());
        if (patternField != null) patternField.setText("ABABCABAB");
        renderSource();
    }

    @Override
    protected void setupI18n() {
        if (structureLabel != null) structureLabel.textProperty().bind(I18N.createStringBinding("label.common.structure"));
        if (algorithmLabel != null) algorithmLabel.textProperty().bind(I18N.createStringBinding("label.common.algorithm"));
        if (editSectionLabel != null) editSectionLabel.textProperty().bind(I18N.createStringBinding("label.string.edit"));
        if (searchSectionLabel != null) searchSectionLabel.textProperty().bind(I18N.createStringBinding("label.string.search"));
        bindButton(replaceBtn, "action.string.replace");
        bindButton(insertBtn, "action.string.insert");
        bindButton(removeBtn, "action.string.remove");
        bindButton(updateBtn, "action.string.update");
        bindButton(runBtn, "action.string.run");
        if (valueField != null) valueField.promptTextProperty().bind(I18N.createStringBinding("prompt.string.value"));
        if (indexField != null) indexField.promptTextProperty().bind(I18N.createStringBinding("prompt.string.index"));
        if (lengthField != null) lengthField.promptTextProperty().bind(I18N.createStringBinding("prompt.string.length"));
        if (characterField != null) characterField.promptTextProperty().bind(I18N.createStringBinding("prompt.string.character"));
        if (patternField != null) patternField.promptTextProperty().bind(I18N.createStringBinding("prompt.string.pattern"));
    }

    @Override
    protected String moduleId() {
        return "string";
    }

    private String selectedAlgorithmId() {
        int index = algorithmSelector == null ? 0 : algorithmSelector.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            index = 0;
        }
        return algorithmIds.isEmpty() ? null : algorithmIds.get(Math.min(index, algorithmIds.size() - 1));
    }

    private void bindSelectors() {
        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> FXCollections.observableArrayList(I18N.text("label.structure.string")), I18N.localeProperty()));
        algorithmSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            javafx.collections.ObservableList<String> labels = FXCollections.observableArrayList();
            for (String id : algorithmIds) {
                labels.add(AlgorithmLabels.text(id));
            }
            return labels;
        }, I18N.localeProperty()));
        Platform.runLater(() -> {
            structureSelector.getSelectionModel().selectFirst();
            algorithmSelector.getSelectionModel().selectFirst();
        });
    }

    private void renderSource() {
        renderStructureState(StringViewState.source(source.value()));
        if (valueField != null && !valueField.isFocused()) valueField.setText(source.value());
        refreshStatsDisplay();
    }

    private Integer parseIndex(TextField field, boolean allowEnd) {
        try {
            int index = Integer.parseInt(field.getText().trim());
            int max = allowEnd ? source.length() : source.length() - 1;
            if (index < 0 || index > max) throw new NumberFormatException();
            return index;
        } catch (RuntimeException exception) {
            logI18n("message.string.invalid_index");
            return null;
        }
    }

    private Integer parsePositive(TextField field) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            return value > 0 ? value : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void bindButton(Button button, String key) {
        if (button != null) button.textProperty().bind(I18N.createStringBinding(key));
    }
}
