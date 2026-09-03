package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.SequenceSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.structure.QueueStructure;
import com.majortom.algorithms.library.structure.StackStructure;
import com.majortom.algorithms.visualization.impl.visualizer.StackQueueVisualizer;
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

/** Stack/Queue controller. LinkedList has its own factual visualizer in Phase 7. */
public final class LinearStructureController extends BaseModuleController<LinearStructureViewState>
        implements StructureSnapshotSupport<SequenceSnapshot<Integer>> {

    private enum Kind { STACK, QUEUE }

    private final Kind kind;
    private final String moduleId;
    private final StackStructure<Integer> stack;
    private final QueueStructure<Integer> queue;

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
        super(new StackQueueVisualizer(), "/fxml/LinearStructureControls.fxml");
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
        Integer value = value();
        if (value == null) {
            return;
        }
        if (executeStructureOperation("push", () -> {
            stack.push(value);
            return null;
        })) {
            renderStructureState(currentState());
            logI18n("message.stack.pushed", value);
        }
    }

    private void pop() {
        if (stack.isEmpty()) {
            logI18n("message.linear.empty");
            return;
        }
        int[] value = new int[1];
        if (executeStructureOperation("pop", () -> {
            value[0] = stack.pop();
            return null;
        })) {
            renderStructureState(currentState());
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
        Integer value = value();
        if (value == null) {
            return;
        }
        if (executeStructureOperation("enqueue", () -> {
            queue.enqueue(value);
            return null;
        })) {
            renderStructureState(currentState());
            logI18n("message.queue.enqueued", value);
        }
    }

    private void dequeue() {
        if (queue.isEmpty()) {
            logI18n("message.linear.empty");
            return;
        }
        int[] value = new int[1];
        if (executeStructureOperation("dequeue", () -> {
            value[0] = queue.dequeue();
            return null;
        })) {
            renderStructureState(currentState());
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
