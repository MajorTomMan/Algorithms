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

/**
 * 图算法控制器
 * 职责：管理图的节点、边增删及算法执行，处理 GraphStream 的视觉状态重置。
 */
public class GraphController<V> extends BaseController<BaseGraph<V>> {

    private final BaseGraphAlgorithms<V> algorithm;
    private Node customControlPane;

    @FXML
    private TextField nodeInputField, fromNodeField, toNodeField, weightField;
    @FXML
    private Button runBtn, addBtn, deleteBtn, linkBtn;

    public GraphController(BaseGraphAlgorithms<V> algorithm, BaseGraph<V> graphData) {
        super(new GraphVisualizer<>(graphData));
        this.algorithm = algorithm;
        loadFXMLControls();
        // 初始渲染
        if (graphData != null && visualizer != null) {
            visualizer.render(graphData, null, null);
        }
    }

    private void loadFXMLControls() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GraphControls.fxml"));
            loader.setResources(I18N.getBundle());
            loader.setController(this);
            this.customControlPane = loader.load();
        } catch (IOException e) {
            System.err.println("[Error] GraphControls load failed: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        setupI18n();
        EffectUtils.applyDynamicEffect(runBtn);
        EffectUtils.applyDynamicEffect(addBtn);
        EffectUtils.applyDynamicEffect(deleteBtn);
        EffectUtils.applyDynamicEffect(linkBtn);
    }

    // --- 实现 BaseController 核心动作接口 ---

    @Override
    public void handleStartAction() {
        BaseGraph<V> data = visualizer.getLastData();
        if (data != null && algorithm != null) {
            startAlgorithm(algorithm, data);
        }
    }

    @Override
    public void handleResetAction() {
        stopAlgorithm();

        // 由于 GraphStream 的 Graph 对象与 View 绑定，我们执行“状态重置”而非“引用替换”
        AlgorithmThreadManager.postStatus(() -> {
            BaseGraph<V> g = visualizer.getLastData();
            if (g != null) {
                // 1. 清除算法中间数据（如访问状态等）
                g.resetGraphState();

                // 2. 清除 GraphStream 节点的 CSS 样式类
                g.getGraph().nodes().forEach(n -> {
                    n.removeAttribute("ui.class");
                    n.setAttribute("ui.label", n.getId());
                });

                // 3. 清除边的 CSS 样式类
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

    // --- 业务逻辑处理 ---

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

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseGraph<V>> alg, BaseGraph<V> data) {
        String startId = nodeInputField.getText().trim();
        if (startId.isEmpty() || data.getGraph().getNode(startId) == null) {
            logUpdate("Error: Start node [" + startId + "] not found.");
            return;
        }
        if (alg instanceof BaseGraphAlgorithms) {
            ((BaseGraphAlgorithms<V>) alg).run(data, startId);
        }
    }

    // --- 辅助与状态更新 ---

    @Override
    protected void updateCurrentDataReference(BaseGraph<V> restoredData) {
        // 图模块通常不建议通过快照恢复来直接替换 Graph 引用，因为会断开 View 绑定。
        // 如需实现彻底回滚，建议在 handleResetAction 中手动清空并根据 originalData 重新构建。
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
    public List<Node> getCustomControls() {
        return Collections.singletonList(customControlPane);
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

    @FXML
    private void handleAlgorithmStart() {
        // 转发给父类规范化的入口
        handleStartAction();
    }
}