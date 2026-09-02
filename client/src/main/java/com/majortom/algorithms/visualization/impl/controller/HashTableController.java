package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.HashTableSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.library.structure.HashEntry;
import com.majortom.algorithms.library.structure.MutableHashTable;
import com.majortom.algorithms.visualization.impl.visualizer.HashTableVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public final class HashTableController extends BaseModuleController<HashTableViewState>
        implements StructureSnapshotSupport<HashTableSnapshot<String, Integer>> {

    private MutableHashTable<String, Integer> table;

    @FXML private Label structureLabel;
    @FXML private ComboBox<String> structureSelector;
    @FXML private Label operationsLabel;
    @FXML private TextField keyField;
    @FXML private TextField valueField;
    @FXML private Button putBtn;
    @FXML private Button removeBtn;
    @FXML private Button findBtn;

    @SuppressWarnings("unchecked")
    public HashTableController() {
        super(new HashTableVisualizer(), "/fxml/HashTableControls.fxml");
        table = module("structure.hash-table.String.Integer", MutableHashTable.class);
        table.put("Aa", 10);
        table.put("BB", 20);
        table.put("alpha", 30);
        renderTable(null, false);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> FXCollections.observableArrayList(I18N.text("label.structure.hash_table")), I18N.localeProperty()));
        Platform.runLater(() -> structureSelector.getSelectionModel().selectFirst());
        renderTable(null, false);
    }

    @FXML
    private void handlePut() {
        String key = keyField.getText().trim();
        Integer value = parseValue();
        if (key.isEmpty() || value == null) {
            logI18n("message.hash.invalid_entry");
            return;
        }
        if (executeStructureOperation("put", () -> {
            table.put(key, value);
            return null;
        })) {
            renderTable(key, true);
            logI18n("message.hash.put", key, value);
        }
    }

    @FXML
    private void handleRemove() {
        String key = keyField.getText().trim();
        if (key.isEmpty()) return;
        if (!table.containsKey(key)) {
            renderTable(key, false);
            logI18n("message.hash.not_found", key);
            return;
        }
        Integer removed = table.get(key);
        if (executeStructureOperation("remove", () -> {
            table.remove(key);
            return null;
        })) {
            renderTable(key, false);
            logI18n("message.hash.removed", key, removed);
        }
    }

    @FXML
    private void handleFind() {
        String key = keyField.getText().trim();
        boolean found = !key.isEmpty() && table.containsKey(key);
        renderTable(key, found);
        if (found) logI18n("message.hash.found", key, table.get(key));
        else logI18n("message.hash.not_found", key);
    }

    @Override
    public StructureSnapshot<HashTableSnapshot<String, Integer>> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), snapshot());
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<HashTableSnapshot<String, Integer>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        MutableHashTable<String, Integer> restored = new MutableHashTable<>(snapshot.state().capacity());
        for (HashTableSnapshot.Entry<String, Integer> entry : snapshot.state().entries()) restored.put(entry.key(), entry.value());
        table = restored;
        invalidateExecutionForInputChange();
        renderTable(null, false);
    }

    @Override
    public String describeStructureSnapshot(HashTableSnapshot<String, Integer> state) {
        return I18N.text("snapshot.hash.detail", state.entries().size(), state.capacity());
    }

    @Override
    public void handleAlgorithmStart() {
        // HashTable is currently a structure-only workbench.
    }

    @Override
    protected String formatStatsMessage() {
        return I18N.text("stats.hash", table.size(), table.capacity());
    }

    @Override
    protected void onResetData() {
        table = new MutableHashTable<>();
        table.put("Aa", 10);
        table.put("BB", 20);
        table.put("alpha", 30);
        renderTable(null, false);
    }

    @Override
    protected void setupI18n() {
        if (structureLabel != null) structureLabel.textProperty().bind(I18N.createStringBinding("label.common.structure"));
        if (operationsLabel != null) operationsLabel.textProperty().bind(I18N.createStringBinding("label.hash.operations"));
        if (keyField != null) keyField.promptTextProperty().bind(I18N.createStringBinding("prompt.hash.key"));
        if (valueField != null) valueField.promptTextProperty().bind(I18N.createStringBinding("prompt.hash.value"));
        bindButton(putBtn, "action.hash.put");
        bindButton(removeBtn, "action.hash.remove");
        bindButton(findBtn, "action.hash.find");
    }

    @Override
    protected String moduleId() { return "hash-table"; }

    private HashTableSnapshot<String, Integer> snapshot() {
        List<HashTableSnapshot.Entry<String, Integer>> entries = new ArrayList<>();
        List<HashEntry<String, Integer>> buckets = table.raw();
        for (int bucketIndex = 0; bucketIndex < buckets.size(); bucketIndex++) {
            HashEntry<String, Integer> current = buckets.get(bucketIndex);
            while (current != null) {
                entries.add(new HashTableSnapshot.Entry<>(bucketIndex, current.key(), current.value()));
                current = current.next();
            }
        }
        return new HashTableSnapshot<>(table.capacity(), entries);
    }

    private void renderTable(String focusKey, boolean found) {
        renderStructureState(new HashTableViewState(snapshot(), focusKey, found));
        refreshStatsDisplay();
    }

    private Integer parseValue() {
        try { return Integer.valueOf(valueField.getText().trim()); }
        catch (RuntimeException exception) { return null; }
    }

    private void bindButton(Button button, String key) {
        if (button != null) button.textProperty().bind(I18N.createStringBinding(key));
    }
}
