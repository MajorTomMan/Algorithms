package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.library.basic.graph.Edge;
import com.majortom.algorithms.library.basic.graph.Vertex;
import com.majortom.algorithms.library.graph.GraphBfs;
import com.majortom.algorithms.library.graph.GraphTraversal;
import com.majortom.algorithms.library.basic.graph.Graph;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.impl.visualizer.GraphVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import com.majortom.algorithms.visualization.structure.SnapshotAlgorithmInputSupport;
import com.majortom.algorithms.visualization.runtime.graph.GraphEventReducer;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
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

public final class GraphController extends BaseModuleController<GraphViewState>
        implements AlgorithmSelectionSupport, StructureSnapshotSupport<GraphSnapshot<Integer>>, SnapshotAlgorithmInputSupport<GraphSnapshot<Integer>> {

    private final List<String> algorithmIds = AlgorithmCatalog.graphTraversals();
    private Graph<Integer> graph;
    private StructureSnapshot<GraphSnapshot<Integer>> algorithmInputSnapshot;
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
        graph = randomGraph(10, 16, false);
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
        StructureSnapshot<GraphSnapshot<Integer>> selectedStructureSnapshot =
                algorithmInputSnapshot == null ? captureStructureSnapshot() : algorithmInputSnapshot;
        GraphSnapshot<Integer> selected = selectedStructureSnapshot.state();
        Graph<Integer> inputGraph = mutableGraph(selected);
        if (inputGraph.isEmpty()) return;
        int algorithmStartNode = inputGraph.vertex(startNode) != null
                ? startNode : firstVertexValue(inputGraph);
        String algorithmId = selectedAlgorithmId();
        if (algorithmId == null) {
            return;
        }
        GraphSnapshot<Integer> inputSnapshot = GraphBfs.snapshot(inputGraph);
        @SuppressWarnings("unchecked")
        GraphTraversal<Integer> algorithm = (GraphTraversal<Integer>)
                module("algorithm.graph.Integer." + algorithmId, GraphTraversal.class);
        startAlgorithm(algorithmId, inputSnapshot, () -> algorithm.traverse(inputGraph, algorithmStartNode),
                () -> new GraphEventReducer(inputSnapshot));
    }

    @Override
    public boolean selectAlgorithm(String algorithmId) {
        int index = algorithmIds.indexOf(algorithmId);
        if (index < 0) {
            return false;
        }
        if (algorithmSelector != null) {
            algorithmSelector.getSelectionModel().select(index);
        }
        return true;
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
        if (graph.vertex(id) == null) {
            logI18n("message.graph.not_found", id);
            return;
        }
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
        if (graph.vertex(from) == null || graph.vertex(to) == null) {
            logI18n("message.error.graph_node_missing");
            return;
        }
        if (!graph.containsEdge(graph.vertex(from), graph.vertex(to))) {
            logI18n("message.graph.edge_not_found", from, to);
            return;
        }
        if (!executeStructureOperation("remove-edge", () -> graph.removeEdge(graph.vertex(from), graph.vertex(to)))) {
            return;
        }
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
        if (graph.vertex(id) != null) {
            logI18n("message.graph.already_exists", id);
            return;
        }
        boolean wasEmpty = graph.isEmpty();
        if (!executeStructureOperation("add-vertex", () -> {
            graph.addVertex(id);
            return null;
        })) {
            return;
        }
        if (wasEmpty) {
            startNode = id;
            startField.setText(String.valueOf(startNode));
        }
        renderGraph();
        logI18n("message.graph.node_added", id);
    }

    private void deleteNode(String text) {
        Integer id = parseNode(text);
        if (id == null) {
            return;
        }
        if (graph.vertex(id) == null) {
            logI18n("message.graph.not_found", id);
            return;
        }
        if (!executeStructureOperation("remove-vertex", () -> graph.removeVertex(graph.vertex(id)))) {
            return;
        }
        if (graph.isEmpty()) {
            startNode = 0;
            startField.clear();
        } else if (graph.vertex(startNode) == null) {
            startNode = firstVertexValue(graph);
            startField.setText(String.valueOf(startNode));
        }
        renderGraph();
        logI18n("message.graph.node_deleted", id);
    }

    private void linkNodes(String fromText, String toText) {
        Integer from = parseNode(fromText);
        Integer to = parseNode(toText);
        if (from == null || to == null || graph.vertex(from) == null || graph.vertex(to) == null) {
            logI18n("message.error.graph_node_missing");
            return;
        }
        if (graph.containsEdge(graph.vertex(from), graph.vertex(to))) {
            logI18n("message.graph.edge_exists", from, to);
            return;
        }
        if (!executeStructureOperation("add-edge", () -> {
            graph.addEdge(graph.vertex(from), graph.vertex(to));
            return null;
        })) {
            return;
        }
        renderGraph();
        logI18n(graph.isDirected() ? "message.graph.link.directed" : "message.graph.link.undirected", from, to, 1);
    }

    private void setStartNode(String text) {
        Integer id = parseNode(text);
        if (id == null || graph.vertex(id) == null) {
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
        renderStructureState(GraphViewState.initial(GraphBfs.snapshot(graph)));
    }

    @Override
    public StructureSnapshot<GraphSnapshot<Integer>> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), graphSnapshot());
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<GraphSnapshot<Integer>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        graph = mutableGraph(snapshot.state());
        if (graph.vertex(startNode) == null) {
            startNode = 0;
            if (!graph.isEmpty()) {
                startNode = firstVertexValue(graph);
            }
        }
        if (startField != null) {
            startField.setText(String.valueOf(startNode));
        }
        invalidateExecutionForStructureChange();
        renderGraph();
        refreshStatsDisplay();
    }

    @Override
    public void useSnapshotAsAlgorithmInput(StructureSnapshot<GraphSnapshot<Integer>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        algorithmInputSnapshot = snapshot;
        invalidateExecutionForInputChange();
    }

    @Override
    public void useCurrentStructureAsAlgorithmInput() {
        algorithmInputSnapshot = null;
        invalidateExecutionForInputChange();
    }

    @Override
    public String algorithmInputSnapshotId() {
        return algorithmInputSnapshot == null ? null : algorithmInputSnapshot.id();
    }

    @Override
    protected boolean algorithmInputTracksCurrentStructure() {
        return algorithmInputSnapshot == null;
    }


    @Override
    protected void restoreAlgorithmState() {
        if (latestViewState() != null) {
            super.restoreAlgorithmState();
            return;
        }
        GraphSnapshot<Integer> selected = algorithmInputSnapshot == null
                ? graphSnapshot() : algorithmInputSnapshot.state();
        renderViewState(GraphViewState.initial(GraphBfs.snapshot(mutableGraph(selected))));
    }


    @Override
    public String describeStructureSnapshot(GraphSnapshot<Integer> state) {
        return I18N.text("snapshot.graph.detail", state.vertices().size(), state.edges().size());
    }

    @Override
    protected String formatStatsMessage() {
        return String.format("%s | %s | %s",
                I18N.text("stats.graph.nodes", graph.vertexCount()),
                I18N.text("stats.graph.edges", GraphBfs.snapshot(graph).edges().size()),
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

    private String selectedAlgorithmId() {
        int index = algorithmSelector == null ? 0 : algorithmSelector.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            index = 0;
        }
        return algorithmIds.isEmpty() ? null : algorithmIds.get(Math.min(index, algorithmIds.size() - 1));
    }

    private void bindSelectors() {
        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> FXCollections.observableArrayList(I18N.text("label.graph.structure.undirected")),
                I18N.localeProperty()));
        algorithmSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            javafx.collections.ObservableList<String> labels = FXCollections.observableArrayList();
            for (String id : algorithmIds) {
                labels.add(AlgorithmLabels.text(id));
            }
            return labels;
        }, I18N.localeProperty()));
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

    private Graph<Integer> randomGraph(int nodeCount, int edgeCount, boolean directed) {
        Graph<Integer> result = new Graph<>(directed);
        for (int node = 0; node < nodeCount; node++) {
            result.addVertex(node);
        }
        Random random = new Random(0x5EEDL);
        Set<String> edges = new LinkedHashSet<>();
        for (int node = 1; node < nodeCount; node++) {
            edges.add((node - 1) + ":" + node);
        }
        while (edges.size() < edgeCount) {
            int from = random.nextInt(nodeCount);
            int to = random.nextInt(nodeCount);
            if (from == to) {
                continue;
            }
            String key = directed || from < to ? from + ":" + to : to + ":" + from;
            edges.add(key);
        }
        for (String edge : edges) {
            String[] parts = edge.split(":", 2);
            int from = Integer.parseInt(parts[0]);
            int to = Integer.parseInt(parts[1]);
            result.addEdge(result.vertex(from), result.vertex(to));
        }
        return result;
    }

    private GraphSnapshot<Integer> graphSnapshot() {
        return GraphBfs.snapshot(graph);
    }

    private Graph<Integer> mutableGraph(GraphSnapshot<Integer> snapshot) {
        return Graph.fromSnapshot(snapshot);
    }

    private int firstVertexValue(Graph<Integer> source) {
        for (Vertex<Integer> vertex : source.vertices()) return vertex.value();
        throw new IllegalStateException("graph is empty");
    }
}
