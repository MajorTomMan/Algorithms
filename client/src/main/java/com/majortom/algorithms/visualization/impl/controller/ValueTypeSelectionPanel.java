package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.international.I18N;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

/** Owns type-selector presentation; committed values and structure resets stay in the Workbench. */
final class ValueTypeSelectionPanel {
    private final VBox valueTypeBox, algorithmValueTypeBox;
    private final Label valueTypeLabel, hashValueTypeLabel;
    private final ComboBox<ValueTypeOption> valueTypeSelector, algorithmValueTypeSelector, hashValueTypeSelector;
    private final Function<String, String> displayName;
    private boolean updating;

    ValueTypeSelectionPanel(VBox valueTypeBox, VBox algorithmValueTypeBox,
            Label valueTypeLabel, Label hashValueTypeLabel, ComboBox<ValueTypeOption> valueTypeSelector,
            ComboBox<ValueTypeOption> algorithmValueTypeSelector,
            ComboBox<ValueTypeOption> hashValueTypeSelector, Function<String, String> displayName) {
        this.valueTypeBox = valueTypeBox;
        this.algorithmValueTypeBox = algorithmValueTypeBox;
        this.valueTypeLabel = valueTypeLabel;
        this.hashValueTypeLabel = hashValueTypeLabel;
        this.valueTypeSelector = valueTypeSelector;
        this.algorithmValueTypeSelector = algorithmValueTypeSelector;
        this.hashValueTypeSelector = hashValueTypeSelector;
        this.displayName = displayName;
    }

    boolean isUpdating() { return updating; }

    void install(Consumer<ValueTypeOption> onStructure, Consumer<ValueTypeOption> onAlgorithm,
            Consumer<ValueTypeOption> onHashValue) {
        configureValueTypeSelector(valueTypeSelector);
        configureValueTypeSelector(algorithmValueTypeSelector);
        configureValueTypeSelector(hashValueTypeSelector);
        valueTypeSelector.valueProperty().addListener((obs, previous, value) -> onStructure.accept(value));
        algorithmValueTypeSelector.valueProperty().addListener((obs, previous, value) -> onAlgorithm.accept(value));
        hashValueTypeSelector.valueProperty().addListener((obs, previous, value) -> onHashValue.accept(value));
    }

    void render(String moduleId, boolean structureMode, boolean previewActive, boolean running,
            String selected, List<ValueTypeOption> available, List<ValueTypeOption> algorithmTypes) {
        boolean maze = StructureIds.MAZE.equals(moduleId);
        setVisibleManaged(valueTypeBox, !maze);
        setVisibleManaged(algorithmValueTypeBox, !maze);
        if (maze) return;
        updating = true;
        try {
            boolean hashTable = "hash-table".equals(moduleId);
            setVisibleManaged(hashValueTypeLabel, hashTable);
            setVisibleManaged(hashValueTypeSelector, hashTable);
            valueTypeLabel.textProperty().unbind();
            if (hashTable) {
                valueTypeLabel.setText(I18N.text("label.value_type.key"));
                valueTypeSelector.getItems().clear();
                hashValueTypeSelector.getItems().clear();
                return;
            }
            String typeLabelKey = switch (moduleId) {
                case StructureIds.TREE -> "label.value_type.node";
                case StructureIds.GRAPH -> "label.value_type.vertex";
                default -> "label.value_type.element";
            };
            valueTypeLabel.setText(I18N.text(typeLabelKey));
            valueTypeSelector.setDisable(StructureIds.STRING.equals(moduleId) || !structureMode || previewActive || running);
            valueTypeSelector.getItems().setAll(available);
            selectValueType(valueTypeSelector, selected);
            algorithmValueTypeSelector.getItems().setAll(algorithmTypes);
            selectValueType(algorithmValueTypeSelector, selected);
            algorithmValueTypeSelector.setDisable(StructureIds.STRING.equals(moduleId) || running);
        } finally {
            updating = false;
        }
    }

    private static void setVisibleManaged(Node node, boolean visible) {
        if (node == null) return;
        node.setManaged(visible);
        node.setVisible(visible);
    }

    private void configureValueTypeSelector(ComboBox<ValueTypeOption> selector) {
        if (selector == null) {
            return;
        }
        selector.setCellFactory(ignored -> valueTypeCell());
        selector.setButtonCell(valueTypeCell());
    }

    private ListCell<ValueTypeOption> valueTypeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ValueTypeOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                    return;
                }
                if (item.available()) {
                    setText(displayName.apply(item.type()));
                    setDisable(false);
                } else {
                    setText(displayName.apply(item.type()) + " · " + I18N.text("label.value_type.unavailable"));
                    setDisable(true);
                }
            }
        };
    }

    private void selectValueType(ComboBox<ValueTypeOption> selector, String type) {
        selector.getSelectionModel().clearSelection();
        if (type == null) {
            return;
        }
        for (ValueTypeOption option : selector.getItems()) {
            if (option.type().equals(type)) {
                selector.getSelectionModel().select(option);
                return;
            }
        }
    }

}
