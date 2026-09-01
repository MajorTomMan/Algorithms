package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.library.tree.AvlCommand;
import com.majortom.algorithms.library.tree.AvlTreeInput;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.impl.visualizer.TreeVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.runtime.tree.AvlTreeEventReducer;
import com.majortom.algorithms.visualization.runtime.tree.AvlTreeViewState;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

public final class TreeController extends BaseModuleController<AvlTreeViewState> {

    private final List<Integer> values = new ArrayList<>();
    private List<AvlCommand> pendingCommands = List.of();

    @FXML private Label structureLabel;
    @FXML private ComboBox<String> structureSelector;
    @FXML private Label algorithmLabel;
    @FXML private Label operationsSectionLabel;
    @FXML private ComboBox<String> algorithmSelector;
    @FXML private TextField valueField;
    @FXML private TextField updateOldValueField;
    @FXML private TextField updateNewValueField;
    @FXML private Button insertBtn;
    @FXML private Button deleteBtn;
    @FXML private Button findBtn;
    @FXML private Button updateBtn;
    @FXML private Button randomBtn;

    public TreeController() {
        super(new TreeVisualizer(), "/fxml/TreeControls.fxml");
        Random random = new Random();
        for (int index = 0; index < 12; index++) {
            int value = random.nextInt(100);
            if (!values.contains(value)) {
                values.add(value);
            }
        }
        runProjectionOnly();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bindSelectors();
        EffectUtils.applyDynamicEffect(insertBtn, deleteBtn, findBtn, updateBtn, randomBtn);
    }

    @FXML
    private void handleInsert() {
        runCommands(AvlCommand.Operation.INSERT, valueField.getText());
    }

    @FXML
    private void handleDelete() {
        runCommands(AvlCommand.Operation.REMOVE, valueField.getText());
    }

    @FXML
    private void handleFind() {
        if (isRunning()) {
            logI18n("message.error.operation_running");
            return;
        }
        Integer target = parseValue(valueField);
        if (target == null) {
            return;
        }
        AvlTreeViewState state = latestViewState();
        SearchResult result = null;
        if (state != null) {
            result = findNode(state.root(), target, null, new ArrayList<>());
        }
        if (result == null) {
            logI18n("message.tree.not_found", target);
            return;
        }
        if (state != null) {
            renderViewState(new AvlTreeViewState(
                    state.root(), state.values(), result.id(), result.value(), result.parentId(), null,
                    new LinkedHashSet<>(result.ancestors()), AvlTreeViewState.Phase.VISITING,
                    null, null, null, false));
        }
        logI18n("message.tree.found", target);
    }

    @FXML
    private void handleUpdate() {
        if (isRunning()) {
            logI18n("message.error.operation_running");
            return;
        }
        Integer oldValue = parseValue(updateOldValueField);
        Integer newValue = parseValue(updateNewValueField);
        if (oldValue == null || newValue == null) {
            return;
        }
        if (!values.contains(oldValue)) {
            logI18n("message.tree.not_found", oldValue);
            return;
        }
        if (oldValue.intValue() != newValue && values.contains(newValue)) {
            logI18n("message.tree.duplicate", newValue);
            return;
        }
        List<AvlCommand> commands = List.of(
                new AvlCommand(AvlCommand.Operation.REMOVE, oldValue),
                new AvlCommand(AvlCommand.Operation.INSERT, newValue));
        pendingCommands = commands;
        dispatchVisualizerAction(
                com.majortom.algorithms.visualization.VisualizationActionType.TREE_DELETE,
                java.util.Map.of("count", commands.size()));
        handleAlgorithmStart();
        logI18n("message.tree.updated", oldValue, newValue);
    }

    @FXML
    private void handleRandom() {
        valueField.setText(String.valueOf(new Random().nextInt(100)));
        dispatchVisualizerAction(com.majortom.algorithms.visualization.VisualizationActionType.TREE_RANDOM);
    }

    private void runCommands(AvlCommand.Operation operation, String input) {
        if (isRunning()) {
            logI18n("message.error.operation_running");
            return;
        }
        List<AvlCommand> commands = new ArrayList<>();
        try {
            for (String part : input.split("[,，]")) {
                if (!part.isBlank()) {
                    commands.add(new AvlCommand(operation, Integer.parseInt(part.trim())));
                }
            }
        } catch (NumberFormatException exception) {
            appendLog(I18N.text("message.error.invalid_tree_input"));
            return;
        }
        if (commands.isEmpty()) {
            appendLog(I18N.text("message.error.invalid_tree_input"));
            return;
        }
        pendingCommands = List.copyOf(commands);
        com.majortom.algorithms.visualization.VisualizationActionType action =
                com.majortom.algorithms.visualization.VisualizationActionType.TREE_INSERT;
        if (operation == AvlCommand.Operation.REMOVE) {
            action = com.majortom.algorithms.visualization.VisualizationActionType.TREE_DELETE;
        }
        dispatchVisualizerAction(action, java.util.Map.of("count", commands.size()));
        handleAlgorithmStart();
    }

    private Integer parseValue(TextField field) {
        try {
            return Integer.valueOf(field.getText().trim());
        } catch (RuntimeException exception) {
            logI18n("message.error.invalid_tree_input");
            return null;
        }
    }

    private SearchResult findNode(
            com.majortom.algorithms.library.tree.AvlNodeSnapshot node,
            int target,
            Long parentId,
            List<Long> path) {
        if (node == null) {
            return null;
        }
        List<Long> nextPath = new ArrayList<>(path);
        nextPath.add(node.id());
        if (target == node.value()) {
            nextPath.removeLast();
            return new SearchResult(node.id(), node.value(), parentId, nextPath);
        }
        if (target < node.value()) {
            return findNode(node.left(), target, node.id(), nextPath);
        }
        return findNode(node.right(), target, node.id(), nextPath);
    }

    private record SearchResult(long id, int value, Long parentId, List<Long> ancestors) {
    }

    @Override
    public void handleAlgorithmStart() {
        if (isRunning()) {
            return;
        }
        List<AvlCommand> commands = pendingCommands;
        pendingCommands = List.of();
        startAlgorithm("tree-avl", new AvlTreeInput(values, commands), AvlTreeEventReducer::new);
    }

    private void runProjectionOnly() {
        startAlgorithm("tree-avl", new AvlTreeInput(values, List.of()), AvlTreeEventReducer::new);
    }

    @Override
    protected void onAlgorithmFinished() {
        AvlTreeViewState state = latestViewState();
        if (state != null) {
            values.clear();
            values.addAll(state.values());
            renderStructureState(state);
        }
        super.onAlgorithmFinished();
        logI18n("message.execution.finished");
    }

    @Override
    protected String formatStatsMessage() {
        AvlTreeViewState state = latestViewState();
        int size = values.size();
        if (state != null) {
            size = state.values().size();
        }
        int height = 0;
        if (state != null && state.root() != null) {
            height = state.root().height();
        }
        return String.format("%s | %s | %s",
                I18N.text("stats.size", size), I18N.text("stats.height", height),
                formatMetric("stats.action", stats.metric("nodes.inserted")
                        + stats.metric("nodes.removed") + stats.metric("rotations")));
    }

    @Override
    protected void onResetData() {
        values.clear();
        pendingCommands = List.of();
        runProjectionOnly();
    }

    @Override
    protected void setupI18n() {
        if (structureLabel != null) {
            structureLabel.textProperty().bind(I18N.createStringBinding("label.common.structure"));
        }
        if (algorithmLabel != null) {
            algorithmLabel.textProperty().bind(I18N.createStringBinding("label.common.algorithm"));
        }
        if (operationsSectionLabel != null) {
            operationsSectionLabel.textProperty().bind(I18N.createStringBinding("label.panel.operations"));
        }
        bindPrompt(valueField, "prompt.tree.value");
        bindPrompt(updateOldValueField, "prompt.tree.old_value");
        bindPrompt(updateNewValueField, "prompt.tree.new_value");
        bindButton(insertBtn, "action.tree.insert");
        bindButton(deleteBtn, "action.tree.delete");
        bindButton(findBtn, "action.tree.find");
        bindButton(updateBtn, "action.tree.update");
        bindButton(randomBtn, "action.tree.random");
    }

    @Override
    protected String moduleId() {
        return "tree";
    }

    private void bindSelectors() {
        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> FXCollections.observableArrayList(I18N.text("label.tree.structure.avl")),
                I18N.localeProperty()));
        algorithmSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> FXCollections.observableArrayList(I18N.text(AlgorithmLabels.key("tree-avl"))),
                I18N.localeProperty()));
        Platform.runLater(() -> {
            structureSelector.getSelectionModel().selectFirst();
            algorithmSelector.getSelectionModel().selectFirst();
        });
    }

    private void bindPrompt(TextField field, String key) {
        if (field != null) {
            field.promptTextProperty().bind(I18N.createStringBinding(key));
        }
    }

    private void bindButton(Button button, String key) {
        if (button != null) {
            button.textProperty().bind(I18N.createStringBinding(key));
        }
    }
}
