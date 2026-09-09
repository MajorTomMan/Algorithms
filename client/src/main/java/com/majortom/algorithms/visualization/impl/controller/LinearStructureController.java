package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.SequenceSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.structure.QueueStructure;
import com.majortom.algorithms.library.structure.StackStructure;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.QueueVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.StackVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
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

/** Stack/Queue controller. LinkedList has its own factual visualizer in Phase 7. */
public final class LinearStructureController extends BaseModuleController<LinearStructureViewState>
        implements StructureSnapshotSupport<SequenceSnapshot<Integer>> {

    private enum Kind { STACK, QUEUE }

    private final Kind kind;
    private final String moduleId;
    private final StackStructure<Integer> stack;
    private final QueueStructure<Integer> queue;
    private boolean structureSelectionEnabled = true;
    private int algorithmSelectedIndex = -1;
    private Consumer<ItemSelection> selectionListener = ignored -> { };

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
    private LinearStructureController(Kind kind, String moduleId) {
        super(visualizer(kind), "/fxml/LinearStructureControls.fxml");
        this.kind = kind;
        this.moduleId = moduleId;
        if (kind == Kind.STACK) {
            stack = (StackStructure<Integer>) module("structure.stack.Integer", LinkedList.class);
        } else {
            stack = null;
        }
        if (kind == Kind.QUEUE) {
            queue = (QueueStructure<Integer>) module("structure.queue.Integer", LinkedList.class);
        } else {
            queue = null;
        }
        seed();
        renderStructureState(currentState());
    }

    private static BaseVisualizer<LinearStructureViewState> visualizer(Kind kind) {
        if (kind == Kind.STACK) {
            return new StackVisualizer();
        } else {
            return new QueueVisualizer();
        }
    }

    public static LinearStructureController stack() {
        return new LinearStructureController(Kind.STACK, "stack");
    }

    public static LinearStructureController queue() {
        return new LinearStructureController(Kind.QUEUE, "queue");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        configureControls();
        if (kind == Kind.STACK) stackVisualizer().setSelectionListener(this::handleVisualSelection);
        else queueVisualizer().setSelectionListener(this::handleVisualSelection);
    }

    @FXML
    private void handlePrimary() {
        if (kind == Kind.STACK) {
            push();
        } else {
            enqueue();
        }
    }

    @FXML
    private void handleSecondary() {
        if (kind == Kind.STACK) {
            pop();
        } else {
            dequeue();
        }
    }

    private void push() {
        clearVisualSelection();
        Integer value = value();
        if (value == null) {
            return;
        }
        if (executeStructureOperation("push", () -> {
            stack.push(value);
            return null;
        })) {
            renderMutation(LinearStructureViewState.Type.PUSH, value);
            stackVisualizer().selectIndex(0);
            logI18n("message.stack.pushed", value);
        }
    }

    private void pop() {
        clearVisualSelection();
        if (stack.isEmpty()) {
            logI18n("message.linear.empty");
            return;
        }
        int[] value = new int[1];
        if (executeStructureOperation("pop", () -> {
            value[0] = stack.pop();
            return null;
        })) {
            renderMutation(LinearStructureViewState.Type.POP, value[0]);
            selectFirstAfterRemoval();
            logI18n("message.stack.popped", value[0]);
        }
    }

    private void enqueue() {
        clearVisualSelection();
        Integer value = value();
        if (value == null) {
            return;
        }
        if (executeStructureOperation("enqueue", () -> {
            queue.enqueue(value);
            return null;
        })) {
            renderMutation(LinearStructureViewState.Type.ENQUEUE, value);
            queueVisualizer().selectIndex(values().size() - 1);
            logI18n("message.queue.enqueued", value);
        }
    }

    private void dequeue() {
        clearVisualSelection();
        if (queue.isEmpty()) {
            logI18n("message.linear.empty");
            return;
        }
        int[] value = new int[1];
        if (executeStructureOperation("dequeue", () -> {
            value[0] = queue.dequeue();
            return null;
        })) {
            renderMutation(LinearStructureViewState.Type.DEQUEUE, value[0]);
            selectFirstAfterRemoval();
            logI18n("message.queue.dequeued", value[0]);
        }
    }

    private void selectFirstAfterRemoval() {
        if (values().isEmpty()) {
            clearVisualSelection();
            valueField.clear();
            if (indexField != null) {
                indexField.clear();
            }
            return;
        }
        if (kind == Kind.STACK) {
            stackVisualizer().selectIndex(0);
        } else {
            queueVisualizer().selectIndex(0);
        }
    }

    private Integer value() {
        try {
            return Integer.valueOf(valueField.getText().trim());
        } catch (RuntimeException exception) {
            logI18n("message.error.invalid_linear_value");
            return null;
        }
    }

    private void seed() {
        if (kind == Kind.STACK) {
            stack.push(12);
            stack.push(24);
            stack.push(36);
        } else {
            queue.enqueue(12);
            queue.enqueue(24);
            queue.enqueue(36);
        }
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
            if (kind == Kind.STACK) {
                for (int index = values.size() - 1; index >= 0; index--) {
                    stack.push(values.get(index));
                }
            } else {
                for (Integer value : values) {
                    queue.enqueue(value);
                }
            }
            return null;
        })) {
            return;
        }
        renderStructureState(currentState());
        if (values.isEmpty()) {
            valueField.clear();
        } else if (kind == Kind.STACK) {
            stackVisualizer().selectIndex(0);
        } else {
            queueVisualizer().selectIndex(0);
        }
        logI18n(messageKey, values.size());
    }

    private LinearStructureViewState currentState() {
        return new LinearStructureViewState(moduleId, values());
    }

    private void renderMutation(LinearStructureViewState.Type type, Integer value) {
        renderStructureState(new LinearStructureViewState(
                moduleId, values(), LinearStructureViewState.Mutation.of(type, value)));
    }

    private List<Integer> values() {
        List<Integer> values = new ArrayList<>();
        Iterable<Integer> source;
        if (kind == Kind.STACK) {
            source = stack;
        } else {
            source = queue;
        }
        for (Integer value : source) {
            values.add(value);
        }
        return List.copyOf(values);
    }

    @Override
    protected String moduleId() {
        return moduleId;
    }

    @Override
    protected String formatStatsMessage() {
        return I18N.text("stats.linear.size", values().size());
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
        return StructureSnapshot.create(moduleId, new SequenceSnapshot<>(values()));
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<SequenceSnapshot<Integer>> snapshot) {
        if (!moduleId.equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        clearVisualSelection();
        clearWithoutRuntime();
        List<Integer> snapshotValues = snapshot.state().values();
        if (kind == Kind.STACK) {
            for (int index = snapshotValues.size() - 1; index >= 0; index--) {
                stack.push(snapshotValues.get(index));
            }
        } else {
            for (Integer value : snapshotValues) {
                queue.enqueue(value);
            }
        }
        renderStructureState(currentState());
    }

    @Override
    public void previewStructureSnapshot(StructureSnapshot<SequenceSnapshot<Integer>> snapshot) {
        if (!moduleId.equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        clearVisualSelection();
        renderPreviewState(new LinearStructureViewState(moduleId, snapshot.state().values()));
    }

    @Override
    public String describeStructureSnapshot(SequenceSnapshot<Integer> state) {
        return I18N.text("snapshot.linear.detail", state.values().size());
    }

    @Override
    public String snapshotPrimaryCount(SequenceSnapshot<Integer> state) {
        return Integer.toString(state.values().size());
    }

    public void setSelectionListener(Consumer<ItemSelection> listener) {
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

    private void handleVisualSelection(int index) {
        if (!structureSelectionEnabled) {
            handleAlgorithmSelection(index);
            return;
        }
        List<Integer> current = values();
        if (index < 0 || index >= current.size()) {
            clearVisualSelection();
            return;
        }
        int value = current.get(index);
        valueField.setText(Integer.toString(value));
        selectionListener.accept(new ItemSelection(index, value, selectionRole(index, current.size()), current.size()));
    }

    private void handleAlgorithmSelection(int index) {
        LinearStructureViewState state = latestViewState();
        if (state == null || index < 0 || index >= state.values().size()) {
            return;
        }
        algorithmSelectedIndex = index;
        publishAlgorithmSelection(state, index);
    }

    private void publishAlgorithmSelection(LinearStructureViewState state, int index) {
        int size = state.values().size();
        int value = state.values().get(index);
        selectionListener.accept(new ItemSelection(index, value, selectionRole(index, size), size));
    }

    @Override
    protected void onPresentationStateChanged(LinearStructureViewState state) {
        if (structureSelectionEnabled || algorithmSelectedIndex < 0) {
            return;
        }
        if (algorithmSelectedIndex >= state.values().size()) {
            clearVisualSelection();
            return;
        }
        boolean shown;
        if (kind == Kind.STACK) {
            shown = stackVisualizer().showSelection(algorithmSelectedIndex);
        } else {
            shown = queueVisualizer().showSelection(algorithmSelectedIndex);
        }
        if (!shown) {
            clearVisualSelection();
            return;
        }
        publishAlgorithmSelection(state, algorithmSelectedIndex);
    }

    private String selectionRole(int index, int size) {
        if (kind == Kind.STACK) {
            if (index == 0) {
                return "TOP";
            }
            return "ITEM";
        }
        if (size == 1) {
            return "FRONT / REAR";
        }
        if (index == 0) {
            return "FRONT";
        }
        if (index == size - 1) {
            return "REAR";
        }
        return "ITEM";
    }

    private void clearVisualSelection() {
        algorithmSelectedIndex = -1;
        if (kind == Kind.STACK) stackVisualizer().clearSelection();
        else queueVisualizer().clearSelection();
        selectionListener.accept(null);
    }

    private StackVisualizer stackVisualizer() {
        return (StackVisualizer) visualizer;
    }

    private QueueVisualizer queueVisualizer() {
        return (QueueVisualizer) visualizer;
    }

    public record ItemSelection(int index, int value, String role, int size) {
    }

    private void clearWithoutRuntime() {
        if (kind == Kind.STACK) {
            while (!stack.isEmpty()) {
                stack.pop();
            }
        } else {
            while (!queue.isEmpty()) {
                queue.dequeue();
            }
        }
    }

    private void configureControls() {
        if (typeLabel == null) {
            return;
        }
        structureLabel.setText(I18N.text("label.common.structure"));
        structureSelector.setItems(javafx.collections.FXCollections.observableArrayList(
                I18N.text("label.linear.structure.linked_list")));
        structureSelector.getSelectionModel().selectFirst();
        operationsLabel.setText(I18N.text("label.linear.operations"));
        valueField.setPromptText(I18N.text("prompt.linear.value"));
        indexField.setVisible(false);
        indexField.setManaged(false);
        if (kind == Kind.STACK) {
            typeLabel.setText(I18N.text("label.linear.feature.stack"));
            primaryBtn.setText(I18N.text("action.stack.push"));
            secondaryBtn.setText(I18N.text("action.stack.pop"));
        } else {
            typeLabel.setText(I18N.text("label.linear.feature.queue"));
            primaryBtn.setText(I18N.text("action.queue.enqueue"));
            secondaryBtn.setText(I18N.text("action.queue.dequeue"));
        }
        quaternaryBtn.setVisible(false);
        quaternaryBtn.setManaged(false);
    }
}
