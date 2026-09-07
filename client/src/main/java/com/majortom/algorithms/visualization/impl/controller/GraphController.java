package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.core.snapshot.GraphSnapshotState;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.core.snapshot.WeightedGraphSnapshot;
import com.majortom.algorithms.library.basic.graph.Edge;
import com.majortom.algorithms.library.basic.graph.Graph;
import com.majortom.algorithms.library.basic.graph.Vertex;
import com.majortom.algorithms.library.basic.graph.WeightedGraph;
import com.majortom.algorithms.library.graph.GraphFamilyAlgorithm;
import com.majortom.algorithms.library.graph.GraphTraversal;
import com.majortom.algorithms.library.graph.MinimumSpanningAlgorithm;
import com.majortom.algorithms.library.structure.GraphStructure;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.impl.visualizer.GraphVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.runtime.graph.GraphEventReducer;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.structure.SnapshotAlgorithmInputSupport;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
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
import java.util.function.Consumer;

public final class GraphController extends BaseModuleController<GraphViewState>
        implements AlgorithmSelectionSupport, StructureSnapshotSupport<GraphSnapshotState<Integer>>,
        SnapshotAlgorithmInputSupport<GraphSnapshotState<Integer>> {

    private Graph<Integer> basicGraph;
    private WeightedGraph<Integer> weightedGraph;
    private GraphVariant activeVariant = GraphVariant.BASIC;
    private List<String> algorithmIds = List.of();
    private StructureSnapshot<GraphSnapshotState<Integer>> algorithmInputSnapshot;
    private int startNode;
    private Consumer<Selection> selectionListener = ignored -> { };
    private Consumer<String> algorithmSelectionListener = ignored -> { };

    @FXML private Label structureLabel;
    @FXML private ComboBox<String> structureSelector;
    @FXML private Label algorithmLabel;
    @FXML private Label executionSectionLabel;
    @FXML private ComboBox<String> algorithmSelector;
    @FXML private Label nodeOperationsLabel;
    @FXML private Label edgeOperationsLabel;
    @FXML private Label traversalLabel;
    @FXML private TextField nodeField;
    @FXML private TextField fromField;
    @FXML private TextField toField;
    @FXML private TextField weightField;
    @FXML private TextField startField;
    @FXML private Button addNodeBtn;
    @FXML private Button deleteNodeBtn;
    @FXML private Button addEdgeBtn;
    @FXML private Button deleteEdgeBtn;
    @FXML private Button setWeightBtn;
    @FXML private Button setStartBtn;
    @FXML private Button runBtn;

    private enum GraphVariant {
        BASIC,
        WEIGHTED
    }

    public GraphController() {
        super(new GraphVisualizer(), "/fxml/GraphControls.fxml");
        basicGraph = module("structure.graph.Integer", Graph.class);
        weightedGraph = module("structure.graph.weighted.Integer", WeightedGraph.class);
        basicGraph = randomGraph(10, 16, false);
        weightedGraph = randomWeightedGraph(10, 16, false);
        startNode = firstVertexValue(basicGraph);
        graphVisualizer().setNodeSelectionListener(this::handleVisualNodeSelection);
        graphVisualizer().setEdgeSelectionListener(this::handleVisualEdgeSelection);
        refreshAlgorithmIds();
        renderGraph();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bindSelectors();
        if (startField != null) {
            startField.setText(Integer.toString(startNode));
        }
        if (weightField != null && weightField.getText().isBlank()) {
            weightField.setText("1");
        }
        EffectUtils.applyDynamicEffect(
                addNodeBtn, deleteNodeBtn, addEdgeBtn,
                deleteEdgeBtn, setWeightBtn, setStartBtn, runBtn);
        refreshVariantControls();
    }

    @Override
    @FXML
    public void handleAlgorithmStart() {
        if (isRunning()) {
            return;
        }
        String algorithmId = selectedAlgorithmId();
        if (algorithmId == null) {
            return;
        }
        GraphSnapshotState<Integer> selectedSnapshot = selectedAlgorithmSnapshot();
        GraphFamilyAlgorithm<Integer> algorithm = graphAlgorithm(algorithmId);
        if (algorithm instanceof GraphTraversal<?>) {
            runTraversal(algorithmId, algorithm, selectedSnapshot);
            return;
        }
        if (algorithm instanceof MinimumSpanningAlgorithm<?>) {
            runMinimumSpanning(algorithmId, algorithm, selectedSnapshot);
            return;
        }
        throw new IllegalStateException("Unsupported graph algorithm contract: " + algorithm.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private GraphFamilyAlgorithm<Integer> graphAlgorithm(String algorithmId) {
        return (GraphFamilyAlgorithm<Integer>) module(
                "algorithm.graph.Integer." + algorithmId,
                GraphFamilyAlgorithm.class);
    }

    @SuppressWarnings("unchecked")
    private void runTraversal(
            String algorithmId,
            GraphFamilyAlgorithm<Integer> algorithm,
            GraphSnapshotState<Integer> inputSnapshot) {
        GraphTraversal<Integer> traversal = (GraphTraversal<Integer>) algorithm;
        GraphStructure<Integer> inputGraph = graphFromSnapshot(inputSnapshot);
        if (inputGraph.isEmpty()) {
            return;
        }
        int algorithmStartNode = startNode;
        if (inputGraph.vertex(algorithmStartNode) == null) {
            algorithmStartNode = firstVertexValue(inputGraph);
        }
        int finalStartNode = algorithmStartNode;
        startAlgorithm(
                algorithmId,
                inputSnapshot,
                () -> traversal.traverse(inputGraph, finalStartNode),
                () -> new GraphEventReducer(inputSnapshot));
    }

    @SuppressWarnings("unchecked")
    private void runMinimumSpanning(
            String algorithmId,
            GraphFamilyAlgorithm<Integer> algorithm,
            GraphSnapshotState<Integer> inputSnapshot) {
        if (!(inputSnapshot instanceof WeightedGraphSnapshot<?> weighted)) {
            throw new IllegalArgumentException("minimum spanning algorithms require a weighted graph snapshot");
        }
        WeightedGraphSnapshot<Integer> sourceSnapshot = (WeightedGraphSnapshot<Integer>) weighted;
        WeightedGraph<Integer> source = WeightedGraph.fromSnapshot(sourceSnapshot);
        WeightedGraphSnapshot<Integer> resultSnapshot = new WeightedGraphSnapshot<>(
                false,
                sourceSnapshot.vertices(),
                List.of());
        WeightedGraph<Integer> result = WeightedGraph.fromSnapshot(resultSnapshot);
        MinimumSpanningAlgorithm<Integer> spanning = (MinimumSpanningAlgorithm<Integer>) algorithm;
        startAlgorithm(
                algorithmId,
                sourceSnapshot,
                () -> {
                    spanning.build(source, result);
                    return null;
                },
                () -> new GraphEventReducer(resultSnapshot));
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
        notifyAlgorithmSelection();
        return true;
    }

    @Override
    public List<String> algorithmIds() {
        return List.copyOf(algorithmIds);
    }

    @Override
    public void setAlgorithmSelectionListener(Consumer<String> listener) {
        if (listener == null) {
            algorithmSelectionListener = ignored -> { };
        } else {
            algorithmSelectionListener = listener;
        }
        notifyAlgorithmSelection();
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
    private void handleAddEdge() {
        linkNodes(fromField.getText(), toField.getText());
    }

    @FXML
    private void handleDeleteEdge() {
        Integer from = parseNode(fromField.getText());
        Integer to = parseNode(toField.getText());
        GraphStructure<Integer> graph = currentGraph();
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
        clearVisualSelection();
        renderGraph();
        logI18n("message.graph.edge_deleted", from, to);
    }

    @FXML
    private void handleSetWeight() {
        if (activeVariant != GraphVariant.WEIGHTED) {
            return;
        }
        Integer from = parseNode(fromField.getText());
        Integer to = parseNode(toField.getText());
        Double weight = parseWeight(weightField.getText());
        if (from == null || to == null || weight == null) {
            return;
        }
        Vertex<Integer> fromVertex = weightedGraph.vertex(from);
        Vertex<Integer> toVertex = weightedGraph.vertex(to);
        if (fromVertex == null || toVertex == null) {
            logI18n("message.error.graph_node_missing");
            return;
        }
        Edge<Integer> edge = weightedGraph.edge(fromVertex, toVertex);
        if (edge == null) {
            logI18n("message.graph.edge_not_found", from, to);
            return;
        }
        if (executeStructureOperation("set-weight", () -> weightedGraph.setWeight(edge, weight))) {
            renderGraph();
        }
    }

    @FXML
    private void handleSetStart() {
        setStartNode(startField.getText());
    }

    private void addNode(String text) {
        Integer id = parseNode(text);
        GraphStructure<Integer> graph = currentGraph();
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
            startField.setText(Integer.toString(startNode));
        }
        renderGraph();
        logI18n("message.graph.node_added", id);
    }

    private void deleteNode(String text) {
        Integer id = parseNode(text);
        GraphStructure<Integer> graph = currentGraph();
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
        clearVisualSelection();
        syncStartNode(graph);
        renderGraph();
        logI18n("message.graph.node_deleted", id);
    }

    private void linkNodes(String fromText, String toText) {
        Integer from = parseNode(fromText);
        Integer to = parseNode(toText);
        GraphStructure<Integer> graph = currentGraph();
        if (from == null || to == null || graph.vertex(from) == null || graph.vertex(to) == null) {
            logI18n("message.error.graph_node_missing");
            return;
        }
        if (graph.containsEdge(graph.vertex(from), graph.vertex(to))) {
            logI18n("message.graph.edge_exists", from, to);
            return;
        }
        if (activeVariant == GraphVariant.WEIGHTED) {
            Double weight = parseWeight(weightField.getText());
            if (weight == null) {
                return;
            }
            if (!executeStructureOperation("add-edge", () -> {
                weightedGraph.addEdge(weightedGraph.vertex(from), weightedGraph.vertex(to), weight);
                return null;
            })) {
                return;
            }
        } else {
            if (!executeStructureOperation("add-edge", () -> {
                basicGraph.addEdge(basicGraph.vertex(from), basicGraph.vertex(to));
                return null;
            })) {
                return;
            }
        }
        renderGraph();
        if (graph.isDirected()) {
            logI18n("message.graph.link.directed", from, to, 1);
        } else {
            logI18n("message.graph.link.undirected", from, to, 1);
        }
    }

    private void setStartNode(String text) {
        Integer id = parseNode(text);
        GraphStructure<Integer> graph = currentGraph();
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

    private Double parseWeight(String text) {
        try {
            double weight = Double.parseDouble(text.trim());
            if (!Double.isFinite(weight)) {
                throw new NumberFormatException("non-finite weight");
            }
            return weight;
        } catch (RuntimeException exception) {
            logI18n("message.error.invalid_graph_weight");
            return null;
        }
    }

    private void activateVariant(GraphVariant variant) {
        if (activeVariant == variant) {
            refreshVariantControls();
            return;
        }
        algorithmInputSnapshot = null;
        invalidateExecutionForInputChange();
        activeVariant = variant;
        clearVisualSelection();
        syncStartNode(currentGraph());
        refreshAlgorithmIds();
        syncStructureSelectorSelection();
        refreshVariantControls();
        renderGraph();
        refreshStatsDisplay();
    }

    private void syncStartNode(GraphStructure<Integer> graph) {
        if (graph.isEmpty()) {
            startNode = 0;
            if (startField != null) {
                startField.clear();
            }
            return;
        }
        if (graph.vertex(startNode) == null) {
            startNode = firstVertexValue(graph);
        }
        if (startField != null) {
            startField.setText(Integer.toString(startNode));
        }
    }

    private void renderGraph() {
        renderStructureState(GraphViewState.initial(currentSnapshot()));
    }

    @Override
    public StructureSnapshot<GraphSnapshotState<Integer>> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), currentSnapshot());
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<GraphSnapshotState<Integer>> snapshot) {
        requireGraphSnapshot(snapshot);
        GraphSnapshotState<Integer> state = snapshot.state();
        if (state instanceof GraphSnapshot<?> basic) {
            @SuppressWarnings("unchecked")
            GraphSnapshot<Integer> typed = (GraphSnapshot<Integer>) basic;
            basicGraph = Graph.fromSnapshot(typed);
            activateVariant(GraphVariant.BASIC);
        } else if (state instanceof WeightedGraphSnapshot<?> weighted) {
            @SuppressWarnings("unchecked")
            WeightedGraphSnapshot<Integer> typed = (WeightedGraphSnapshot<Integer>) weighted;
            weightedGraph = WeightedGraph.fromSnapshot(typed);
            activateVariant(GraphVariant.WEIGHTED);
        } else {
            throw new IllegalArgumentException("unsupported graph snapshot type: " + state.getClass().getName());
        }
        algorithmInputSnapshot = null;
        syncStartNode(currentGraph());
        invalidateExecutionForStructureChange();
        clearVisualSelection();
        renderGraph();
        refreshStatsDisplay();
    }

    @Override
    public void useSnapshotAsAlgorithmInput(StructureSnapshot<GraphSnapshotState<Integer>> snapshot) {
        requireGraphSnapshot(snapshot);
        GraphSnapshotState<Integer> state = snapshot.state();
        if (state instanceof GraphSnapshot<?>) {
            activateVariant(GraphVariant.BASIC);
        } else if (state instanceof WeightedGraphSnapshot<?>) {
            activateVariant(GraphVariant.WEIGHTED);
        } else {
            throw new IllegalArgumentException("unsupported graph snapshot type: " + state.getClass().getName());
        }
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
        if (algorithmInputSnapshot == null) {
            return null;
        }
        return algorithmInputSnapshot.id();
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
        renderViewState(GraphViewState.initial(selectedAlgorithmSnapshot()));
    }

    @Override
    public String describeStructureSnapshot(GraphSnapshotState<Integer> state) {
        if (state instanceof GraphSnapshot<?> basic) {
            return I18N.text("snapshot.graph.detail", basic.vertices().size(), basic.edges().size());
        }
        if (state instanceof WeightedGraphSnapshot<?> weighted) {
            return I18N.text("snapshot.graph.detail", weighted.vertices().size(), weighted.edges().size());
        }
        throw new IllegalArgumentException("unsupported graph snapshot type: " + state.getClass().getName());
    }

    @Override
    public String structurePrimaryCount() {
        return Integer.toString(currentGraph().vertexCount());
    }

    @Override
    public String structureSecondaryCount() {
        return Integer.toString(currentGraph().edgeCount());
    }

    @Override
    protected String formatStatsMessage() {
        GraphStructure<Integer> graph = currentGraph();
        return String.format("%s | %s | %s",
                I18N.text("stats.graph.nodes", graph.vertexCount()),
                I18N.text("stats.graph.edges", graph.edgeCount()),
                formatMetric("stats.action", stats.metric("nodes.visited")
                        + stats.metric("edges.examined")));
    }

    @Override
    protected void onResetData() {
        renderGraph();
    }

    @Override
    protected void setupI18n() {
        if (structureLabel != null) {
            structureLabel.textProperty().bind(I18N.createStringBinding("label.common.structure"));
        }
        if (algorithmLabel != null) {
            algorithmLabel.textProperty().bind(I18N.createStringBinding("label.common.algorithm"));
        }
        if (executionSectionLabel != null) {
            executionSectionLabel.textProperty().bind(I18N.createStringBinding("label.panel.execution"));
        }
        bindLabel(nodeOperationsLabel, "label.graph.node_ops");
        bindLabel(edgeOperationsLabel, "label.graph.edge_ops");
        bindLabel(traversalLabel, "label.graph.run_start");
        bindPrompt(nodeField, "prompt.graph.node");
        bindPrompt(fromField, "prompt.graph.from");
        bindPrompt(toField, "prompt.graph.to");
        bindPrompt(weightField, "prompt.graph.weight");
        bindPrompt(startField, "prompt.graph.start");
        bindButton(addNodeBtn, "action.graph.add");
        bindButton(deleteNodeBtn, "action.graph.delete");
        bindButton(addEdgeBtn, "action.graph.link");
        bindButton(deleteEdgeBtn, "action.graph.delete_edge");
        bindButton(setWeightBtn, "action.graph.set_weight");
        bindButton(setStartBtn, "action.graph.set_start");
        bindButton(runBtn, "action.graph.run");
    }

    @Override
    protected String moduleId() {
        return "graph";
    }

    @Override
    public String selectedAlgorithmId() {
        if (algorithmIds.isEmpty()) {
            return null;
        }
        int index = 0;
        if (algorithmSelector != null) {
            int selectedIndex = algorithmSelector.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                index = selectedIndex;
            }
        }
        if (index >= algorithmIds.size()) {
            index = 0;
        }
        return algorithmIds.get(index);
    }

    private void bindSelectors() {
        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> FXCollections.observableArrayList(
                        I18N.text("label.graph.structure.basic"),
                        I18N.text("label.graph.structure.weighted")),
                I18N.localeProperty()));
        structureSelector.getSelectionModel().selectedIndexProperty().addListener((observable, previous, current) -> {
            if (current == null || current.intValue() < 0) {
                return;
            }
            if (current.intValue() == 0) {
                activateVariant(GraphVariant.BASIC);
            } else {
                activateVariant(GraphVariant.WEIGHTED);
            }
        });
        algorithmSelector.getSelectionModel().selectedIndexProperty().addListener((observable, previous, current) -> {
            refreshAlgorithmControls();
            notifyAlgorithmSelection();
        });
        I18N.localeProperty().addListener((observable, previous, current) -> {
            refreshAlgorithmSelector();
            Platform.runLater(this::syncStructureSelectorSelection);
        });
        Platform.runLater(() -> {
            syncStructureSelectorSelection();
            refreshAlgorithmSelector();
            refreshAlgorithmControls();
        });
    }

    private void refreshVariantControls() {
        refreshAlgorithmIds();
        refreshAlgorithmSelector();
        boolean weighted = activeVariant == GraphVariant.WEIGHTED;
        setVisibleManaged(weightField, weighted);
        setVisibleManaged(setWeightBtn, weighted);
        refreshAlgorithmControls();
    }

    private void refreshAlgorithmIds() {
        if (activeVariant == GraphVariant.BASIC) {
            algorithmIds = AlgorithmCatalog.basicGraphAlgorithms();
        } else {
            algorithmIds = AlgorithmCatalog.weightedGraphAlgorithms();
        }
    }

    private void refreshAlgorithmSelector() {
        if (algorithmSelector == null) {
            return;
        }
        String previousId = selectedAlgorithmId();
        javafx.collections.ObservableList<String> labels = FXCollections.observableArrayList();
        for (String id : algorithmIds) {
            labels.add(AlgorithmLabels.text(id));
        }
        algorithmSelector.setItems(labels);
        int index = algorithmIds.indexOf(previousId);
        if (index < 0 && !algorithmIds.isEmpty()) {
            index = 0;
        }
        if (index >= 0) {
            algorithmSelector.getSelectionModel().select(index);
        } else {
            algorithmSelector.getSelectionModel().clearSelection();
        }
        notifyAlgorithmSelection();
    }

    private void notifyAlgorithmSelection() {
        algorithmSelectionListener.accept(selectedAlgorithmId());
    }

    private void refreshAlgorithmControls() {
        String algorithmId = selectedAlgorithmId();
        boolean traversal = algorithmId != null && AlgorithmCatalog.graphTraversals().contains(algorithmId);
        setVisibleManaged(startField, traversal);
        setVisibleManaged(setStartBtn, traversal);
        setVisibleManaged(traversalLabel, traversal);
    }

    private void syncStructureSelectorSelection() {
        if (structureSelector == null) {
            return;
        }
        if (activeVariant == GraphVariant.BASIC) {
            structureSelector.getSelectionModel().select(0);
        } else {
            structureSelector.getSelectionModel().select(1);
        }
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

    private void setVisibleManaged(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setManaged(visible);
        node.setVisible(visible);
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
            String key;
            if (directed || from < to) {
                key = from + ":" + to;
            } else {
                key = to + ":" + from;
            }
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

    private WeightedGraph<Integer> randomWeightedGraph(int nodeCount, int edgeCount, boolean directed) {
        Graph<Integer> topology = randomGraph(nodeCount, edgeCount, directed);
        GraphSnapshot<Integer> snapshot = graphSnapshot(topology);
        List<WeightedGraphSnapshot.Vertex<Integer>> vertices = snapshot.vertices().stream()
                .map(vertex -> new WeightedGraphSnapshot.Vertex<>(vertex.id(), vertex.value()))
                .toList();
        List<WeightedGraphSnapshot.Edge> edges = new ArrayList<>();
        int index = 0;
        for (GraphSnapshot.Edge edge : snapshot.edges()) {
            double weight = 1.0d + ((index * 7) % 19);
            edges.add(new WeightedGraphSnapshot.Edge(edge.id(), edge.fromId(), edge.toId(), weight));
            index++;
        }
        return WeightedGraph.fromSnapshot(new WeightedGraphSnapshot<>(directed, vertices, edges));
    }

    public void setSelectionListener(Consumer<Selection> listener) {
        if (listener == null) {
            selectionListener = ignored -> { };
        } else {
            selectionListener = listener;
        }
    }

    private void handleVisualNodeSelection(long nodeId) {
        GraphSnapshotState<Integer> snapshot = currentSnapshot();
        Integer value = snapshotVertexValue(snapshot, nodeId);
        if (value == null) {
            clearVisualSelection();
            return;
        }
        int degree = snapshotDegree(snapshot, nodeId);
        String text = Integer.toString(value);
        if (nodeField != null) {
            nodeField.setText(text);
        }
        if (startField != null) {
            startField.setText(text);
        }
        selectionListener.accept(new NodeSelection(nodeId, value, degree));
    }

    private void handleVisualEdgeSelection(long edgeId) {
        GraphSnapshotState<Integer> snapshot = currentSnapshot();
        SnapshotEdge edge = snapshotEdge(snapshot, edgeId);
        if (edge == null) {
            clearVisualSelection();
            return;
        }
        Integer from = snapshotVertexValue(snapshot, edge.fromId());
        Integer to = snapshotVertexValue(snapshot, edge.toId());
        if (from == null || to == null) {
            clearVisualSelection();
            return;
        }
        if (fromField != null) {
            fromField.setText(Integer.toString(from));
        }
        if (toField != null) {
            toField.setText(Integer.toString(to));
        }
        if (weightField != null && edge.weight() != null) {
            weightField.setText(formatWeight(edge.weight()));
        }
        selectionListener.accept(new EdgeSelection(edgeId, from, to, snapshot.directed()));
    }

    private String formatWeight(double weight) {
        if (weight == Math.rint(weight)) {
            return Long.toString((long) weight);
        }
        return Double.toString(weight);
    }

    private void clearVisualSelection() {
        graphVisualizer().clearSelection();
        selectionListener.accept(null);
    }

    private GraphVisualizer graphVisualizer() {
        return (GraphVisualizer) visualizer;
    }

    public sealed interface Selection permits NodeSelection, EdgeSelection {
    }

    public record NodeSelection(long id, int value, int degree) implements Selection {
    }

    public record EdgeSelection(long id, int fromValue, int toValue, boolean directed) implements Selection {
    }

    private record SnapshotEdge(long id, long fromId, long toId, Double weight) {
    }

    private GraphStructure<Integer> currentGraph() {
        if (activeVariant == GraphVariant.BASIC) {
            return basicGraph;
        }
        return weightedGraph;
    }

    private GraphSnapshotState<Integer> currentSnapshot() {
        if (activeVariant == GraphVariant.BASIC) {
            return graphSnapshot(basicGraph);
        }
        return weightedGraph.snapshot();
    }

    private GraphSnapshotState<Integer> selectedAlgorithmSnapshot() {
        GraphSnapshotState<Integer> state;
        if (algorithmInputSnapshot == null) {
            state = currentSnapshot();
        } else {
            state = algorithmInputSnapshot.state();
        }
        if (!snapshotMatchesActiveVariant(state)) {
            throw new IllegalStateException("algorithm input graph variant does not match active structure");
        }
        return state;
    }

    private boolean snapshotMatchesActiveVariant(GraphSnapshotState<Integer> state) {
        if (activeVariant == GraphVariant.BASIC) {
            return state instanceof GraphSnapshot<?>;
        }
        return state instanceof WeightedGraphSnapshot<?>;
    }

    private void requireGraphSnapshot(StructureSnapshot<GraphSnapshotState<Integer>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
    }

    private GraphStructure<Integer> graphFromSnapshot(GraphSnapshotState<Integer> snapshot) {
        if (snapshot instanceof GraphSnapshot<?> basic) {
            @SuppressWarnings("unchecked")
            GraphSnapshot<Integer> typed = (GraphSnapshot<Integer>) basic;
            return Graph.fromSnapshot(typed);
        }
        if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            @SuppressWarnings("unchecked")
            WeightedGraphSnapshot<Integer> typed = (WeightedGraphSnapshot<Integer>) weighted;
            return WeightedGraph.fromSnapshot(typed);
        }
        throw new IllegalArgumentException("unsupported graph snapshot type: " + snapshot.getClass().getName());
    }

    private GraphSnapshot<Integer> graphSnapshot(GraphStructure<Integer> graph) {
        List<GraphSnapshot.Vertex<Integer>> vertices = new ArrayList<>();
        for (Vertex<Integer> vertex : graph.vertices()) {
            vertices.add(new GraphSnapshot.Vertex<>(vertex.id(), vertex.value()));
        }
        List<GraphSnapshot.Edge> edges = new ArrayList<>();
        for (Edge<Integer> edge : graph.edges()) {
            edges.add(new GraphSnapshot.Edge(edge.id(), edge.from().id(), edge.to().id()));
        }
        return new GraphSnapshot<>(graph.isDirected(), vertices, edges);
    }

    private Integer snapshotVertexValue(GraphSnapshotState<Integer> snapshot, long nodeId) {
        if (snapshot instanceof GraphSnapshot<?> basic) {
            for (GraphSnapshot.Vertex<?> vertex : basic.vertices()) {
                if (vertex.id() == nodeId) {
                    return (Integer) vertex.value();
                }
            }
            return null;
        }
        if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            for (WeightedGraphSnapshot.Vertex<?> vertex : weighted.vertices()) {
                if (vertex.id() == nodeId) {
                    return (Integer) vertex.value();
                }
            }
            return null;
        }
        return null;
    }

    private int snapshotDegree(GraphSnapshotState<Integer> snapshot, long nodeId) {
        int degree = 0;
        if (snapshot instanceof GraphSnapshot<?> basic) {
            for (GraphSnapshot.Edge edge : basic.edges()) {
                if (edge.fromId() == nodeId || edge.toId() == nodeId) {
                    degree++;
                }
            }
            return degree;
        }
        if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            for (WeightedGraphSnapshot.Edge edge : weighted.edges()) {
                if (edge.fromId() == nodeId || edge.toId() == nodeId) {
                    degree++;
                }
            }
        }
        return degree;
    }

    private SnapshotEdge snapshotEdge(GraphSnapshotState<Integer> snapshot, long edgeId) {
        if (snapshot instanceof GraphSnapshot<?> basic) {
            for (GraphSnapshot.Edge edge : basic.edges()) {
                if (edge.id() == edgeId) {
                    return new SnapshotEdge(edge.id(), edge.fromId(), edge.toId(), null);
                }
            }
            return null;
        }
        if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            for (WeightedGraphSnapshot.Edge edge : weighted.edges()) {
                if (edge.id() == edgeId) {
                    return new SnapshotEdge(edge.id(), edge.fromId(), edge.toId(), edge.weight());
                }
            }
        }
        return null;
    }

    private int firstVertexValue(GraphStructure<Integer> source) {
        for (Vertex<Integer> vertex : source.vertices()) {
            return vertex.value();
        }
        throw new IllegalStateException("graph is empty");
    }
}
