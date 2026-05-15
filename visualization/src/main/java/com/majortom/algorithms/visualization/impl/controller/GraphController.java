package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.BaseGraphAlgorithms;
import com.majortom.algorithms.core.graph.impl.DirectedGraph;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmDescriptor;
import com.majortom.algorithms.visualization.algorithm.AlgorithmFamily;
import com.majortom.algorithms.visualization.algorithm.AlgorithmRegistry;
import com.majortom.algorithms.visualization.algorithm.AlgorithmStructure;
import com.majortom.algorithms.visualization.impl.visualizer.GraphVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 图算法模块控制器。
 *
 * <p>底栏只保留结构、算法和操作入口；新增节点、删除节点、连边和遍历起点设置
 * 统一收口到操作弹窗，避免默认界面被多个输入框挤满。</p>
 */
public class GraphController<V> extends BaseModuleController<BaseGraph<V>> {

    /**
     * 当前图结构。
     */
    private AlgorithmStructure currentStructure = AlgorithmStructure.DIRECTED_GRAPH;

    /**
     * 当前执行记录使用的稳定算法 ID。
     */
    private String currentAlgorithmId;

    /**
     * 当前遍历起点。
     */
    private String currentStartNodeId = "A";

    @FXML
    private Label structureLabel;
    @FXML
    private ComboBox<String> structureSelector;
    @FXML
    private Label algorithmLabel;
    @FXML
    private ComboBox<String> algorithmSelector;
    @FXML
    private Button runBtn;
    @FXML
    private Button operationBtn;

    /**
     * 创建图模块控制器。
     *
     * @param algorithm 默认算法实例；保留构造签名兼容模块注册，实际下拉由注册表驱动
     * @param graphData 图结构实体
     */
    public GraphController(BaseGraphAlgorithms<V> algorithm, BaseGraph<V> graphData) {
        super(new GraphVisualizer<>(graphData), "/fxml/GraphControls.fxml");

        if (graphData != null) {
            visualizer.render(graphData);
        }
    }

    /**
     * 初始化结构与算法下拉框。
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bindStructureSelector();
        bindAlgorithmSelector();
        EffectUtils.applyDynamicEffect(runBtn, operationBtn);
    }

    /**
     * 响应全局 Start 和局部运行按钮。
     */
    @Override
    public void handleAlgorithmStart() {
        BaseGraph<V> currentData = visualizer.getLastData();
        BaseGraphAlgorithms<V> algorithm = selectedAlgorithm();

        if (currentData != null && algorithm != null && !AlgorithmThreadManager.isRunning()) {
            startAlgorithm(algorithm, currentData);
        }
    }

    /**
     * 打开图操作弹窗。
     */
    @FXML
    private void openGraphOperationDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(I18N.text("dialog.graph.title"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TextField nodeField = new TextField();
        nodeField.setPromptText(I18N.text("prompt.graph.node"));
        nodeField.getStyleClass().addAll("dark-textfield", "dialog-input");

        TextField fromField = new TextField();
        fromField.setPromptText(I18N.text("prompt.graph.from"));
        fromField.getStyleClass().addAll("dark-textfield", "dialog-input");

        TextField toField = new TextField();
        toField.setPromptText(I18N.text("prompt.graph.to"));
        toField.getStyleClass().addAll("dark-textfield", "dialog-input");

        TextField weightField = new TextField("1");
        weightField.setPromptText(I18N.text("prompt.graph.weight"));
        weightField.getStyleClass().addAll("dark-textfield", "dialog-input");

        TextField startField = new TextField(currentStartNodeId);
        startField.setPromptText(I18N.text("prompt.graph.start"));
        startField.getStyleClass().addAll("dark-textfield", "dialog-input");

        Button addNodeButton = new Button(I18N.text("action.graph.add"));
        addNodeButton.getStyleClass().addAll("btn-ran-purple", "compact-button");
        addNodeButton.setOnAction(event -> addNode(nodeField.getText()));

        Button deleteNodeButton = new Button(I18N.text("action.graph.delete"));
        deleteNodeButton.getStyleClass().addAll("btn-ran-red", "compact-button");
        deleteNodeButton.setOnAction(event -> deleteNode(nodeField.getText()));

        Button linkButton = new Button(I18N.text("action.graph.link"));
        linkButton.getStyleClass().addAll("btn-ran-gold", "compact-button");
        linkButton.setOnAction(event -> linkNodes(fromField.getText(), toField.getText(), weightField.getText()));

        Button setStartButton = new Button(I18N.text("action.graph.set_start"));
        setStartButton.getStyleClass().addAll("btn-ran-blue", "compact-button");
        setStartButton.setOnAction(event -> setStartNode(startField.getText()));

        HBox nodeButtons = new HBox(10, addNodeButton, deleteNodeButton);
        nodeButtons.getStyleClass().add("dialog-action-row");

        HBox edgeFields = new HBox(10, fromField, toField, weightField);
        HBox.setHgrow(fromField, Priority.ALWAYS);
        HBox.setHgrow(toField, Priority.ALWAYS);
        HBox.setHgrow(weightField, Priority.ALWAYS);

        VBox content = new VBox(16,
                formSection(I18N.text("label.graph.node_ops"), nodeField, nodeButtons),
                formSection(I18N.text("label.graph.edge_ops"), edgeFields, new HBox(10, linkButton)),
                formSection(I18N.text("label.graph.run_start"), startField, new HBox(10, setStartButton)));
        content.getStyleClass().add("operation-dialog-content");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style/ui_theme.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("operation-dialog-pane");
        dialog.getDialogPane().setPrefWidth(680);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    /**
     * 创建操作弹窗中的表单分区。
     */
    private VBox formSection(String title, javafx.scene.Node fields, javafx.scene.Node actions) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-section-title");
        VBox section = new VBox(8, titleLabel, fields, actions);
        section.getStyleClass().add("dialog-form-section");
        return section;
    }

    /**
     * 算法核心执行逻辑。
     */
    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseGraph<V>> alg, BaseGraph<V> data) {
        if (currentStartNodeId == null ||
                currentStartNodeId.isBlank() ||
                data.getGraph().getNode(currentStartNodeId) == null) {
            appendLog(I18N.text("message.error.invalid_graph_start", currentStartNodeId));
            return;
        }

        if (alg instanceof BaseGraphAlgorithms) {
            ((BaseGraphAlgorithms<V>) alg).run(data, currentStartNodeId);
        }
    }

    /**
     * 格式化统计信息：图算法通常关注访问节点数和边数。
     */
    @Override
    protected String formatStatsMessage() {
        BaseGraph<V> data = visualizer.getLastData();
        if (data == null) {
            return I18N.text("stats.graph.empty");
        }
        return String.format("%s | %s | %s",
                I18N.text("stats.graph.nodes", data.getGraph().getNodeCount()),
                I18N.text("stats.graph.edges", data.getGraph().getEdgeCount()),
                formatMetric("stats.action", stats.actionCount()));
    }

    /**
     * 算法结束后输出日志。
     */
    @Override
    protected void onAlgorithmFinished() {
        super.onAlgorithmFinished();
        logI18n("message.execution.finished");
    }

    /**
     * 重置图的高亮状态。
     */
    @Override
    protected void onResetData() {
        BaseGraph<V> graph = visualizer.getLastData();
        if (graph != null) {
            graph.resetStatistics();
            graph.getGraph().nodes().forEach(node -> {
                node.removeAttribute("ui.class");
                node.setAttribute("ui.label", node.getId());
            });
            graph.getGraph().edges().forEach(edge -> edge.removeAttribute("ui.class"));
            visualizer.render(graph, null, null);
        }
    }

    /**
     * 绑定国际化文案。
     */
    @Override
    protected void setupI18n() {
        if (structureLabel != null) {
            structureLabel.textProperty().bind(I18N.createStringBinding("label.common.structure"));
        }
        if (algorithmLabel != null) {
            algorithmLabel.textProperty().bind(I18N.createStringBinding("label.common.algorithm"));
        }
        if (runBtn != null) {
            runBtn.textProperty().bind(I18N.createStringBinding("action.graph.run"));
        }
        if (operationBtn != null) {
            operationBtn.textProperty().bind(I18N.createStringBinding("action.graph.operation"));
        }
    }

    /**
     * 获取模块 ID。
     */
    @Override
    protected String moduleId() {
        return "graph";
    }

    /**
     * 返回当前选中算法的稳定 ID。
     */
    @Override
    protected String executionAlgorithmId(BaseAlgorithms<BaseGraph<V>> algorithm) {
        return currentAlgorithmId == null ? super.executionAlgorithmId(algorithm) : currentAlgorithmId;
    }

    /**
     * 绑定结构下拉框。
     */
    private void bindStructureSelector() {
        if (structureSelector == null) {
            return;
        }

        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> list = FXCollections.observableArrayList();
            list.add(I18N.text("label.graph.structure.directed"));
            return list;
        }, I18N.localeProperty()));

        Platform.runLater(() -> structureSelector.getSelectionModel().selectFirst());
    }

    /**
     * 绑定算法下拉框。
     */
    private void bindAlgorithmSelector() {
        if (algorithmSelector == null) {
            return;
        }

        algorithmSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> list = FXCollections.observableArrayList();
            graphOptions().forEach(option -> list.add(I18N.text(option.labelKey())));
            return list;
        }, I18N.localeProperty()));

        Platform.runLater(() -> algorithmSelector.getSelectionModel().selectFirst());
    }

    /**
     * 查询当前图结构下可用算法。
     */
    private List<AlgorithmDescriptor> graphOptions() {
        return AlgorithmRegistry.find(moduleId(), AlgorithmFamily.GRAPH_TRAVERSAL, currentStructure);
    }

    /**
     * 创建当前选中的图算法。
     */
    @SuppressWarnings("unchecked")
    private BaseGraphAlgorithms<V> selectedAlgorithm() {
        List<AlgorithmDescriptor> options = graphOptions();
        int index = algorithmSelector == null ? 0 : algorithmSelector.getSelectionModel().getSelectedIndex();

        if (index < 0 || index >= options.size()) {
            currentAlgorithmId = null;
            return null;
        }

        AlgorithmDescriptor option = options.get(index);
        currentAlgorithmId = option.id();
        return (BaseGraphAlgorithms<V>) option.create();
    }

    /**
     * 添加节点。
     */
    private void addNode(String nodeId) {
        String id = normalize(nodeId);
        BaseGraph<V> data = visualizer.getLastData();
        if (id.isEmpty() || data == null || data.getGraph().getNode(id) != null) {
            return;
        }

        org.graphstream.graph.Node node = data.getGraph().addNode(id);
        node.setAttribute("ui.label", id);
        visualizer.render(data, null, null);
        logI18n("message.graph.node_added", id);
    }

    /**
     * 删除节点。
     */
    private void deleteNode(String nodeId) {
        String id = normalize(nodeId);
        BaseGraph<V> data = visualizer.getLastData();
        if (id.isEmpty() || data == null || data.getGraph().getNode(id) == null) {
            return;
        }

        data.getGraph().removeNode(id);
        visualizer.render(data, null, null);
        logI18n("message.graph.node_deleted", id);
    }

    /**
     * 建立边关系。
     */
    private void linkNodes(String fromText, String toText, String weightText) {
        String from = normalize(fromText);
        String to = normalize(toText);
        BaseGraph<V> data = visualizer.getLastData();
        if (from.isEmpty() || to.isEmpty() || data == null) {
            return;
        }

        try {
            int weight = normalize(weightText).isEmpty() ? 1 : Integer.parseInt(normalize(weightText));
            if (data.getGraph().getNode(from) != null && data.getGraph().getNode(to) != null) {
                data.addEdge(from, to, weight);
                visualizer.render(data, null, null);
                String key = data instanceof DirectedGraph
                        ? "message.graph.link.directed"
                        : "message.graph.link.undirected";
                logI18n(key, from, to, weight);
            } else {
                logI18n("message.error.graph_node_missing");
            }
        } catch (NumberFormatException e) {
            logI18n("message.error.graph_parse");
        }
    }

    /**
     * 设置遍历起点。
     */
    private void setStartNode(String nodeId) {
        String id = normalize(nodeId);
        BaseGraph<V> data = visualizer.getLastData();
        if (id.isEmpty() || data == null || data.getGraph().getNode(id) == null) {
            appendLog(I18N.text("message.error.invalid_graph_start", id));
            return;
        }

        currentStartNodeId = id;
        appendLog(I18N.text("message.graph.start_set", id));
    }

    /**
     * 清理用户输入。
     */
    private String normalize(String text) {
        return text == null ? "" : text.trim();
    }
}
