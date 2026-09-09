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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/** Linked-list workbench backed by factual node/link events and the family-specific linked visualizer. */
public final class LinkedListController extends BaseModuleController<LinkedListViewState>
        implements StructureSnapshotSupport<SequenceSnapshot<Integer>> {
    private static final String MODULE_ID = "linked-list";

    private final LinkedStructure<Integer> linkedList;
    private boolean structureSelectionEnabled = true;
    private Long algorithmSelectedNodeId;
    private Consumer<NodeSelection> selectionListener = ignored -> { };

    @FXML private Label typeLabel;
    @FXML private Label structureLabel;
    @FXML private ComboBox<String> structureSelector;
    @FXML private Label operationsLabel;
    @FXML private TextField valueField;
    @FXML private TextField indexField;
    @FXML private Button primaryBtn;
    @FXML private Button secondaryBtn;
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
        quaternaryBtn.setOnAction(event -> update());
        linkedVisualizer().setSelectionListener(this::handleVisualSelection);
    }

    @FXML
    private void handlePrimary() {
        insert();
    }

    @FXML
    private void handleSecondary() {
        remove();
    }

    private void insert() {
        clearVisualSelection();
        Integer value = value();
        if (value == null) {
            return;
        }
        Integer parsedIndex = index(true);
        if (parsedIndex == null) {
            return;
        }
        int target;
        if (indexField.getText().isBlank()) {
            target = linkedList.size();
        } else {
            target = parsedIndex;
        }
        if (target < 0 || target > linkedList.size()) {
            logI18n("message.error.invalid_linear_index");
            return;
        }
        if (executeAndReduce("insert", () -> linkedList.insert(target, value))) {
            selectLinkedAtIndex(target);
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
            selectLinkedAfterRemoval(index);
            logI18n("message.linear.removed", removed[0], index);
        }
    }

    private void selectLinkedAtIndex(int index) {
        LinkedListViewState state = latestStructureState();
        if (state == null) {
            state = currentState();
        }
        List<Long> order = orderedNodeIds(state);
        if (index < 0 || index >= order.size()) {
            clearVisualSelection();
            return;
        }
        linkedVisualizer().selectNode(order.get(index));
    }

    private void selectLinkedAfterRemoval(int removedIndex) {
        LinkedListViewState state = latestStructureState();
        if (state == null || state.nodes().isEmpty()) {
            clearVisualSelection();
            indexField.clear();
            valueField.clear();
            return;
        }
        List<Long> order = orderedNodeIds(state);
        if (order.isEmpty()) {
            clearVisualSelection();
            indexField.clear();
            valueField.clear();
            return;
        }
        int nextIndex = removedIndex;
        if (nextIndex >= order.size()) {
            nextIndex = order.size() - 1;
        }
        linkedVisualizer().selectNode(order.get(nextIndex));
    }

    private void update() {
        clearVisualSelection();
        Integer target = index(false);
        Integer value = value();
        if (target == null || value == null || target < 0 || target >= linkedList.size()) {
            logI18n("message.error.invalid_linear_index");
            return;
        }
        int index = target;
        int[] previous = new int[1];
        if (executeAndReduce("update", () -> previous[0] = linkedList.set(index, value))) {
            selectLinkedAtIndex(index);
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

    @Override
    protected boolean supportsDataTools() {
        return true;
    }

    @Override
    protected void applyBulkData(String input) {
        List<Integer> values = parseIntegerBatchInput(input);
        if (values == null) {
            return;
        }
        replaceValues(values, "bulk-replace", "message.data.bulk_applied");
    }

    @Override
    protected void randomizeData() {
        java.util.Random random = new java.util.Random();
        List<Integer> values = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            values.add(random.nextInt(100) + 1);
        }
        replaceValues(List.copyOf(values), "randomize", "message.data.randomized");
    }

    private void replaceValues(List<Integer> values, String operationId, String messageKey) {
        clearVisualSelection();
        if (!executeStructureOperation(operationId, () -> {
            clearWithoutRuntime();
            for (int index = 0; index < values.size(); index++) {
                linkedList.insert(index, values.get(index));
            }
            return null;
        })) {
            return;
        }
        renderStructureState(currentState());
        if (values.isEmpty()) {
            valueField.clear();
            indexField.clear();
        } else {
            selectLinkedAtIndex(0);
        }
        logI18n(messageKey, values.size());
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
        clearVisualSelection();
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
        clearVisualSelection();
        clearWithoutRuntime();
        for (Integer value : snapshot.state().values()) {
            linkedList.insert(linkedList.size(), value);
        }
        renderStructureState(currentState());
    }

    @Override
    public void previewStructureSnapshot(StructureSnapshot<SequenceSnapshot<Integer>> snapshot) {
        if (!MODULE_ID.equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        clearVisualSelection();
        renderPreviewState(LinkedListViewState.fromValues(snapshot.state().values()));
    }

    @Override
    public String describeStructureSnapshot(SequenceSnapshot<Integer> state) {
        return I18N.text("snapshot.linear.detail", state.values().size());
    }

    @Override
    public String snapshotPrimaryCount(SequenceSnapshot<Integer> state) {
        return Integer.toString(state.values().size());
    }

    public void setSelectionListener(Consumer<NodeSelection> listener) {
        if (listener == null) {
            selectionListener = ignored -> { };
        } else {
            selectionListener = listener;
        }
    }

    public void setStructureSelectionEnabled(boolean enabled) {
        if (structureSelectionEnabled != enabled) {
            clearVisualSelection();
        }
        structureSelectionEnabled = enabled;
    }

    private void handleVisualSelection(long nodeId) {
        if (nodeId <= 0L) {
            clearVisualSelection();
            return;
        }
        if (!structureSelectionEnabled) {
            handleAlgorithmSelection(nodeId);
            return;
        }
        LinkedListViewState state = latestStructureState();
        if (state == null) {
            state = currentState();
        }
        LinkedListViewState.Node node = state.nodes().get(nodeId);
        if (node == null) {
            clearVisualSelection();
            return;
        }
        List<Long> order = orderedNodeIds(state);
        int index = order.indexOf(nodeId);
        valueField.setText(Integer.toString(node.value()));
        if (index >= 0) {
            indexField.setText(Integer.toString(index));
        }
        selectionListener.accept(new NodeSelection(
                node.id(), node.value(), node.previousId(), node.nextId(), index, state.nodes().size()));
    }

    private void handleAlgorithmSelection(long nodeId) {
        LinkedListViewState state = latestViewState();
        if (state == null) {
            return;
        }
        algorithmSelectedNodeId = nodeId;
        if (!publishAlgorithmSelection(state, nodeId)) {
            clearVisualSelection();
        }
    }

    private boolean publishAlgorithmSelection(LinkedListViewState state, long nodeId) {
        LinkedListViewState.Node node = state.nodes().get(nodeId);
        if (node == null) {
            return false;
        }
        int index = orderedNodeIds(state).indexOf(nodeId);
        selectionListener.accept(new NodeSelection(
                node.id(), node.value(), node.previousId(), node.nextId(), index, state.nodes().size()));
        return true;
    }

    @Override
    protected void onPresentationStateChanged(LinkedListViewState state) {
        if (structureSelectionEnabled || algorithmSelectedNodeId == null) {
            return;
        }
        long nodeId = algorithmSelectedNodeId;
        if (!linkedVisualizer().showSelection(nodeId) || !publishAlgorithmSelection(state, nodeId)) {
            clearVisualSelection();
        }
    }

    private List<Long> orderedNodeIds(LinkedListViewState state) {
        List<Long> order = new ArrayList<>();
        Long current = state.nodes().values().stream()
                .filter(candidate -> candidate.previousId() == null)
                .map(LinkedListViewState.Node::id)
                .findFirst()
                .orElse(null);
        while (current != null && state.nodes().containsKey(current) && !order.contains(current)) {
            order.add(current);
            current = state.nodes().get(current).nextId();
        }
        return List.copyOf(order);
    }

    private void clearVisualSelection() {
        algorithmSelectedNodeId = null;
        linkedVisualizer().clearSelection();
        selectionListener.accept(null);
    }

    private LinkedListVisualizer linkedVisualizer() {
        return (LinkedListVisualizer) visualizer;
    }

    public record NodeSelection(long id, int value, Long previousId, Long nextId, int index, int size) {
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
        typeLabel.setText(I18N.text("label.linear.feature.linked_list"));
        structureLabel.setText(I18N.text("label.common.structure"));
        structureSelector.setItems(javafx.collections.FXCollections.observableArrayList(
                I18N.text("label.linear.structure.linked_list")));
        structureSelector.getSelectionModel().selectFirst();
        operationsLabel.setText(I18N.text("label.linear.operations"));
        valueField.setPromptText(I18N.text("prompt.linear.value"));
        indexField.setPromptText(I18N.text("prompt.linear.index"));
        primaryBtn.setText(I18N.text("action.linked_list.insert"));
        secondaryBtn.setText(I18N.text("action.linked_list.remove"));
        quaternaryBtn.setText(I18N.text("action.linked_list.update"));
        indexField.setVisible(true);
        indexField.setManaged(true);
        quaternaryBtn.setVisible(true);
        quaternaryBtn.setManaged(true);
    }
}
