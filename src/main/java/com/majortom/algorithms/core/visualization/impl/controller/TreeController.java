package com.majortom.algorithms.core.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.BaseTreeAlgorithms;
import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.impl.visualizer.TreeVisualizer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 树结构算法控制器 (FXML 适配版)
 * 职责：绑定 TreeControls.fxml，协调 Tree 数据实体与 Algorithms 算法逻辑。
 */
public class TreeController<T extends Comparable<T>> extends BaseController<BaseTree<T>> {

    // 1. 分离数据与逻辑：treeData 是容器，treeAlgorithms 是操作者
    private final BaseTree<T> treeData;
    private final BaseTreeAlgorithms<T> treeAlgorithms;

    private Node customControlPane;

    @FXML
    private Label sideStatsLabel;
    @FXML
    private TextArea sideLogArea;
    @FXML
    private TextField inputField;

    /**
     * 构造函数
     * 
     * @param treeData  传入数据容器实体
     * @param algorithm 传入具体的算法逻辑实现
     */
    public TreeController(BaseTree<T> treeData, BaseTreeAlgorithms<T> algorithm) {
        this.treeData = treeData;
        this.treeAlgorithms = algorithm;
        loadFXMLControls();
    }

    private void loadFXMLControls() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TreeControls.fxml"));
            loader.setController(this);
            this.customControlPane = loader.load();
        } catch (IOException e) {
            System.err.println("[Error] Failed to load TreeControls.fxml: " + e.getMessage());
        }
    }

    public void setUIReferences(Label statsLabel, TextArea logArea) {
        this.sideStatsLabel = statsLabel;
        this.sideLogArea = logArea;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化视觉呈现组件，它应该观察 treeData
        this.visualizer = new TreeVisualizer<T>();
    }

    @Override
    public List<Node> getCustomControls() {
        return Collections.singletonList(customControlPane);
    }

    // --- FXML 动作处理 ---

    @FXML
    private void handleInsert() {
        // 关键改动：执行者是 treeAlgorithms，操作的数据对象是 treeData
        startAlgorithm(treeAlgorithms, treeData);
    }

    @FXML
    private void handleRandom() {
        int randomVal = (int) (Math.random() * 100);
        inputField.setText(String.valueOf(randomVal));
    }

    // --- 算法逻辑执行钩子 ---

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseTree<T>> alg, BaseTree<T> tree) {
        String input = inputField.getText();
        if (input == null || input.trim().isEmpty())
            return;

        // 此时 alg 其实就是 treeAlgorithms，tree 其实就是 treeData
        if (alg instanceof BaseTreeAlgorithms) {
            BaseTreeAlgorithms<T> targetAlg = (BaseTreeAlgorithms<T>) alg;

            String[] values = input.split("[,，]");
            for (String valStr : values) {
                if (!isRunning)
                    break;

                try {
                    T val = parseValue(valStr.trim());
                    // 核心调用：使用算法逻辑去操作树实体
                    targetAlg.put(tree, val);

                    if (sideLogArea != null) {
                        Platform.runLater(() -> sideLogArea.appendText("Successfully inserted: " + val + "\n"));
                    }
                } catch (Exception e) {
                    if (sideLogArea != null) {
                        Platform.runLater(() -> sideLogArea.appendText("Error: Invalid input " + valStr + "\n"));
                    }
                }
            }
        }
    }

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        // 统计信息现在从 treeData 实体中获取
        if (sideStatsLabel != null && treeData != null) {
            Platform.runLater(() -> sideStatsLabel.setText(
                    String.format("Size: %d\nHeight: %d\nActions: %d",
                            treeData.size(), treeData.height(), actionCount)));
        }
    }

    @SuppressWarnings("unchecked")
    private T parseValue(String s) {
        return (T) Integer.valueOf(s);
    }

    @Override
    public void handleAlgorithmStart() {
        // TODO Auto-generated method stub
        startAlgorithm(treeAlgorithms, treeData);
    }
}