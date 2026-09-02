package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.SequenceSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.basic.node.ListNode;
import com.majortom.algorithms.library.structure.LinkedQueue;
import com.majortom.algorithms.library.structure.LinkedStack;
import com.majortom.algorithms.library.structure.LinkedStructure;
import com.majortom.algorithms.library.structure.QueueStructure;
import com.majortom.algorithms.library.structure.StackStructure;
import com.majortom.algorithms.visualization.impl.visualizer.LinearStructureVisualizer;
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

public final class LinearStructureController extends BaseModuleController<LinearStructureViewState>
        implements StructureSnapshotSupport<SequenceSnapshot<Integer>> {

    private enum Kind { LINKED_LIST, STACK, QUEUE }

    private final Kind kind;
    private final String moduleId;
    private final LinkedStructure<Integer> linkedList;
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

    private LinearStructureController(Kind kind, String moduleId) {
        super(new LinearStructureVisualizer(), "/fxml/LinearStructureControls.fxml");
        this.kind = kind;
        this.moduleId = moduleId;
        this.linkedList = kind == Kind.LINKED_LIST ? module("structure.linked-list.Integer", LinkedList.class) : null;
        this.stack = kind == Kind.STACK ? module("structure.stack.Integer", LinkedStack.class) : null;
        this.queue = kind == Kind.QUEUE ? module("structure.queue.Integer", LinkedQueue.class) : null;
        seed();
        renderStructureState(currentState());
    }

    public static LinearStructureController linkedList() { return new LinearStructureController(Kind.LINKED_LIST, "linked-list"); }
    public static LinearStructureController stack() { return new LinearStructureController(Kind.STACK, "stack"); }
    public static LinearStructureController queue() { return new LinearStructureController(Kind.QUEUE, "queue"); }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        configureControls();
    }

    @FXML private void handlePrimary() {
        switch (kind) {
            case LINKED_LIST -> insert();
            case STACK -> push();
            case QUEUE -> enqueue();
        }
    }

    @FXML private void handleSecondary() {
        switch (kind) {
            case LINKED_LIST -> remove();
            case STACK -> pop();
            case QUEUE -> dequeue();
        }
    }

    @FXML private void handleTertiary() {
        switch (kind) {
            case LINKED_LIST -> get();
            case STACK -> peek();
            case QUEUE -> front();
        }
    }

    @FXML private void handleQuaternary() {
        if (kind == Kind.LINKED_LIST) update();
        else if (kind == Kind.QUEUE) rear();
    }

    private void insert() {
        Integer value = value();
        if (value == null) return;
        Integer parsedIndex = index(true);
        if (parsedIndex == null) return;
        int target = indexField.getText().isBlank() ? linkedList.size() : parsedIndex;
        if (target < 0 || target > linkedList.size()) { logI18n("message.error.invalid_linear_index"); return; }
        if (executeStructureOperation("insert", () -> { linkedList.insert(target, value); return null; })) {
            renderStructureState(currentState());
            logI18n("message.linear.inserted", value, target);
        }
    }

    private void remove() {
        Integer target = index(false);
        if (target == null || target < 0 || target >= linkedList.size()) { logI18n("message.error.invalid_linear_index"); return; }
        final int index = target;
        final int[] removed = new int[1];
        if (executeStructureOperation("remove", () -> { removed[0] = linkedList.remove(index); return null; })) {
            renderStructureState(currentState());
            logI18n("message.linear.removed", removed[0], index);
        }
    }

    private void get() {
        Integer target = index(false);
        if (target == null || target < 0 || target >= linkedList.size()) { logI18n("message.error.invalid_linear_index"); return; }
        logI18n("message.linear.value_at", target, linkedList.get(target));
    }

    private void update() {
        Integer target = index(false);
        Integer value = value();
        if (target == null || value == null || target < 0 || target >= linkedList.size()) { logI18n("message.error.invalid_linear_index"); return; }
        final int index = target;
        final int[] previous = new int[1];
        if (executeStructureOperation("update", () -> { previous[0] = linkedList.update(index, value); return null; })) {
            renderStructureState(currentState());
            logI18n("message.linear.updated", index, previous[0], value);
        }
    }

    private void push() {
        Integer value = value();
        if (value == null) return;
        if (executeStructureOperation("push", () -> { stack.push(value); return null; })) {
            renderStructureState(currentState());
            logI18n("message.stack.pushed", value);
        }
    }

    private void pop() {
        if (stack.isEmpty()) { logI18n("message.linear.empty"); return; }
        final int[] value = new int[1];
        if (executeStructureOperation("pop", () -> { value[0] = stack.pop(); return null; })) {
            renderStructureState(currentState());
            logI18n("message.stack.popped", value[0]);
        }
    }

    private void peek() {
        if (stack.isEmpty()) { logI18n("message.linear.empty"); return; }
        logI18n("message.stack.peek", stack.peek());
    }

    private void enqueue() {
        Integer value = value();
        if (value == null) return;
        if (executeStructureOperation("enqueue", () -> { queue.enqueue(value); return null; })) {
            renderStructureState(currentState());
            logI18n("message.queue.enqueued", value);
        }
    }

    private void dequeue() {
        if (queue.isEmpty()) { logI18n("message.linear.empty"); return; }
        final int[] value = new int[1];
        if (executeStructureOperation("dequeue", () -> { value[0] = queue.dequeue(); return null; })) {
            renderStructureState(currentState());
            logI18n("message.queue.dequeued", value[0]);
        }
    }

    private void front() {
        if (queue.isEmpty()) { logI18n("message.linear.empty"); return; }
        logI18n("message.queue.front", queue.front());
    }

    private void rear() {
        if (queue.isEmpty()) { logI18n("message.linear.empty"); return; }
        logI18n("message.queue.rear", queue.rear());
    }

    private Integer value() {
        try { return Integer.valueOf(valueField.getText().trim()); }
        catch (RuntimeException exception) { logI18n("message.error.invalid_linear_value"); return null; }
    }

    private Integer index(boolean optional) {
        String text = indexField.getText().trim();
        if (optional && text.isEmpty()) return 0;
        try { return Integer.valueOf(text); }
        catch (RuntimeException exception) { logI18n("message.error.invalid_linear_index"); return null; }
    }

    private void seed() {
        switch (kind) {
            case LINKED_LIST -> { linkedList.insert(0, 12); linkedList.insert(1, 24); linkedList.insert(2, 36); }
            case STACK -> { stack.push(12); stack.push(24); stack.push(36); }
            case QUEUE -> { queue.enqueue(12); queue.enqueue(24); queue.enqueue(36); }
        }
    }

    private LinearStructureViewState currentState() { return new LinearStructureViewState(moduleId, values()); }

    private List<Integer> values() {
        List<Integer> values = new ArrayList<>();
        ListNode<Integer> node = switch (kind) {
            case LINKED_LIST -> linkedList.raw();
            case STACK -> stack.raw();
            case QUEUE -> queue.raw();
        };
        while (node != null) { values.add(node.data); node = node.next; }
        return List.copyOf(values);
    }

    @Override protected String moduleId() { return moduleId; }
    @Override protected String formatStatsMessage() { return I18N.text("stats.linear.size", values().size()); }
    @Override protected void setupI18n() { if (typeLabel != null) configureControls(); }
    @Override public void handleAlgorithmStart() { logI18n("message.linear.no_algorithm"); }

    @Override
    protected void onResetData() {
        switch (kind) {
            case LINKED_LIST -> { while (!linkedList.isEmpty()) linkedList.remove(linkedList.size() - 1); }
            case STACK -> { while (!stack.isEmpty()) stack.pop(); }
            case QUEUE -> { while (!queue.isEmpty()) queue.dequeue(); }
        }
        seed();
        renderStructureState(currentState());
    }

    @Override
    public StructureSnapshot<SequenceSnapshot<Integer>> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId, new SequenceSnapshot<>(values()));
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<SequenceSnapshot<Integer>> snapshot) {
        if (!moduleId.equals(snapshot.moduleId())) throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        clearWithoutRuntime();
        List<Integer> snapshotValues = snapshot.state().values();
        if (kind == Kind.STACK) {
            for (int index = snapshotValues.size() - 1; index >= 0; index--) stack.push(snapshotValues.get(index));
        } else {
            for (Integer value : snapshotValues) {
                if (kind == Kind.LINKED_LIST) linkedList.insert(linkedList.size(), value);
                else queue.enqueue(value);
            }
        }
        renderStructureState(currentState());
    }

    @Override public String describeStructureSnapshot(SequenceSnapshot<Integer> state) { return I18N.text("snapshot.linear.detail", state.values().size()); }

    private void clearWithoutRuntime() {
        switch (kind) {
            case LINKED_LIST -> { while (!linkedList.isEmpty()) linkedList.remove(linkedList.size() - 1); }
            case STACK -> { while (!stack.isEmpty()) stack.pop(); }
            case QUEUE -> { while (!queue.isEmpty()) queue.dequeue(); }
        }
    }

    private void configureControls() {
        if (typeLabel == null) return;
        operationsLabel.setText(I18N.text("label.linear.operations"));
        valueField.setPromptText(I18N.text("prompt.linear.value"));
        indexField.setPromptText(I18N.text("prompt.linear.index"));
        switch (kind) {
            case LINKED_LIST -> {
                typeLabel.setText(I18N.text("label.linear.linked_list"));
                primaryBtn.setText(I18N.text("action.linked_list.insert"));
                secondaryBtn.setText(I18N.text("action.linked_list.remove"));
                tertiaryBtn.setText(I18N.text("action.linked_list.get"));
                quaternaryBtn.setText(I18N.text("action.linked_list.update"));
                indexField.setVisible(true); indexField.setManaged(true); quaternaryBtn.setVisible(true); quaternaryBtn.setManaged(true);
            }
            case STACK -> {
                typeLabel.setText(I18N.text("label.linear.stack"));
                primaryBtn.setText(I18N.text("action.stack.push"));
                secondaryBtn.setText(I18N.text("action.stack.pop"));
                tertiaryBtn.setText(I18N.text("action.stack.peek"));
                indexField.setVisible(false); indexField.setManaged(false); quaternaryBtn.setVisible(false); quaternaryBtn.setManaged(false);
            }
            case QUEUE -> {
                typeLabel.setText(I18N.text("label.linear.queue"));
                primaryBtn.setText(I18N.text("action.queue.enqueue"));
                secondaryBtn.setText(I18N.text("action.queue.dequeue"));
                tertiaryBtn.setText(I18N.text("action.queue.front"));
                quaternaryBtn.setText(I18N.text("action.queue.rear"));
                indexField.setVisible(false); indexField.setManaged(false); quaternaryBtn.setVisible(true); quaternaryBtn.setManaged(true);
            }
        }
    }
}
