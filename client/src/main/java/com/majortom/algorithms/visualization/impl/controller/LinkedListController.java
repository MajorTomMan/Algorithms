package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.snapshot.SequenceSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.structure.LinkedStructure;
import com.majortom.algorithms.visualization.impl.visualizer.linked.LinkedListVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.runtime.Reduction;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListEventReducer;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/** Linked-list workbench backed by factual node/link events and the family-specific linked visualizer. */
public final class LinkedListController extends BaseModuleController<LinkedListViewState>
        implements StructureSnapshotSupport<SequenceSnapshot<Integer>> {
    private static final String MODULE_ID = "linked-list";

    private final LinkedStructure<Integer> linkedList;

    @FXML private Label typeLabel;
    @FXML private Label operationsLabel;
    @FXML private TextField valueField;
    @FXML private TextField indexField;
    @FXML private Button primaryBtn;
    @FXML private Button secondaryBtn;
    @FXML private Button tertiaryBtn;
    @FXML private Button quaternaryBtn;

    @SuppressWarnings("unchecked")
    public LinkedListController() {
        super(new LinkedListVisualizer(), "/fxml/LinearStructureControls.fxml");
        linkedList = (LinkedStructure<Integer>) module("structure.linked-list.Integer", LinkedList.class);
        seed();
        renderStructureState(currentState());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        configureControls();
    }

    @FXML
    private void handlePrimary() {
        insert();
    }

    @FXML
    private void handleSecondary() {
        remove();
    }

    @FXML
    private void handleTertiary() {
        get();
    }

    @FXML
    private void handleQuaternary() {
        update();
    }

    private void insert() {
        Integer value = value();
        if (value == null) {
            return;
        }
        Integer parsedIndex = index(true);
        if (parsedIndex == null) {
            return;
        }
        int target = indexField.getText().isBlank() ? linkedList.size() : parsedIndex;
        if (target < 0 || target > linkedList.size()) {
            logI18n("message.error.invalid_linear_index");
            return;
        }
        if (executeAndReduce("insert", () -> linkedList.insert(target, value))) {
            logI18n("message.linear.inserted", value, target);
        }
    }

    private void remove() {
        Integer target = index(false);
        if (target == null || target < 0 || target >= linkedList.size()) {
            logI18n("message.error.invalid_linear_index");
            return;
        }
        int index = target;
        int[] removed = new int[1];
        if (executeAndReduce("remove", () -> removed[0] = linkedList.remove(index))) {
            logI18n("message.linear.removed", removed[0], index);
        }
    }

    private void get() {
        Integer target = index(false);
        if (target == null || target < 0 || target >= linkedList.size()) {
            logI18n("message.error.invalid_linear_index");
            return;
        }
        logI18n("message.linear.value_at", target, linkedList.get(target));
    }

    private void update() {
        Integer target = index(false);
        Integer value = value();
        if (target == null || value == null || target < 0 || target >= linkedList.size()) {
            logI18n("message.error.invalid_linear_index");
            return;
        }
        int index = target;
        int[] previous = new int[1];
        if (executeAndReduce("update", () -> previous[0] = linkedList.set(index, value))) {
            logI18n("message.linear.updated", index, previous[0], value);
        }
    }

    private boolean executeAndReduce(String operationId, Runnable mutation) {
        int eventStart = structureEvents().size();
        LinkedListViewState before = latestStructureState();
        if (before == null) {
            before = currentState();
        }
        if (!executeStructureOperation(operationId, () -> {
            mutation.run();
            return null;
        })) {
            return false;
        }

        LinkedListEventReducer reducer = new LinkedListEventReducer(before);
        LinkedListViewState state = before;
        List<EventEnvelope> events = structureEvents();
        for (int index = eventStart; index < events.size(); index++) {
            Reduction<LinkedListViewState> reduction = reducer.reduce(state, events.get(index));
            state = reduction.state();
        }
        renderStructureState(state);
        return true;
    }

    private Integer value() {
        try {
            return Integer.valueOf(valueField.getText().trim());
        } catch (RuntimeException exception) {
            logI18n("message.error.invalid_linear_value");
            return null;
        }
    }

    private Integer index(boolean optional) {
        String text = indexField.getText().trim();
        if (optional && text.isEmpty()) {
            return 0;
        }
        try {
            return Integer.valueOf(text);
        } catch (RuntimeException exception) {
            logI18n("message.error.invalid_linear_index");
            return null;
        }
    }

    private void seed() {
        linkedList.insert(0, 12);
        linkedList.insert(1, 24);
        linkedList.insert(2, 36);
    }

    private LinkedListViewState currentState() {
        return LinkedListViewState.source(linkedList.head());
    }

    private List<Integer> values() {
        List<Integer> values = new ArrayList<>();
        for (Integer value : linkedList) {
            values.add(value);
        }
        return List.copyOf(values);
    }

    @Override
    protected String moduleId() {
        return MODULE_ID;
    }

    @Override
    protected String formatStatsMessage() {
        return I18N.text("stats.linear.size", linkedList.size());
    }

    @Override
    protected void setupI18n() {
        if (typeLabel != null) {
            configureControls();
        }
    }

    @Override
    public void handleAlgorithmStart() {
        logI18n("message.linear.no_algorithm");
    }

    @Override
    protected void onResetData() {
        clearWithoutRuntime();
        seed();
        renderStructureState(currentState());
    }

    @Override
    public StructureSnapshot<SequenceSnapshot<Integer>> captureStructureSnapshot() {
        return StructureSnapshot.create(MODULE_ID, new SequenceSnapshot<>(values()));
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<SequenceSnapshot<Integer>> snapshot) {
        if (!MODULE_ID.equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        clearWithoutRuntime();
        for (Integer value : snapshot.state().values()) {
            linkedList.insert(linkedList.size(), value);
        }
        renderStructureState(currentState());
    }

    @Override
    public String describeStructureSnapshot(SequenceSnapshot<Integer> state) {
        return I18N.text("snapshot.linear.detail", state.values().size());
    }

    private void clearWithoutRuntime() {
        while (!linkedList.isEmpty()) {
            linkedList.remove(linkedList.size() - 1);
        }
    }

    private void configureControls() {
        if (typeLabel == null) {
            return;
        }
        typeLabel.setText(I18N.text("label.linear.linked_list"));
        operationsLabel.setText(I18N.text("label.linear.operations"));
        valueField.setPromptText(I18N.text("prompt.linear.value"));
        indexField.setPromptText(I18N.text("prompt.linear.index"));
        primaryBtn.setText(I18N.text("action.linked_list.insert"));
        secondaryBtn.setText(I18N.text("action.linked_list.remove"));
        tertiaryBtn.setText(I18N.text("action.linked_list.get"));
        quaternaryBtn.setText(I18N.text("action.linked_list.update"));
        indexField.setVisible(true);
        indexField.setManaged(true);
        quaternaryBtn.setVisible(true);
        quaternaryBtn.setManaged(true);
    }
}
