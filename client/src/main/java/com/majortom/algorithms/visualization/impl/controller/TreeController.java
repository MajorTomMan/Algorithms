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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

public final class TreeController extends BaseModuleController<AvlTreeViewState> {

    private final List<Integer> values = new ArrayList<>();
    private List<AvlCommand> pendingCommands = List.of();

    @FXML private Label structureLabel;
    @FXML private ComboBox<String> structureSelector;
    @FXML private Label algorithmLabel;
    @FXML private ComboBox<String> algorithmSelector;
    @FXML private Button operationBtn;

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
        EffectUtils.applyDynamicEffect(operationBtn);
    }

    @FXML
    private void openTreeOperationDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(I18N.text("dialog.tree.title"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        TextField valueField = new TextField();
        valueField.setPromptText(I18N.text("prompt.tree.value"));
        OperationDialogTheme.addClasses(valueField, "dark-textfield", "dialog-input");
        Button insertButton = new Button(I18N.text("action.tree.insert"));
        OperationDialogTheme.addClasses(insertButton, "btn-ran-blue", "compact-button");
        insertButton.setOnAction(event -> runCommands(AvlCommand.Operation.INSERT, valueField.getText()));
        Button deleteButton = new Button(I18N.text("action.tree.delete"));
        OperationDialogTheme.addClasses(deleteButton, "btn-ran-red", "compact-button");
        deleteButton.setOnAction(event -> runCommands(AvlCommand.Operation.REMOVE, valueField.getText()));
        Button randomButton = new Button(I18N.text("action.tree.random"));
        OperationDialogTheme.addClasses(randomButton, "btn-ran-gold", "compact-button");
        randomButton.setOnAction(event -> {
            valueField.setText(String.valueOf(new Random().nextInt(100)));
            dispatchVisualizerAction(com.majortom.algorithms.visualization.VisualizationActionType.TREE_RANDOM);
        });
        Label sectionTitle = new Label(I18N.text("label.tree.value"));
        OperationDialogTheme.addClasses(sectionTitle, "dialog-section-title");
        VBox content = new VBox(12,
                sectionTitle, valueField, new HBox(10, insertButton, deleteButton, randomButton));
        OperationDialogTheme.addClasses(content, "dialog-form-section");
        dialog.getDialogPane().setContent(content);
        OperationDialogTheme.apply(dialog, 540.0d);
        dialog.showAndWait();
    }

    private void runCommands(AvlCommand.Operation operation, String input) {
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
        AvlTreeViewState state = visualizer.getLastData();
        if (state != null) {
            values.clear();
            values.addAll(state.values());
        }
        super.onAlgorithmFinished();
        logI18n("message.execution.finished");
    }

    @Override
    protected String formatStatsMessage() {
        AvlTreeViewState state = visualizer.getLastData();
        int size = values.size();
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
        if (operationBtn != null) {
            operationBtn.textProperty().bind(I18N.createStringBinding("action.tree.operation"));
        }
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
}
