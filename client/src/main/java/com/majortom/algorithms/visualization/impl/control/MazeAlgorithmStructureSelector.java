package com.majortom.algorithms.visualization.impl.control;

import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.international.I18N;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;

import java.util.List;

/** Algorithm-mode Maze structure selector that delegates to the existing generator selector. */
public final class MazeAlgorithmStructureSelector extends ComboBox<String> {

    private final List<String> arrayGenerators = AlgorithmCatalog.arrayMazeGenerators();
    private final List<String> graphGenerators = AlgorithmCatalog.graphMazeGenerators();
    private boolean syncing;
    private ComboBox<String> generatorSelector;

    public MazeAlgorithmStructureSelector() {
        setMaxWidth(Double.MAX_VALUE);
        refreshItems();
        getSelectionModel().selectFirst();
        valueProperty().addListener((observable, oldValue, newValue) -> selectMatchingGenerator());
        parentProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(this::connectGenerator));
        I18N.localeProperty().addListener((observable, oldValue, newValue) -> {
            int selected = Math.max(0, getSelectionModel().getSelectedIndex());
            refreshItems();
            getSelectionModel().select(Math.min(selected, getItems().size() - 1));
            Platform.runLater(this::syncFromGenerator);
        });
    }

    private void refreshItems() {
        getItems().setAll(I18N.text("label.maze.structure.array"), I18N.text("label.maze.structure.graph"));
    }

    @SuppressWarnings("unchecked")
    private void connectGenerator() {
        Parent root = rootOf(this);
        Node node;
        if (root == null) {
            node = null;
        } else {
            node = root.lookup("#generatorSelector");
        }
        if (!(node instanceof ComboBox<?> comboBox)) {
            return;
        }
        ComboBox<String> next = (ComboBox<String>) comboBox;
        if (next == generatorSelector) {
            syncFromGenerator();
            return;
        }
        generatorSelector = next;
        generatorSelector.valueProperty().addListener((observable, oldValue, newValue) -> syncFromGenerator());
        syncFromGenerator();
    }

    private void selectMatchingGenerator() {
        if (syncing || generatorSelector == null) {
            return;
        }
        List<String> candidates;
        if (getSelectionModel().getSelectedIndex() == 1) {
            candidates = graphGenerators;
        } else {
            candidates = arrayGenerators;
        }
        if (candidates.isEmpty()) {
            return;
        }
        String label = AlgorithmLabels.text(candidates.getFirst());
        int index = generatorSelector.getItems().indexOf(label);
        if (index >= 0) {
            generatorSelector.getSelectionModel().select(index);
        }
    }

    private void syncFromGenerator() {
        if (generatorSelector == null || generatorSelector.getValue() == null) {
            return;
        }
        String selectedGenerator = generatorSelector.getValue();
        boolean graph = graphGenerators.stream().map(AlgorithmLabels::text).anyMatch(selectedGenerator::equals);
        syncing = true;
        try {
            if (graph) {
                getSelectionModel().select(1);
            } else {
                getSelectionModel().select(0);
            }
        } finally {
            syncing = false;
        }
    }

    private static Parent rootOf(Node node) {
        Parent parent = node.getParent();
        while (parent != null && parent.getParent() != null) {
            parent = parent.getParent();
        }
        return parent;
    }
}
