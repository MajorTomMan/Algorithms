package com.majortom.algorithms.core.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.BaseTreeAlgorithms;
import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.impl.visualizer.TreeVisualizer;
import com.majortom.algorithms.core.visualization.international.I18N;
import com.majortom.algorithms.core.visualization.manager.AlgorithmThreadManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class TreeController<T extends Comparable<T>> extends BaseController<BaseTree<T>> {

    private final BaseTree<T> treeData;
    private final BaseTreeAlgorithms<T> treeAlgorithms;
    private Node customControlPane;

    private enum Mode {
        INSERT, DELETE
    }

    private Mode currentMode = Mode.INSERT;

    @FXML
    private Label inputLabel;
    @FXML
    private Button deleteBtn;
    @FXML
    private Button insertBtn;
    @FXML
    private Button randomBtn;
    @FXML
    private TextField inputField;

    public TreeController(BaseTree<T> treeData, BaseTreeAlgorithms<T> algorithm) {
        super(new TreeVisualizer<T>());
        this.treeData = treeData;
        this.treeAlgorithms = algorithm;
        loadFXMLControls();
    }

    private void loadFXMLControls() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TreeControls.fxml"));
            loader.setResources(I18N.getBundle());
            loader.setController(this);
            this.customControlPane = loader.load();
            setupI18n();
        } catch (IOException e) {
            System.err.println("FXML load failed.");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        if (visualizer != null && treeData != null) {
            visualizer.render(treeData);
        }
    }

    @Override
    public List<Node> getCustomControls() {
        return Collections.singletonList(customControlPane);
    }

    @FXML
    private void handleInsert() {
        this.currentMode = Mode.INSERT;
        handleAlgorithmStart();
    }

    @FXML
    private void handleDelete() {
        this.currentMode = Mode.DELETE;
        handleAlgorithmStart();
    }

    @FXML
    private void handleRandom() {
        int randomVal = (int) (Math.random() * 100);
        inputField.setText(String.valueOf(randomVal));
    }

    @FXML
    private void handleReset() {
        stopAlgorithm();
        if (treeData != null) {
            treeData.clear();
            visualizer.render(treeData);
        }
        if (logArea != null)
            logArea.clear();
    }

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseTree<T>> alg, BaseTree<T> tree) {
        String input = inputField.getText();
        if (input == null || input.trim().isEmpty() || !(alg instanceof BaseTreeAlgorithms)) {
            return;
        }

        BaseTreeAlgorithms<T> targetAlg = (BaseTreeAlgorithms<T>) alg;
        String[] values = input.split("[,，]");

        for (String valStr : values) {
            if (!isRunning())
                break;
            try {
                T val = parseValue(valStr.trim());
                if (currentMode == Mode.INSERT) {
                    targetAlg.put(tree, val);
                    logUpdate("Inserted: " + val);
                } else {
                    targetAlg.remove(tree, val);
                    logUpdate("Removed: " + val);
                }
            } catch (Exception e) {
                logUpdate("Error processing: " + valStr);
            }
        }
    }

    private void logUpdate(String message) {
        AlgorithmThreadManager.postStatus(() -> {
            if (logArea != null) {
                logArea.appendText(message + "\n");
            }
        });
    }

    @Override
    public void handleAlgorithmStart() {
        if (treeData != null && treeAlgorithms != null) {
            startAlgorithm(treeAlgorithms, treeData);
        }
    }

    @SuppressWarnings("unchecked")
    private T parseValue(String s) {
        return (T) Integer.valueOf(s);
    }

    @Override
    protected void setupI18n() {
        if (inputLabel != null)
            inputLabel.textProperty().bind(I18N.createStringBinding("ctrl.tree.val"));
        if (insertBtn != null)
            insertBtn.textProperty().bind(I18N.createStringBinding("btn.tree.insert"));
        if (randomBtn != null)
            randomBtn.textProperty().bind(I18N.createStringBinding("btn.tree.random"));
        if (deleteBtn != null)
            deleteBtn.textProperty().bind(I18N.createStringBinding("btn.tree.delete"));
        if (inputField != null)
            inputField.promptTextProperty().bind(I18N.createStringBinding("ctrl.tree.prompt"));
    }

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        if (statsLabel != null && treeData != null) {
            statsLabel.setText(String.format("Size: %d | Height: %d\nSteps: %d",
                    treeData.size(), treeData.height(), actionCount));
        }
    }
}