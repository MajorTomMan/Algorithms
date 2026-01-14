package com.majortom.algorithms.core.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.BaseGraphAlgorithms;
import com.majortom.algorithms.core.graph.impl.DirectedGraph;
import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.impl.visualizer.GraphVisualizer;
import com.majortom.algorithms.core.visualization.international.I18N;
import com.majortom.algorithms.core.visualization.manager.AlgorithmThreadManager;
import com.majortom.algorithms.utils.EffectUtils;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class GraphController<V> extends BaseController<BaseGraph<V>> {

    private final BaseGraphAlgorithms<V> algorithm;
    private Node customControlPane;

    @FXML
    private TextField nodeInputField; // 节点 ID 输入
    @FXML
    private TextField fromNodeField; // 边起点
    @FXML
    private TextField toNodeField; // 边终点
    @FXML
    private TextField weightField; // 权重

    @FXML
    private Button runBtn, addBtn, deleteBtn, linkBtn;

    public GraphController(BaseGraphAlgorithms<V> algorithm, BaseGraph<V> graphData) {
        super(new GraphVisualizer<>(graphData));
        this.algorithm = algorithm;
        loadFXMLControls();
        if (graphData != null)
            visualizer.render(graphData);
    }

    private void loadFXMLControls() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GraphControls.fxml"));
            loader.setResources(I18N.getBundle());
            loader.setController(this);
            this.customControlPane = loader.load();
            setupI18n();
        } catch (IOException e) {
            System.err.println("[Error] GraphControls load failed: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TODO Auto-generated method stub
        super.initialize(location, resources);
        EffectUtils.applyDynamicEffect(runBtn);
        EffectUtils.applyDynamicEffect(addBtn);
        EffectUtils.applyDynamicEffect(deleteBtn);
        EffectUtils.applyDynamicEffect(linkBtn);
    }

    @Override
    public List<Node> getCustomControls() {
        return Collections.singletonList(customControlPane);
    }

    @Override
    public void handleAlgorithmStart() {
        if (visualizer.getLastData() != null)
            startAlgorithm(algorithm, visualizer.getLastData());
    }

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
                logUpdate("status.tree.insert", nodeId);
            }
        });
    }

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
                    // 根据类型切换国际化 Key
                    String key = (data instanceof DirectedGraph) ? "status.graph.link_directed"
                            : "status.graph.link_undirected";
                    logUpdate(key, from, to, weight);
                } else {
                    logUpdate("status.graph.node_missing");
                }
            } catch (NumberFormatException e) {
                logUpdate("status.graph.parse_error");
            }
        });
    }

    @FXML
    private void handleDeleteNode() {
        String nodeId = nodeInputField.getText().trim();
        if (nodeId.isEmpty())
            return;
        AlgorithmThreadManager.postStatus(() -> {
            BaseGraph<V> data = visualizer.getLastData();
            if (data != null && data.getGraph().getNode(nodeId) != null) {
                data.getGraph().removeNode(nodeId);
                logUpdate("status.tree.delete", nodeId);
            }
        });
    }

    @FXML
    public void handleReset() {
        stopAlgorithm();
        AlgorithmThreadManager.postStatus(() -> {
            BaseGraph<V> g = visualizer.getLastData();
            if (g != null) {
                g.reset();
                g.getGraph().nodes().forEach(n -> {
                    n.removeAttribute("ui.class");
                    n.setAttribute("ui.label", n.getId());
                });
                g.getGraph().edges().forEach(e -> e.removeAttribute("ui.class"));
            }
            if (logArea != null)
                logArea.clear();
            if (statsLabel != null) {
                statsLabel.textProperty().unbind();
                statsLabel.setText(I18N.getBundle().getString("side.ready"));
            }
        });
    }

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseGraph<V>> alg, BaseGraph<V> data) {
        String startId = nodeInputField.getText().trim();
        if (startId.isEmpty() || data.getGraph().getNode(startId) == null) {
            logUpdate("Error: Start node [" + startId + "] not found.");
            return;
        }
        if (alg instanceof BaseGraphAlgorithms)
            ((BaseGraphAlgorithms<V>) alg).run(data, startId);
    }

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        AlgorithmThreadManager.postStatus(() -> {
            if (statsLabel != null) {
                String sK = I18N.getBundle().getString("stats.action");
                String cK = I18N.getBundle().getString("stats.compare");
                statsLabel.setText(String.format("%s: %d | %s: %d", sK, actionCount, cK, compareCount));
            }
        });
    }

    @Override
    protected void onAlgorithmFinished() {
        super.onAlgorithmFinished();
        logUpdate("side.finished");
    }

    private void logUpdate(String key, Object... args) {
        AlgorithmThreadManager.postStatus(() -> {
            if (logArea != null) {
                try {
                    String p = I18N.getBundle().getString(key);
                    logArea.appendText(String.format(p, args) + "\n");
                } catch (Exception e) {
                    logArea.appendText(key + "\n");
                }
            }
        });
    }

    @Override
    protected void setupI18n() {
        // 1. 节点控制部分
        if (nodeInputField != null)
            nodeInputField.promptTextProperty().bind(I18N.createStringBinding("ctrl.graph.prompt"));

        // 2. 边连接控制部分 (新字段修复)
        if (fromNodeField != null)
            fromNodeField.promptTextProperty().bind(I18N.createStringBinding("ctrl.graph.from"));
        if (toNodeField != null)
            toNodeField.promptTextProperty().bind(I18N.createStringBinding("ctrl.graph.to"));
        if (weightField != null)
            weightField.promptTextProperty().bind(I18N.createStringBinding("ctrl.graph.weight"));
        if (linkBtn != null)
            linkBtn.textProperty().bind(I18N.createStringBinding("btn.graph.link"));

        // 3. 操作按钮部分
        if (runBtn != null)
            runBtn.textProperty().bind(I18N.createStringBinding("btn.graph.run"));
        if (addBtn != null)
            addBtn.textProperty().bind(I18N.createStringBinding("btn.tree.insert"));
        if (deleteBtn != null)
            deleteBtn.textProperty().bind(I18N.createStringBinding("btn.tree.delete"));
    }
}