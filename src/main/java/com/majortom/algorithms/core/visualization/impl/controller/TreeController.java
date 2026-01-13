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
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 树结构算法控制器
 * 职责：协调 Tree 数据实体与 AVL/BST 算法逻辑，支持动态插入与可视化呈现。
 */
public class TreeController<T extends Comparable<T>> extends BaseController<BaseTree<T>> {

    private final BaseTree<T> treeData;
    private final BaseTreeAlgorithms<T> treeAlgorithms;
    private Node customControlPane;

    @FXML
    private TextField inputField;

    /**
     * 构造函数
     * 
     * @param treeData  数据容器实体（如 AVL 树的数据承载体）
     * @param algorithm 具体的算法实现（如 AVLTree 的 put/remove 逻辑）
     */
    public TreeController(BaseTree<T> treeData, BaseTreeAlgorithms<T> algorithm) {
        // 🚩 修正：现在 super 仅接收 visualizer。
        // TreeVisualizer 内部会根据 BaseTree 的 root 进行坐标计算和绘制。
        super(new TreeVisualizer<T>());
        this.treeData = treeData;
        this.treeAlgorithms = algorithm;
        loadFXMLControls();
    }

    private void loadFXMLControls() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TreeControls.fxml"));
            loader.setResources(ResourceBundle.getBundle("language.language"));
            loader.setController(this);
            this.customControlPane = loader.load();
        } catch (IOException e) {
            System.err.println("[Error] Tree FXML load failed.");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        // 初始空树绘制
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
        if (input == null || input.trim().isEmpty())
            return;

        // 🚩 修正：基于 BaseController 的 S extends BaseStructure 契约进行调用
        if (alg instanceof BaseTreeAlgorithms) {
            BaseTreeAlgorithms<T> targetAlg = (BaseTreeAlgorithms<T>) alg;

            String[] values = input.split("[,，]");
            for (String valStr : values) {
                // 🚩 检查 Manager 状态，确保能被 stopAlgorithm() 瞬间中断
                if (!isRunning())
                    break;

                try {
                    T val = parseValue(valStr.trim());
                    // 执行插入：AVLTree 内部会调用 syncTree 触发 UI 渲染
                    targetAlg.put(tree, val);

                    Platform.runLater(() -> {
                        if (logArea != null)
                            logArea.appendText("Inserted Node: " + val + "\n");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        if (logArea != null)
                            logArea.appendText("Error parsing: " + valStr + "\n");
                    });
                }
            }
        }
    }

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        // 🚩 此时已在 UI 线程，利用 treeData 内部的统计量更新面板
        if (statsLabel != null && treeData != null) {
            statsLabel.setText(String.format("Size: %d | Height: %d\nSteps: %d",
                    treeData.size(), treeData.height(), actionCount));
        }
    }

    @Override
    public void handleAlgorithmStart() {
        if (treeData != null && treeAlgorithms != null) {
            startAlgorithm(treeAlgorithms, treeData);
        }
    }

    @SuppressWarnings("unchecked")
    private T parseValue(String s) {
        // 默认为 Integer，可根据实际需求扩展
        return (T) Integer.valueOf(s);
    }
}