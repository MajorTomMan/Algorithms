package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.BaseTreeAlgorithms;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.impl.visualizer.TreeVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 树算法模块控制器
 * 职责：管理树结构的维护（增删节点）与平衡/搜索算法的执行渲染。
 */
public class TreeController<T extends Comparable<T>> extends BaseModuleController<BaseTree<T>> {

    private final BaseTree<T> treeData;
    private final BaseTreeAlgorithms<T> treeAlgorithms;

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
        // 自动完成 FXML 载入与控制面板注入
        super(new TreeVisualizer<>(), "/fxml/TreeControls.fxml");
        this.treeData = treeData;
        this.treeAlgorithms = algorithm;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        // 初始静态渲染
        if (visualizer != null && treeData != null) {
            visualizer.render(treeData);
        }

        EffectUtils.applyDynamicEffect(deleteBtn, insertBtn, randomBtn);
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

    /**
     * 实现基类钩子：响应全局 Start 按钮。
     * 默认采用当前 Mode (Insert/Delete) 执行。
     */
    @Override
    public void handleAlgorithmStart() {
        if (treeData != null && treeAlgorithms != null) {
            if (!AlgorithmThreadManager.isRunning()) {
                startAlgorithm(treeAlgorithms, treeData);
            }

        }
    }

    /**
     * 算法核心执行逻辑：支持单值或逗号分隔的批量操作。
     */
    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseTree<T>> alg, BaseTree<T> tree) {
        String input = inputField.getText();
        if (input == null || input.trim().isEmpty() || !(alg instanceof BaseTreeAlgorithms)) {
            appendLog("Error: Invalid input or algorithm type.");
            return;
        }

        BaseTreeAlgorithms<T> targetAlg = (BaseTreeAlgorithms<T>) alg;
        // 支持中英文逗号解析
        String[] values = input.split("[,，]");

        for (String valStr : values) {
            // 关键：批量操作中必须检查运行状态，以响应 UI 的“停止”指令
            if (!AlgorithmThreadManager.isRunning()) {
                break;
            }

            try {
                T val = parseValue(valStr.trim());
                if (currentMode == Mode.INSERT) {
                    targetAlg.put(tree, val);
                    logI18n("status.tree.insert", val);
                } else {
                    targetAlg.remove(tree, val);
                    logI18n("status.tree.delete", val);
                }
            } catch (Exception e) {
                appendLog("Error processing value: " + valStr);
            }
        }
    }

    /**
     * 格式化统计信息：增加树的高度显示，这是树算法的关键指标。
     */
    @Override
    protected String formatStatsMessage() {
        if (treeData == null)
            return "Tree: N/A";
        return String.format("Size: %d | Height: %d\nSteps: %d",
                treeData.size(), treeData.height(), stats.actionCount);
    }

    /**
     * 实现基类的重置钩子：对应全局 Reset 按钮。
     */
    @Override
    protected void onResetData() {
        AlgorithmThreadManager.stopAll();

        if (treeData != null) {
            treeData.resetStatistics();
            visualizer.render(treeData, null, null);
            appendLog("Tree structure cleared.");
        }
    }

    @SuppressWarnings("unchecked")
    private T parseValue(String s) {
        // 针对当前业务逻辑默认按 Integer 处理
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
}