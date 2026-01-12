package com.majortom.algorithms.core.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.BaseGraphAlgorithms;
import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.impl.visualizer.GraphVisualizer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.graphstream.graph.Graph;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 图算法控制器
 * 职责：适配 BaseGraph 实体与特定图算法，处理起点选择与 GraphStream 状态同步。
 */
public class GraphController<V> extends BaseController<Graph> {

    private final BaseGraphAlgorithms<V> algorithm; // 具体的算法逻辑（如 BFS, DFS）
    private final BaseGraph<V> graphData; // 图数据实体封装
    private Node customControlPane;

    private Label sideStatsLabel;
    private TextArea sideLogArea;

    @FXML
    private TextField nodeInputField;

    public GraphController(BaseGraphAlgorithms<V> algorithm, BaseGraph<V> graphData) {
        this.algorithm = algorithm;
        this.graphData = graphData;
        loadFXMLControls();
    }

    private void loadFXMLControls() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GraphControls.fxml"));
            loader.setController(this);
            this.customControlPane = loader.load();
        } catch (IOException e) {
            System.err.println("[Error] GraphControls.fxml load failed: " + e.getMessage());
        }
    }

    public void setUIReferences(Label statsLabel, TextArea logArea) {
        this.sideStatsLabel = statsLabel;
        this.sideLogArea = logArea;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化视觉呈现组件
        this.visualizer = new GraphVisualizer();
    }

    @Override
    public List<Node> getCustomControls() {
        return Collections.singletonList(customControlPane);
    }

    @FXML
    private void handleRunAlgorithm() {
        // 启动算法流程。注意：这里传的是 GraphStream 的 Graph 实例
        startAlgorithm(algorithm, graphData.getGraph());
    }

    @FXML
    private void handleReset() {
        // 重置 UI 样式，这属于数据实体的清理逻辑
        Graph g = graphData.getGraph();
        g.nodes().forEach(n -> n.removeAttribute("ui.class"));
        g.edges().forEach(e -> e.removeAttribute("ui.class"));

        if (sideLogArea != null)
            sideLogArea.clear();
        if (sideStatsLabel != null)
            sideStatsLabel.setText("Status: Ready");
    }

    // --- 算法执行逻辑适配 ---

    @Override
    protected void executeAlgorithm(BaseAlgorithms<Graph> alg, Graph graph) {
        String startNodeId = nodeInputField.getText().trim();

        // 1. 验证输入合法性
        if (startNodeId.isEmpty() || graph.getNode(startNodeId) == null) {
            if (sideLogArea != null) {
                Platform.runLater(
                        () -> sideLogArea.appendText("System Error: Starting Node [" + startNodeId + "] not found.\n"));
            }
            return;
        }

        // 2. 核心桥接逻辑
        // 尽管 BaseAlgorithms 要求实现 run(Graph)，但图算法需要 BaseGraph 和 StartID。
        if (alg instanceof BaseGraphAlgorithms) {
            BaseGraphAlgorithms<V> graphAlg = (BaseGraphAlgorithms<V>) alg;

            // 这里不调用 alg.run(graph)，而是调用图算法特有的 run(BaseGraph, String)
            // 这种设计允许我们在 Controller 层拦截 UI 参数并注入给算法
            graphAlg.run(graphData, startNodeId);
        }
    }

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        if (sideStatsLabel != null) {
            Platform.runLater(() -> sideStatsLabel.setText(
                    String.format("Current Focus: %s\nStep Count: %d",
                            nodeInputField.getText(), actionCount)));
        }
    }

    @Override
    protected void onAlgorithmFinished() {
        if (sideLogArea != null) {
            Platform.runLater(() -> sideLogArea.appendText("System: Graph traversal complete.\n"));
        }
    }

    @Override
    public void handleAlgorithmStart() {
        // TODO Auto-generated method stub
        handleRunAlgorithm();
    }
}