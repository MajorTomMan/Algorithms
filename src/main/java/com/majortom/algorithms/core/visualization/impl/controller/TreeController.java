package com.majortom.algorithms.core.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.BaseTreeAlgorithms;
import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.impl.visualizer.TreeVisualizer;
import com.majortom.algorithms.core.visualization.international.I18N;
import com.majortom.algorithms.core.visualization.manager.AlgorithmThreadManager;
import com.majortom.algorithms.utils.EffectUtils;
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

/**
 * 树结构算法控制器
 * 职责：连接 AVLTreeEntity (数据) 与 AVLTreeAlgorithms (算法)。
 */
public class TreeController<T extends Comparable<T>> extends BaseController<BaseTree<T>> {

    private BaseTree<T> treeData;
    private final BaseTreeAlgorithms<T> treeAlgorithms;
    private Node customControlPane;

    private enum Mode {
        INSERT, DELETE
    }

    private Mode currentMode = Mode.INSERT;

    @FXML
    private Label inputLabel;
    @FXML
    private Button deleteBtn, insertBtn, randomBtn;
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
        } catch (IOException e) {
            System.err.println("FXML load failed: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        setupI18n();
        if (visualizer != null && treeData != null) {
            visualizer.render(treeData, null, null);
        }
        EffectUtils.applyDynamicEffect(deleteBtn);
        EffectUtils.applyDynamicEffect(insertBtn);
        EffectUtils.applyDynamicEffect(randomBtn);
    }

    @Override
    public void handleStartAction() {
        if (treeData != null && treeAlgorithms != null) {
            // 在开始新算法前，确保原数据已备份（BaseController 逻辑）
            startAlgorithm(treeAlgorithms, treeData);
        }
    }

    @Override
    public void handleResetAction() {
        stopAlgorithm();

        if (originalData != null) {
            // 核心修复：从快照克隆一份全新的 Entity，确保状态完全回滚
            BaseTree<T> restored = (BaseTree<T>) originalData.copy();
            updateCurrentDataReference(restored);

            // 重置统计数据并在 UI 上反映
            this.treeData.resetStatistics();
            visualizer.render(this.treeData, null, null);
            updateUIComponents(0, 0);
        } else {
            treeData.clear();
            visualizer.render(treeData, null, null);
        }

        if (logArea != null) {
            logArea.clear();
            logArea.appendText("System: Tree state restored.\n");
        }
    }

    @FXML
    private void handleInsert() {
        this.currentMode = Mode.INSERT;
        handleStartAction();
    }

    @FXML
    private void handleDelete() {
        this.currentMode = Mode.DELETE;
        handleStartAction();
    }

    @FXML
    private void handleRandom() {
        int randomVal = (int) (Math.random() * 100);
        inputField.setText(String.valueOf(randomVal));
    }

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseTree<T>> alg, BaseTree<T> tree) {
        String input = inputField.getText();
        if (input == null || input.trim().isEmpty() || !(alg instanceof BaseTreeAlgorithms))
            return;

        BaseTreeAlgorithms<T> targetAlg = (BaseTreeAlgorithms<T>) alg;
        String[] values = input.split("[,，]");

        for (String valStr : values) {
            if (!AlgorithmThreadManager.isRunning())
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

    @Override
    protected void updateCurrentDataReference(BaseTree<T> restoredData) {
        // 更新 Controller 持有的引用，确保后续操作针对的是回滚后的新实例
        this.treeData = restoredData;
    }

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        if (statsLabel != null && treeData != null) {
            // 除了步骤数，实时展示树的规模与高度，增加专业感
            String stats = String.format("Size: %d | Height: %d\nSteps: %d | Compares: %d",
                    treeData.size(), treeData.height(), actionCount, compareCount);
            statsLabel.setText(stats);
        }
    }

    private void logUpdate(String message) {
        AlgorithmThreadManager.postStatus(() -> {
            if (logArea != null)
                logArea.appendText(message + "\n");
        });
    }

    @SuppressWarnings("unchecked")
    private T parseValue(String s) {
        // 这里可以根据 T 的实际类型进行动态转换
        return (T) Integer.valueOf(s);
    }

    @Override
    public List<Node> getCustomControls() {
        return Collections.singletonList(customControlPane);
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
}