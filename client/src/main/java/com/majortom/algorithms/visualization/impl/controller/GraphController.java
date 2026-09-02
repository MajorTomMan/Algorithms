package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.library.graph.GraphBfs;
import com.majortom.algorithms.library.graph.IntEdge;
import com.majortom.algorithms.library.graph.IntGraph;
import com.majortom.algorithms.library.structure.MutableGraph;
import com.majortom.algorithms.utils.EffectUtils;
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

    private MutableGraph<Integer> graph;
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
        StructureSnapshot<GraphSnapshot<Integer>> selectedStructureSnapshot =
                algorithmInputSnapshot == null ? captureStructureSnapshot() : algorithmInputSnapshot;
        GraphSnapshot<Integer> selected = selectedStructureSnapshot.state();
        MutableGraph<Integer> inputGraph = mutableGraph(selected);
        if (inputGraph.size() == 0) return;
        int algorithmStartNode = inputGraph.containsVertex(startNode)
                ? startNode : inputGraph.raw().keySet().iterator().next();
        dispatchVisualizerAction(
                com.majortom.algorithms.visualization.VisualizationActionType.GRAPH_RUN,
                java.util.Map.of("startNode", algorithmStartNode));
        IntGraph inputSnapshot = GraphBfs.snapshot(inputGraph);
        GraphBfs algorithm = module("algorithm.graph.Integer.graph-bfs", GraphBfs.class);
        startAlgorithm("graph-bfs", inputSnapshot, () -> algorithm.traverse(inputGraph, algorithmStartNode), () -> new GraphEventReducer(inputSnapshot));
    }

    @Override
    public boolean selectAlgorithm(String algorithmId) {
        if (!"graph-bfs".equals(algorithmId)) {
            return false;
        }
        if (algorithmSelector != null) {
            algorithmSelector.getSelectionModel().selectFirst();
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
        if (!graph.containsVertex(id)) {
            logI18n("message.graph.not_found", id);
            return;
        }
        renderViewState(new GraphViewState(
                GraphBfs.snapshot(graph), Set.of(), Set.of(), List.of(), java.util.Map.of(), id, null,
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
        if (!graph.containsVertex(from) || !graph.containsVertex(to)) {
            logI18n("message.error.graph_node_missing");
            return;
        }
        if (!graph.neighbors(from).contains(to)) {
            logI18n("message.graph.edge_not_found", from, to);
            return;
        }
        if (!executeStructureOperation("remove-edge", () -> graph.removeEdge(from, to))) {
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
        if (graph.containsVertex(id)) {
            logI18n("message.graph.already_exists", id);
            return;
        }
        boolean wasEmpty = graph.size() == 0;
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
        if (!graph.containsVertex(id)) {
            logI18n("message.graph.not_found", id);
            return;
        }
        if (!executeStructureOperation("remove-vertex", () -> graph.removeVertex(id))) {
            return;
        }
        if (graph.size() == 0) {
            startNode = 0;
            startField.clear();
        } else if (!graph.containsVertex(startNode)) {
            startNode = graph.raw().keySet().iterator().next();
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
        if (from == null || to == null || !graph.containsVertex(from) || !graph.containsVertex(to)) {
            logI18n("message.error.graph_node_missing");
            return;
        }
        if (graph.neighbors(from).contains(to)) {
            logI18n("message.graph.edge_exists", from, to);
            return;
        }
        if (!executeStructureOperation("add-edge", () -> {
            graph.addEdge(from, to);
            return null;
        })) {
            return;
        }
        renderGraph();
        dispatchVisualizerAction(
                com.majortom.algorithms.visualization.VisualizationActionType.GRAPH_LINK,
                java.util.Map.of("from", from, "to", to));
        logI18n("message.graph.link.directed", from, to, 1);
    }

    private void setStartNode(String text) {
        Integer id = parseNode(text);
        if (id == null || !graph.containsVertex(id)) {
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
        if (!graph.containsVertex(startNode)) {
            startNode = 0;
            if (graph.size() > 0) {
                startNode = graph.raw().keySet().iterator().next();
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
                I18N.text("stats.graph.nodes", graph.size()),
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

    private MutableGraph<Integer> randomGraph(int nodeCount, int edgeCount) {
        MutableGraph<Integer> result = new MutableGraph<>();
        for (int node = 0; node < nodeCount; node++) result.addVertex(node);
        Random random = new Random();
        Set<IntEdge> edges = new LinkedHashSet<>();
        for (int node = 1; node < nodeCount; node++) edges.add(new IntEdge(node - 1, node));
        while (edges.size() < edgeCount) {
            int from = random.nextInt(nodeCount);
            int to = random.nextInt(nodeCount);
            if (from != to) edges.add(new IntEdge(from, to));
        }
        for (IntEdge edge : edges) result.addEdge(edge.from(), edge.to());
        return result;
    }

    private GraphSnapshot<Integer> graphSnapshot() {
        List<Integer> vertices = new ArrayList<>(graph.raw().keySet());
        List<GraphSnapshot.Edge<Integer>> edges = new ArrayList<>();
        for (Integer from : vertices) {
            for (Integer to : graph.neighbors(from)) {
                edges.add(new GraphSnapshot.Edge<>(from, to));
            }
        }
        return new GraphSnapshot<>(vertices, edges);
    }

    private MutableGraph<Integer> mutableGraph(GraphSnapshot<Integer> snapshot) {
        MutableGraph<Integer> result = new MutableGraph<>();
        for (Integer node : snapshot.vertices()) {
            result.addVertex(node);
        }
        for (GraphSnapshot.Edge<Integer> edge : snapshot.edges()) {
            result.addEdge(edge.from(), edge.to());
        }
        return result;
    }
}
