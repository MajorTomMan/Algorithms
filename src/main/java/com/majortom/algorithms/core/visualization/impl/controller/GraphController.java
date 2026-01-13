package com.majortom.algorithms.core.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.BaseGraphAlgorithms;
import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.impl.visualizer.GraphVisualizer;
import com.majortom.algorithms.core.visualization.international.I18N;

import javafx.application.Platform;
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
 * 图算法控制器 (重构版)
 * 职责：适配 BaseGraph 实体，处理起点选择，并确保 GraphStream 渲染与算法步进同步。
 * * @param <V> 节点存储的数据类型
 */
public class GraphController<V> extends BaseController<BaseGraph<V>> {

    private final BaseGraphAlgorithms<V> algorithm;
    private Node customControlPane;

    @FXML
    private TextField nodeInputField;
    @FXML
    private Label startNodeLabel; // 对应 FXML 中的 %ctrl.graph.start_id
    @FXML
    private Button runBtn; // 对应 FXML 中的 %btn.graph.run
    @FXML
    private Button resetBtn; // 对应 FXML 中的 %btn.reset

    /**
     * 构造函数
     * 
     * @param algorithm 具体的图算法逻辑（如 BFS, Dijkstra）
     * @param graphData 图数据实体容器
     */
    public GraphController(BaseGraphAlgorithms<V> algorithm, BaseGraph<V> graphData) {
        // 🚩 修正：泛型对齐为 BaseGraph<V>，它是 BaseStructure 的子类
        super(new GraphVisualizer<>(graphData));
        this.algorithm = algorithm;
        loadFXMLControls();
    }

    private void loadFXMLControls() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GraphControls.fxml"));

            loader.setResources(I18N.getBundle());
            loader.setController(this);
            this.customControlPane = loader.load();

            setupI18n();

        } catch (IOException e) {
            System.err.println("[Error] GraphControls.fxml load failed: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
    }

    @Override
    public List<Node> getCustomControls() {
        return Collections.singletonList(customControlPane);
    }

    @Override
    public void handleAlgorithmStart() {
        // 🚩 这里的 visualizer.getLastData() 获取的就是构造时传入的 BaseGraph 实例
        if (visualizer.getLastData() != null) {
            startAlgorithm(algorithm, visualizer.getLastData());
        }
    }

    @FXML
    private void handleReset() {
        stopAlgorithm();

        BaseGraph<V> g = visualizer.getLastData();
        if (g != null) {
            g.reset(); // 利用基类 reset 清理统计量
            // 清理 GraphStream 特有的样式属性
            g.getGraph().nodes().forEach(n -> n.removeAttribute("ui.class"));
            g.getGraph().edges().forEach(e -> e.removeAttribute("ui.class"));
        }

        if (logArea != null)
            logArea.clear();
        if (statsLabel != null)
            statsLabel.setText("Status: Ready");
    }

    // --- 算法执行逻辑适配 ---

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseGraph<V>> alg, BaseGraph<V> data) {
        // 1. 获取 UI 输入
        String startNodeId = nodeInputField.getText().trim();

        // 2. 验证合法性（检查 GraphStream 节点是否存在）
        if (startNodeId.isEmpty() || data.getGraph().getNode(startNodeId) == null) {
            Platform.runLater(() -> {
                if (logArea != null)
                    logArea.appendText("System Error: Node [" + startNodeId + "] not found.\n");
            });
            return;
        }

        // 3. 执行算法
        // 由于 BaseGraphAlgorithms 继承了 BaseAlgorithms<BaseGraph<V>>
        // 且它必须实现 run(BaseGraph<V> data, String startId)
        if (alg instanceof BaseGraphAlgorithms) {
            ((BaseGraphAlgorithms<V>) alg).run(data, startNodeId);
        }
    }

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        if (statsLabel != null) {
            statsLabel.setText(String.format("Steps: %d | Compares: %d", actionCount, compareCount));
        }
    }

    @Override
    protected void onAlgorithmFinished() {
        super.onAlgorithmFinished();
        // 可以在这里执行一些收尾的视觉效果，比如全图闪烁一下
    }

    @Override
    protected void setupI18n() {
        // 1. 标签文本绑定：控制“起点ID”文字
        if (startNodeLabel != null) {
            startNodeLabel.textProperty().bind(I18N.createStringBinding("ctrl.graph.start_id"));
        }

        // 2. 输入框提示词绑定：让用户知道该输什么（如 "输入节点 A"）
        if (nodeInputField != null) {
            nodeInputField.promptTextProperty().bind(I18N.createStringBinding("ctrl.graph.prompt"));
        }

        // 3. 执行按钮绑定：点击“运行”或“Run”
        if (runBtn != null) {
            runBtn.textProperty().bind(I18N.createStringBinding("btn.graph.run"));
        }

        // 4. 重置按钮绑定：统一全局的“重置”字样
        if (resetBtn != null) {
            resetBtn.textProperty().bind(I18N.createStringBinding("btn.reset"));
        }
    }
}