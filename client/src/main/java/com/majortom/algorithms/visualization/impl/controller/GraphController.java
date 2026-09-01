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
import com.majortom.algorithms.visualization.structure.StructureSnapshot;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

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
    @FXML private Label executionSectionLabel;
    @FXML private ComboBox<String> algorithmSelector;
    @FXML private Label nodeOperationsLabel;
    @FXML private Label edgeOperationsLabel;
    @FXML private Label traversalLabel;
    @FXML private TextField nodeField;
    @FXML private TextField findNodeField;
    @FXML private TextField fromField;
    @FXML private TextField toField;
    @FXML private TextField startField;
    @FXML private Button addNodeBtn;
    @FXML private Button deleteNodeBtn;
    @FXML private Button findNodeBtn;
    @FXML private Button addEdgeBtn;
    @FXML private Button deleteEdgeBtn;
    @FXML private Button setStartBtn;
    @FXML private Button runBtn;

    public GraphController() {
        super(new GraphVisualizer(), "/fxml/GraphControls.fxml");
        graph = randomGraph(10, 16);
        renderGraph();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bindSelectors();
        startField.setText(String.valueOf(startNode));
        EffectUtils.applyDynamicEffect(
                addNodeBtn, deleteNodeBtn, findNodeBtn, addEdgeBtn,
                deleteEdgeBtn, setStartBtn, runBtn);
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
    private void handleAddNode() {
        addNode(nodeField.getText());
    }

    @FXML
    private void handleDeleteNode() {
        deleteNode(nodeField.getText());
    }

    @FXML
    private void handleFindNode() {
        if (isRunning()) {
            logI18n("message.error.operation_running");
            return;
        }
        Integer id = parseNode(findNodeField.getText());
        if (id == null) {
            return;
        }
        if (!graph.nodes().contains(id)) {
            logI18n("message.graph.not_found", id);
            return;
        }
        renderViewState(new GraphViewState(
                graph, Set.of(), Set.of(), List.of(), java.util.Map.of(), id, null,
                GraphViewState.Phase.VISITING, false));
        logI18n("message.graph.found", id);
    }

    @FXML
    private void handleAddEdge() {
        linkNodes(fromField.getText(), toField.getText());
    }

    @FXML
    private void handleDeleteEdge() {
        Integer from = parseNode(fromField.getText());
        Integer to = parseNode(toField.getText());
        if (from == null || to == null) {
            return;
        }
        if (!graph.nodes().contains(from) || !graph.nodes().contains(to)) {
            logI18n("message.error.graph_node_missing");
            return;
        }
        List<IntEdge> edges = new ArrayList<>(graph.edges());
        boolean removed = edges.removeIf(edge -> edge.from() == from && edge.to() == to);
        if (!removed) {
            logI18n("message.graph.edge_not_found", from, to);
            return;
        }
        invalidateExecutionForInputChange();
        graph = new IntGraph(graph.nodes(), edges);
        renderGraph();
        logI18n("message.graph.edge_deleted", from, to);
    }

    @FXML
    private void handleSetStart() {
        setStartNode(startField.getText());
    }

    private void addNode(String text) {
        Integer id = parseNode(text);
        if (id == null) {
            return;
        }
        if (graph.nodes().contains(id)) {
            logI18n("message.graph.already_exists", id);
            return;
        }
        boolean wasEmpty = graph.nodes().isEmpty();
        List<Integer> nodes = new ArrayList<>(graph.nodes());
        nodes.add(id);
        invalidateExecutionForInputChange();
        graph = new IntGraph(nodes, graph.edges());
        if (wasEmpty) {
            startNode = id;
            startField.setText(String.valueOf(startNode));
        }
        renderGraph();
        dispatchVisualizerAction(
                com.majortom.algorithms.visualization.VisualizationActionType.GRAPH_ADD_NODE,
                java.util.Map.of("node", id));
        logI18n("message.graph.node_added", id);
    }

    private void deleteNode(String text) {
        Integer id = parseNode(text);
        if (id == null) {
            return;
        }
        if (!graph.nodes().contains(id)) {
            logI18n("message.graph.not_found", id);
            return;
        }
        List<Integer> nodes = new ArrayList<>(graph.nodes());
        nodes.remove(id);
        List<IntEdge> edges = graph.edges().stream()
                .filter(edge -> edge.from() != id && edge.to() != id)
                .toList();
        invalidateExecutionForInputChange();
        graph = new IntGraph(nodes, edges);
        if (nodes.isEmpty()) {
            startNode = 0;
            startField.clear();
        } else if (!nodes.contains(startNode)) {
            startNode = nodes.getFirst();
            startField.setText(String.valueOf(startNode));
        }
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
            logI18n("message.graph.edge_exists", from, to);
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
            renderGraph();
        }
        appendLog(I18N.text("message.graph.start_set", id));
    }

    private Integer parseNode(String text) {
        try {
            return Integer.valueOf(text.trim());
        } catch (RuntimeException exception) {
            logI18n("message.error.invalid_graph_value");
            return null;
        }
    }

    private void renderGraph() {
        renderStructureState(GraphViewState.initial(graph));
    }

    @Override
    public StructureSnapshot<GraphViewState> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), GraphViewState.initial(graph));
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<GraphViewState> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        graph = snapshot.state().graph();
        if (!graph.nodes().contains(startNode)) {
            startNode = 0;
            if (!graph.nodes().isEmpty()) {
                startNode = graph.nodes().getFirst();
            }
        }
        if (startField != null) {
            startField.setText(String.valueOf(startNode));
        }
        invalidateExecutionForInputChange();
        renderGraph();
        refreshStatsDisplay();
    }

    @Override
    public String describeStructureSnapshot(GraphViewState state) {
        return I18N.text("snapshot.graph.detail", state.graph().nodes().size(), state.graph().edges().size());
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
        if (executionSectionLabel != null) {
            executionSectionLabel.textProperty().bind(I18N.createStringBinding("label.panel.execution"));
        }
        bindLabel(nodeOperationsLabel, "label.graph.node_ops");
        bindLabel(edgeOperationsLabel, "label.graph.edge_ops");
        bindLabel(traversalLabel, "label.graph.run_start");
        bindPrompt(nodeField, "prompt.graph.node");
        bindPrompt(findNodeField, "prompt.graph.node");
        bindPrompt(fromField, "prompt.graph.from");
        bindPrompt(toField, "prompt.graph.to");
        bindPrompt(startField, "prompt.graph.start");
        bindButton(addNodeBtn, "action.graph.add");
        bindButton(deleteNodeBtn, "action.graph.delete");
        bindButton(findNodeBtn, "action.graph.find");
        bindButton(addEdgeBtn, "action.graph.link");
        bindButton(deleteEdgeBtn, "action.graph.delete_edge");
        bindButton(setStartBtn, "action.graph.set_start");
        bindButton(runBtn, "action.graph.run");
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

    private void bindLabel(Label label, String key) {
        if (label != null) {
            label.textProperty().bind(I18N.createStringBinding(key));
        }
    }

    private void bindPrompt(TextField field, String key) {
        if (field != null) {
            field.promptTextProperty().bind(I18N.createStringBinding(key));
        }
    }

    private void bindButton(Button button, String key) {
        if (button != null) {
            button.textProperty().bind(I18N.createStringBinding(key));
        }
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
