package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.SequenceSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.structure.QueueStructure;
import com.majortom.algorithms.library.structure.StackStructure;
import com.majortom.algorithms.visualization.impl.visualizer.QueueVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.StackVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
    private Consumer<ItemSelection> selectionListener = ignored -> { };

    @FXML private Label typeLabel;
    @FXML private Label operationsLabel;
    @FXML private TextField valueField;
    @FXML private TextField indexField;
    @FXML private Button primaryBtn;
    @FXML private Button secondaryBtn;
    @FXML private Button tertiaryBtn;
    @FXML private Button quaternaryBtn;

    @SuppressWarnings("unchecked")
    private LinearStructureController(Kind kind, String moduleId) {
        super(kind == Kind.STACK ? new StackVisualizer() : new QueueVisualizer(), "/fxml/LinearStructureControls.fxml");
        this.kind = kind;
        this.moduleId = moduleId;
        stack = kind == Kind.STACK ? (StackStructure<Integer>) module("structure.stack.Integer", LinkedList.class) : null;
        queue = kind == Kind.QUEUE ? (QueueStructure<Integer>) module("structure.queue.Integer", LinkedList.class) : null;
        seed();
        renderStructureState(currentState());
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

    @FXML
    private void handleTertiary() {
        if (kind == Kind.STACK) {
            peek();
        } else {
            front();
        }
    }

    @FXML
    private void handleQuaternary() {
        if (kind == Kind.QUEUE) {
            rear();
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
            logI18n("message.stack.popped", value[0]);
        }
    }

    private void peek() {
        if (stack.isEmpty()) {
            logI18n("message.linear.empty");
            return;
        }
        logI18n("message.stack.peek", stack.peek());
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
            logI18n("message.queue.dequeued", value[0]);
        }
    }

    private void front() {
        if (queue.isEmpty()) {
            logI18n("message.linear.empty");
            return;
        }
        logI18n("message.queue.front", queue.front());
    }

    private void rear() {
        if (queue.isEmpty()) {
            logI18n("message.linear.empty");
            return;
        }
        logI18n("message.queue.rear", queue.rear());
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

    private LinearStructureViewState currentState() {
        return new LinearStructureViewState(moduleId, values());
    }

    private void renderMutation(LinearStructureViewState.Type type, Integer value) {
        renderStructureState(new LinearStructureViewState(
                moduleId, values(), LinearStructureViewState.Mutation.of(type, value)));
    }

    private List<Integer> values() {
        List<Integer> values = new ArrayList<>();
        Iterable<Integer> source = kind == Kind.STACK ? stack : queue;
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
    public String describeStructureSnapshot(SequenceSnapshot<Integer> state) {
        return I18N.text("snapshot.linear.detail", state.values().size());
    }

    public void setSelectionListener(Consumer<ItemSelection> listener) {
        selectionListener = listener == null ? ignored -> { } : listener;
    }

    private void handleVisualSelection(int index) {
        List<Integer> current = values();
        if (index < 0 || index >= current.size()) {
            clearVisualSelection();
            return;
        }
        int value = current.get(index);
        valueField.setText(Integer.toString(value));
        String role;
        if (kind == Kind.STACK) {
            role = index == 0 ? "TOP" : "ITEM";
        } else if (current.size() == 1) {
            role = "FRONT / REAR";
        } else if (index == 0) {
            role = "FRONT";
        } else if (index == current.size() - 1) {
            role = "REAR";
        } else {
            role = "ITEM";
        }
        selectionListener.accept(new ItemSelection(index, value, role, current.size()));
    }

    private void clearVisualSelection() {
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
        operationsLabel.setText(I18N.text("label.linear.operations"));
        valueField.setPromptText(I18N.text("prompt.linear.value"));
        indexField.setVisible(false);
        indexField.setManaged(false);
        if (kind == Kind.STACK) {
            typeLabel.setText(I18N.text("label.linear.stack"));
            primaryBtn.setText(I18N.text("action.stack.push"));
            secondaryBtn.setText(I18N.text("action.stack.pop"));
            tertiaryBtn.setText(I18N.text("action.stack.peek"));
            quaternaryBtn.setVisible(false);
            quaternaryBtn.setManaged(false);
        } else {
            typeLabel.setText(I18N.text("label.linear.queue"));
            primaryBtn.setText(I18N.text("action.queue.enqueue"));
            secondaryBtn.setText(I18N.text("action.queue.dequeue"));
            tertiaryBtn.setText(I18N.text("action.queue.front"));
            quaternaryBtn.setText(I18N.text("action.queue.rear"));
            quaternaryBtn.setVisible(true);
            quaternaryBtn.setManaged(true);
        }
    }
}
