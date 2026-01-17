package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.BaseGraphAlgorithms;
import com.majortom.algorithms.core.graph.impl.DirectedGraph;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.impl.visualizer.GraphVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 图算法模块控制器
 * 职责：管理图结构的动态维护（增删点边）与图算法的执行与渲染同步。
 */
public class GraphController<V> extends BaseModuleController<BaseGraph<V>> {

    private final BaseGraphAlgorithms<V> algorithm;

    @FXML
    private TextField nodeInputField; // 节点 ID 输入
    @FXML
    private TextField fromNodeField; // 边起点
    @FXML
    private TextField toNodeField; // 边终点
    @FXML
    private TextField weightField; // 权重
    @FXML
    private Button runBtn; // 局部运行按钮（如果保留）
    @FXML
    private Button addBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Button linkBtn;

    public GraphController(BaseGraphAlgorithms<V> algorithm, BaseGraph<V> graphData) {
        // 自动加载 FXML 并绑定
        super(new GraphVisualizer<>(graphData), "/fxml/GraphControls.fxml");
        this.algorithm = algorithm;

        if (graphData != null) {
            visualizer.render(graphData);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        // 为该模块特有的操作按钮应用视觉交互效果
        EffectUtils.applyDynamicEffect(runBtn, addBtn, deleteBtn, linkBtn);
    }

    /**
     * 实现基类钩子：响应全局 Start 按钮逻辑
     */
    @Override
    public void handleAlgorithmStart() {
        BaseGraph<V> currentData = visualizer.getLastData();
        if (currentData != null && algorithm != null) {
            startAlgorithm(algorithm, currentData);
        }
    }

    /**
     * UI 事件：添加节点
     */
    @FXML
    private void handleAddNode() {
        String nodeId = nodeInputField.getText().trim();
        if (nodeId.isEmpty())
            return;

        AlgorithmThreadManager.postStatus(() -> {
            BaseGraph<V> data = visualizer.getLastData();
            if (data != null && data.getGraph().getNode(nodeId) == null) {
                org.graphstream.graph.Node n = data.getGraph().addNode(nodeId);
                n.setAttribute("ui.label", nodeId);
                logI18n("status.tree.insert", nodeId);
            }
        });
    }

    /**
     * UI 事件：建立边关系
     */
    @FXML
    private void handleLink() {
        String from = fromNodeField.getText().trim();
        String to = toNodeField.getText().trim();
        String wText = weightField.getText().trim();

        if (from.isEmpty() || to.isEmpty())
            return;

        AlgorithmThreadManager.postStatus(() -> {
            BaseGraph<V> data = visualizer.getLastData();
            if (data == null)
                return;

            try {
                int weight = wText.isEmpty() ? 1 : Integer.parseInt(wText);
                if (data.getGraph().getNode(from) != null && data.getGraph().getNode(to) != null) {
                    data.addEdge(from, to, weight);
                    String key = (data instanceof DirectedGraph) ? "status.graph.link_directed"
                            : "status.graph.link_undirected";
                    logI18n(key, from, to, weight);
                } else {
                    logI18n("status.graph.node_missing");
                }
            } catch (NumberFormatException e) {
                logI18n("status.graph.parse_error");
            }
        });
    }

    /**
     * UI 事件：删除节点
     */
    @FXML
    private void handleDeleteNode() {
        String nodeId = nodeInputField.getText().trim();
        if (nodeId.isEmpty())
            return;

        AlgorithmThreadManager.postStatus(() -> {
            BaseGraph<V> data = visualizer.getLastData();
            if (data != null && data.getGraph().getNode(nodeId) != null) {
                data.getGraph().removeNode(nodeId);
                logI18n("status.tree.delete", nodeId);
            }
        });
    }

    /**
     * 算法核心执行逻辑
     */
    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseGraph<V>> alg, BaseGraph<V> data) {
        String startId = nodeInputField.getText().trim();

        // 校验起点有效性
        if (startId.isEmpty() || data.getGraph().getNode(startId) == null) {
            appendLog("Error: Start node [" + startId + "] invalid.");
            return;
        }

        if (alg instanceof BaseGraphAlgorithms) {
            ((BaseGraphAlgorithms<V>) alg).run(data, startId);
        }
    }

    /**
     * 格式化统计信息：图算法通常关注访问节点数和边数
     */
    @Override
    protected String formatStatsMessage() {
        BaseGraph<V> data = visualizer.getLastData();
        if (data == null)
            return "Graph: N/A";
        return String.format("Nodes: %d | Edges: %d\nSteps: %d",
                data.getGraph().getNodeCount(),
                data.getGraph().getEdgeCount(),
                stats.actionCount);
    }

    @Override
    protected void onAlgorithmFinished() {
        super.onAlgorithmFinished();
        logI18n("side.finished");
    }

    @Override
    protected void onResetData() {
        BaseGraph<V> g = visualizer.getLastData();
        if (g != null) {
            g.resetStatistics();
            g.getGraph().nodes().forEach(n -> {
                n.removeAttribute("ui.class");
                n.setAttribute("ui.label", n.getId());
            });
            g.getGraph().edges().forEach(e -> e.removeAttribute("ui.class"));
        }
    }

    @Override
    protected void setupI18n() {
        if (nodeInputField != null)
            nodeInputField.promptTextProperty().bind(I18N.createStringBinding("ctrl.graph.prompt"));
        if (fromNodeField != null)
            fromNodeField.promptTextProperty().bind(I18N.createStringBinding("ctrl.graph.from"));
        if (toNodeField != null)
            toNodeField.promptTextProperty().bind(I18N.createStringBinding("ctrl.graph.to"));
        if (weightField != null)
            weightField.promptTextProperty().bind(I18N.createStringBinding("ctrl.graph.weight"));

        if (linkBtn != null)
            linkBtn.textProperty().bind(I18N.createStringBinding("btn.graph.link"));
        if (runBtn != null)
            runBtn.textProperty().bind(I18N.createStringBinding("btn.graph.run"));
        if (addBtn != null)
            addBtn.textProperty().bind(I18N.createStringBinding("btn.tree.insert"));
        if (deleteBtn != null)
            deleteBtn.textProperty().bind(I18N.createStringBinding("btn.tree.delete"));
    }
}