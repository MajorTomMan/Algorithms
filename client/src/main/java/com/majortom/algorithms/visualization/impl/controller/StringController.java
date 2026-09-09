package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.event.structure.StringStructureEvent;
import com.majortom.algorithms.core.snapshot.StringSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.library.string.LongestSubstringAlgorithm;
import com.majortom.algorithms.library.string.StringAlgorithm;
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
import java.util.function.Consumer;

public final class StringController extends BaseModuleController<StringViewState>
        implements AlgorithmSelectionSupport, StructureSnapshotSupport<StringSnapshot>, SnapshotAlgorithmInputSupport<StringSnapshot> {

    private final List<String> algorithmIds = AlgorithmCatalog.stringAlgorithms();
    private final StringStructure source;
    private StructureSnapshot<StringSnapshot> algorithmInputSnapshot;
    private boolean structureSelectionEnabled = true;
    private int algorithmSelectedIndex = -1;
    private Consumer<IndexSelection> selectionListener = ignored -> { };
    private Consumer<String> algorithmSelectionListener = ignored -> { };

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
        stringVisualizer().setOnIndexSelected(this::handleStringSelection);
        bindSelectors();
        valueField.setText(source.value());
        patternField.setText("ABABCABAB");
        patternField.textProperty().addListener((observable, previous, current) -> {
            if (!structureSelectionEnabled) {
                stringVisualizer().setAlgorithmPattern(current);
            }
        });
        renderSource();
    }

    @FXML
    private void handleReplace() {
        clearStringSelection();
        String value = valueField.getText();
        if (executeStructureOperation("replace", () -> {
            source.replace(0, source.length(), value);
            return null;
        })) {
            renderLatestStructureMutation();
            if (source.length() > 0) {
                stringVisualizer().selectIndex(0);
            } else {
                indexField.clear();
                characterField.clear();
                lengthField.clear();
            }
        }
    }

    @Override
    protected boolean supportsDataTools() {
        return true;
    }

    @Override
    protected String bulkInputPromptKey() {
        return "prompt.data.bulk.string";
    }

    @Override
    protected void applyBulkData(String input) {
        replaceFromDataTool(input == null ? "" : input, "message.data.bulk_applied");
    }

    @Override
    protected void randomizeData() {
        java.util.Random random = new java.util.Random();
        StringBuilder builder = new StringBuilder(20);
        for (int index = 0; index < 20; index++) {
            builder.append((char) ('A' + random.nextInt(26)));
        }
        replaceFromDataTool(builder.toString(), "message.data.randomized");
    }

    private void replaceFromDataTool(String value, String messageKey) {
        clearStringSelection();
        if (!executeStructureOperation("bulk-replace", () -> {
            source.replace(0, source.length(), value);
            return null;
        })) {
            return;
        }
        if (valueField != null) {
            valueField.setText(value);
        }
        renderLatestStructureMutation();
        if (source.length() > 0) {
            stringVisualizer().selectIndex(0);
        } else {
            indexField.clear();
            characterField.clear();
            lengthField.clear();
        }
        logI18n(messageKey, value.length());
    }

    @FXML
    private void handleInsert() {
        clearStringSelection();
        Integer index = parseIndex(indexField, true);
        if (index == null) return;
        String value = valueField.getText();
        if (value.isEmpty()) return;
        if (executeStructureOperation("insert", () -> {
            source.insert(index, value);
            return null;
        })) {
            renderLatestStructureMutation();
            stringVisualizer().selectIndex(index);
        }
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
        })) {
            renderLatestStructureMutation();
            selectStringAfterRemoval(index);
        }
    }

    private void selectStringAfterRemoval(int removedIndex) {
        if (source.length() == 0) {
            clearStringSelection();
            indexField.clear();
            characterField.clear();
            lengthField.clear();
            return;
        }
        int nextIndex = removedIndex;
        if (nextIndex >= source.length()) {
            nextIndex = source.length() - 1;
        }
        stringVisualizer().selectIndex(nextIndex);
    }

    @FXML
    private void handleUpdate() {
        clearStringSelection();
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
        })) {
            renderLatestStructureMutation();
            stringVisualizer().selectIndex(index);
        }
    }

    @Override
    @FXML
    public void handleAlgorithmStart() {
        if (isRunning()) {
            return;
        }
        String algorithmId = selectedAlgorithmId();
        if (algorithmId == null) {
            return;
        }
        StructureSnapshot<StringSnapshot> inputSnapshot;
        if (algorithmInputSnapshot == null) {
            inputSnapshot = captureStructureSnapshot();
        } else {
            inputSnapshot = algorithmInputSnapshot;
        }
        String target = inputSnapshot.state().value();
        StringStructure input = new com.majortom.algorithms.library.basic.String(target);
        StringAlgorithm algorithm = module(
                "algorithm.string.String." + algorithmId,
                StringAlgorithm.class);
        if (algorithm instanceof StringSearch search) {
            runStringSearch(algorithmId, search, input, target);
            return;
        }
        if (algorithm instanceof LongestSubstringAlgorithm longestSubstring) {
            runLongestSubstring(algorithmId, longestSubstring, input, target);
            return;
        }
        throw new IllegalStateException("Unsupported string algorithm contract: " + algorithm.getClass().getName());
    }

    private void runStringSearch(
            String algorithmId,
            StringSearch algorithm,
            StringStructure input,
            String target) {
        String pattern = patternField.getText();
        if (pattern == null || pattern.isEmpty()) {
            logI18n("message.string.pattern_required");
            return;
        }
        stringVisualizer().setAlgorithmPattern(pattern);
        startAlgorithm(
                algorithmId,
                Map.of("target", target, "pattern", pattern),
                () -> algorithm.search(input, pattern),
                () -> new StringEventReducer(target));
    }

    private void runLongestSubstring(
            String algorithmId,
            LongestSubstringAlgorithm algorithm,
            StringStructure input,
            String target) {
        stringVisualizer().clearAlgorithmPattern();
        startAlgorithm(
                algorithmId,
                Map.of("target", target),
                () -> algorithm.find(input),
                () -> new StringEventReducer(target));
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
        notifyAlgorithmSelection();
        return true;
    }

    @Override
    public List<String> algorithmIds() {
        return algorithmIds;
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

    @Override
    public StructureSnapshot<StringSnapshot> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), new StringSnapshot(source.value()));
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<StringSnapshot> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        clearStringSelection();
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
        String value;
        if (algorithmInputSnapshot == null) {
            value = source.value();
        } else {
            value = algorithmInputSnapshot.state().value();
        }
        renderViewState(StringViewState.source(value));
    }


    @Override
    public void previewStructureSnapshot(StructureSnapshot<StringSnapshot> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        clearStringSelection();
        renderPreviewState(StringViewState.source(snapshot.state().value()));
    }

    @Override
    public String describeStructureSnapshot(StringSnapshot state) {
        String value = state.value();
        String preview;
        if (value.length() <= 18) {
            preview = value;
        } else {
            preview = value.substring(0, 18) + "…";
        }
        return I18N.text("snapshot.string.detail", value.length(), preview);
    }

    @Override
    public String snapshotPrimaryCount(StringSnapshot state) {
        return Integer.toString(state.value().length());
    }

    @Override
    protected String formatStatsMessage() {
        return String.format("%s | %s | %s", I18N.text("stats.size", source.length()),
                formatMetric("stats.compare", stats.metric("comparisons")),
                I18N.text("stats.frames", visualFrameCount()));
    }

    @Override
    protected void onResetData() {
        clearStringSelection();
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

    @Override
    public String selectedAlgorithmId() {
        int index;
        if (algorithmSelector == null) {
            index = 0;
        } else {
            index = algorithmSelector.getSelectionModel().getSelectedIndex();
        }
        if (index < 0) {
            index = 0;
        }
        if (algorithmIds.isEmpty()) {
            return null;
        } else {
            return algorithmIds.get(Math.min(index, algorithmIds.size() - 1));
        }
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
        algorithmSelector.getSelectionModel().selectedIndexProperty().addListener((observable, previous, current) -> {
            refreshAlgorithmControls();
            notifyAlgorithmSelection();
        });
        Platform.runLater(() -> {
            structureSelector.getSelectionModel().selectFirst();
            algorithmSelector.getSelectionModel().selectFirst();
            refreshAlgorithmControls();
            notifyAlgorithmSelection();
        });
    }

    private void notifyAlgorithmSelection() {
        algorithmSelectionListener.accept(selectedAlgorithmId());
    }

    private void refreshAlgorithmControls() {
        String algorithmId = selectedAlgorithmId();
        boolean search = algorithmId != null && AlgorithmCatalog.stringSearches().contains(algorithmId);
        if (patternField != null) {
            patternField.setManaged(search);
            patternField.setVisible(search);
        }
        if (searchSectionLabel != null) {
            searchSectionLabel.textProperty().unbind();
            String key;
            if (search) {
                key = "label.string.search";
            } else {
                key = "label.string.algorithm";
            }
            searchSectionLabel.textProperty().bind(I18N.createStringBinding(key));
        }
        if (!search) {
            stringVisualizer().clearAlgorithmPattern();
        } else if (!structureSelectionEnabled && patternField != null) {
            stringVisualizer().setAlgorithmPattern(patternField.getText());
        }
    }

    private void renderSource() {
        renderStructureState(StringViewState.source(source.value()));
        if (valueField != null && !valueField.isFocused()) valueField.setText(source.value());
        refreshStatsDisplay();
    }

    /** Projects the latest factual String StructureEvent into Structure presentation state. */
    private void renderLatestStructureMutation() {
        renderStructureState(new StringViewState(source.value(), latestStringMutation(),
                StringViewState.Observation.none(), 0, false));
        if (valueField != null && !valueField.isFocused()) valueField.setText(source.value());
        refreshStatsDisplay();
    }

    private StringViewState.Mutation latestStringMutation() {
        List<com.majortom.algorithms.core.runtime.EventEnvelope> events = structureEvents();
        for (int index = events.size() - 1; index >= 0; index--) {
            Object event = events.get(index).event();
            if (event instanceof StringStructureEvent.Inserted inserted) {
                return StringViewState.Mutation.inserted(inserted.index(), inserted.value().length());
            }
            if (event instanceof StringStructureEvent.Removed removed) {
                return StringViewState.Mutation.removed(removed.index(), removed.value().length());
            }
            if (event instanceof StringStructureEvent.Updated updated) {
                return StringViewState.Mutation.updated(updated.index());
            }
            if (event instanceof StringStructureEvent.Replaced replaced) {
                return StringViewState.Mutation.replaced(replaced.index(), replaced.value().length());
            }
        }
        return StringViewState.Mutation.none();
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
            clearStringSelection();
        }
        structureSelectionEnabled = enabled;
        if (enabled) {
            stringVisualizer().clearAlgorithmPattern();
            return;
        }
        String algorithmId = selectedAlgorithmId();
        boolean search = algorithmId != null && AlgorithmCatalog.stringSearches().contains(algorithmId);
        if (search && patternField != null) {
            stringVisualizer().setAlgorithmPattern(patternField.getText());
        } else {
            stringVisualizer().clearAlgorithmPattern();
        }
    }

    private void handleStringSelection(int index) {
        if (structureSelectionEnabled) {
            handleStructureStringSelection(index);
            return;
        }
        StringViewState state = latestViewState();
        if (state == null || index < 0 || index >= state.value().length()) {
            return;
        }
        algorithmSelectedIndex = index;
        selectionListener.accept(new IndexSelection(index, state.value().charAt(index), state.value().length()));
    }

    @Override
    protected void onPresentationStateChanged(StringViewState state) {
        if (structureSelectionEnabled || algorithmSelectedIndex < 0) {
            return;
        }
        if (algorithmSelectedIndex >= state.value().length()) {
            clearStringSelection();
            return;
        }
        stringVisualizer().showSelection(algorithmSelectedIndex);
        selectionListener.accept(new IndexSelection(
                algorithmSelectedIndex,
                state.value().charAt(algorithmSelectedIndex),
                state.value().length()));
    }

    private void handleStructureStringSelection(int index) {
        if (index < 0 || index >= source.length()) {
            return;
        }
        char value = source.charAt(index);
        if (indexField != null) indexField.setText(Integer.toString(index));
        if (characterField != null) characterField.setText(Character.toString(value));
        if (lengthField != null) lengthField.setText("1");
        selectionListener.accept(new IndexSelection(index, value, source.length()));
    }

    private void clearStringSelection() {
        algorithmSelectedIndex = -1;
        stringVisualizer().clearSelection();
        selectionListener.accept(null);
    }

    private StringVisualizer stringVisualizer() {
        return (StringVisualizer) visualizer;
    }

    public record IndexSelection(int index, char value, int length) {
    }

    private Integer parseIndex(TextField field, boolean allowEnd) {
        try {
            int index = Integer.parseInt(field.getText().trim());
            int max;
            if (allowEnd) {
                max = source.length();
            } else {
                max = source.length() - 1;
            }
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
            if (value > 0) {
                return value;
            } else {
                return null;
            }
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void bindButton(Button button, String key) {
        if (button != null) button.textProperty().bind(I18N.createStringBinding(key));
    }
}
