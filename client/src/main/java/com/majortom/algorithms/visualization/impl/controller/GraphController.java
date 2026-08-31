package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.library.graph.GraphBfsInput;
import com.majortom.algorithms.library.graph.IntEdge;
import com.majortom.algorithms.library.graph.IntGraph;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.impl.visualizer.GraphVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.runtime.graph.GraphEventReducer;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;

public final class GraphController extends BaseModuleController<GraphViewState> {

    private IntGraph graph;
    private int startNode = 0;

    @FXML private Label structureLabel;
    @FXML private ComboBox<String> structureSelector;
    @FXML private Label algorithmLabel;
    @FXML private ComboBox<String> algorithmSelector;
    @FXML private Button runBtn;
    @FXML private Button operationBtn;

    public GraphController() {
        super(new GraphVisualizer(), "/fxml/GraphControls.fxml");
        graph = randomGraph(10, 16);
        renderGraph();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bindSelectors();
        EffectUtils.applyDynamicEffect(runBtn, operationBtn);
    }

    @Override
    @FXML
    public void handleAlgorithmStart() {
        if (isRunning()) {
            return;
        }
        if (!graph.nodes().contains(startNode)) {
            appendLog(I18N.text("message.error.invalid_graph_start", startNode));
            return;
        }
        dispatchVisualizerAction(
                com.majortom.algorithms.visualization.VisualizationActionType.GRAPH_RUN,
                java.util.Map.of("startNode", startNode));
        startAlgorithm("graph-bfs", new GraphBfsInput(graph, startNode), () -> new GraphEventReducer(graph));
    }

    @FXML
    private void openGraphOperationDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(I18N.text("dialog.graph.title"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        TextField nodeField = field(I18N.text("prompt.graph.node"));
        TextField fromField = field(I18N.text("prompt.graph.from"));
        TextField toField = field(I18N.text("prompt.graph.to"));
        TextField startField = field(I18N.text("prompt.graph.start"));
        startField.setText(String.valueOf(startNode));
        Button add = new Button(I18N.text("action.graph.add"));
        OperationDialogTheme.addClasses(add, "btn-ran-purple", "compact-button");
        add.setOnAction(event -> addNode(nodeField.getText()));
        Button delete = new Button(I18N.text("action.graph.delete"));
        OperationDialogTheme.addClasses(delete, "btn-ran-red", "compact-button");
        delete.setOnAction(event -> deleteNode(nodeField.getText()));
        Button link = new Button(I18N.text("action.graph.link"));
        OperationDialogTheme.addClasses(link, "btn-ran-gold", "compact-button");
        link.setOnAction(event -> linkNodes(fromField.getText(), toField.getText()));
        Button setStart = new Button(I18N.text("action.graph.set_start"));
        OperationDialogTheme.addClasses(setStart, "btn-ran-blue", "compact-button");
        setStart.setOnAction(event -> setStartNode(startField.getText()));
        HBox edgeFields = new HBox(10, fromField, toField);
        HBox.setHgrow(fromField, Priority.ALWAYS);
        HBox.setHgrow(toField, Priority.ALWAYS);
        dialog.getDialogPane().setContent(new VBox(16,
                formSection(I18N.text("label.graph.node_ops"), nodeField, new HBox(10, add, delete)),
                formSection(I18N.text("label.graph.edge_ops"), edgeFields, new HBox(10, link)),
                formSection(I18N.text("label.graph.run_start"), startField, new HBox(10, setStart))));
        OperationDialogTheme.apply(dialog, 680.0d);
        dialog.showAndWait();
    }

    private TextField field(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        OperationDialogTheme.addClasses(field, "dark-textfield", "dialog-input");
        return field;
    }

    private VBox formSection(String title, javafx.scene.Node fields, javafx.scene.Node actions) {
        Label titleLabel = new Label(title);
        OperationDialogTheme.addClasses(titleLabel, "dialog-section-title");
        OperationDialogTheme.addClasses(actions, "dialog-action-row");
        VBox section = new VBox(8, titleLabel, fields, actions);
        OperationDialogTheme.addClasses(section, "dialog-form-section");
        return section;
    }

    private void addNode(String text) {
        Integer id = parseNode(text);
        if (id == null || graph.nodes().contains(id)) return;
        List<Integer> nodes = new ArrayList<>(graph.nodes());
        nodes.add(id);
        invalidateExecutionForInputChange();
        graph = new IntGraph(nodes, graph.edges());
        renderGraph();
        dispatchVisualizerAction(
                com.majortom.algorithms.visualization.VisualizationActionType.GRAPH_ADD_NODE,
                java.util.Map.of("node", id));
        logI18n("message.graph.node_added", id);
    }

    private void deleteNode(String text) {
        Integer id = parseNode(text);
        if (id == null || !graph.nodes().contains(id)) return;
        List<Integer> nodes = new ArrayList<>(graph.nodes());
        nodes.remove(id);
        List<IntEdge> edges = graph.edges().stream()
                .filter(edge -> edge.from() != id && edge.to() != id)
                .toList();
        invalidateExecutionForInputChange();
        graph = new IntGraph(nodes, edges);
        if (!nodes.contains(startNode) && !nodes.isEmpty()) startNode = nodes.getFirst();
        renderGraph();
        dispatchVisualizerAction(
                com.majortom.algorithms.visualization.VisualizationActionType.GRAPH_DELETE_NODE,
                java.util.Map.of("node", id));
        logI18n("message.graph.node_deleted", id);
    }

    private void linkNodes(String fromText, String toText) {
        Integer from = parseNode(fromText);
        Integer to = parseNode(toText);
        if (from == null || to == null || !graph.nodes().contains(from) || !graph.nodes().contains(to)) {
            logI18n("message.error.graph_node_missing");
            return;
        }
        List<IntEdge> edges = new ArrayList<>(graph.edges());
        IntEdge edge = new IntEdge(from, to);
        if (edges.contains(edge)) {
            return;
        }
        edges.add(edge);
        invalidateExecutionForInputChange();
        graph = new IntGraph(graph.nodes(), edges);
        renderGraph();
        dispatchVisualizerAction(
                com.majortom.algorithms.visualization.VisualizationActionType.GRAPH_LINK,
                java.util.Map.of("from", from, "to", to));
        logI18n("message.graph.link.directed", from, to, 1);
    }

    private void setStartNode(String text) {
        Integer id = parseNode(text);
        if (id == null || !graph.nodes().contains(id)) {
            appendLog(I18N.text("message.error.invalid_graph_start", text));
            return;
        }
        if (startNode != id) {
            invalidateExecutionForInputChange();
            startNode = id;
        }
        appendLog(I18N.text("message.graph.start_set", id));
    }

    private Integer parseNode(String text) {
        try {
            return Integer.valueOf(text.trim());
        } catch (RuntimeException exception) {
            logI18n("message.error.graph_parse");
            return null;
        }
    }

    private void renderGraph() {
        renderViewState(GraphViewState.initial(graph));
    }

    @Override
    protected String formatStatsMessage() {
        return String.format("%s | %s | %s",
                I18N.text("stats.graph.nodes", graph.nodes().size()),
                I18N.text("stats.graph.edges", graph.edges().size()),
                formatMetric("stats.action", stats.metric("nodes.visited")
                        + stats.metric("edges.examined")));
    }

    @Override
    protected void onResetData() {
        renderGraph();
    }

    @Override
    protected void setupI18n() {
        if (structureLabel != null) structureLabel.textProperty().bind(I18N.createStringBinding("label.common.structure"));
        if (algorithmLabel != null) algorithmLabel.textProperty().bind(I18N.createStringBinding("label.common.algorithm"));
        if (runBtn != null) runBtn.textProperty().bind(I18N.createStringBinding("action.graph.run"));
        if (operationBtn != null) operationBtn.textProperty().bind(I18N.createStringBinding("action.graph.operation"));
    }

    @Override
    protected String moduleId() {
        return "graph";
    }

    private void bindSelectors() {
        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> FXCollections.observableArrayList(I18N.text("label.graph.structure.directed")),
                I18N.localeProperty()));
        algorithmSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> FXCollections.observableArrayList(I18N.text(AlgorithmLabels.key("graph-bfs"))),
                I18N.localeProperty()));
        Platform.runLater(() -> {
            structureSelector.getSelectionModel().selectFirst();
            algorithmSelector.getSelectionModel().selectFirst();
        });
    }

    private IntGraph randomGraph(int nodeCount, int edgeCount) {
        List<Integer> nodes = new ArrayList<>();
        for (int node = 0; node < nodeCount; node++) nodes.add(node);
        Random random = new Random();
        Set<IntEdge> edges = new LinkedHashSet<>();
        for (int node = 1; node < nodeCount; node++) edges.add(new IntEdge(node - 1, node));
        while (edges.size() < edgeCount) {
            int from = random.nextInt(nodeCount);
            int to = random.nextInt(nodeCount);
            if (from != to) edges.add(new IntEdge(from, to));
        }
        return new IntGraph(nodes, List.copyOf(edges));
    }
}
