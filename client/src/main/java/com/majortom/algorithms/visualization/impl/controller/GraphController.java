package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.core.snapshot.GraphSnapshotState;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.core.snapshot.WeightedGraphSnapshot;
import com.majortom.algorithms.structure.graph.Edge;
import com.majortom.algorithms.structure.graph.Vertex;
import com.majortom.algorithms.structure.graph.WeightedGraph;
import com.majortom.algorithms.algorithm.graph.GraphFamilyAlgorithm;
import com.majortom.algorithms.algorithm.graph.GraphTraversal;
import com.majortom.algorithms.algorithm.graph.MinimumSpanningAlgorithm;
import com.majortom.algorithms.structure.graph.GraphStructure;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.impl.visualizer.GraphVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.runtime.VisualValue;
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

    private WeightedGraph<Integer> undirectedGraph;
    private WeightedGraph<Integer> directedGraph;
    private GraphVariant activeVariant = GraphVariant.UNDIRECTED;
    private List<String> algorithmIds = List.of();
    private StructureSnapshot<GraphSnapshotState<Integer>> algorithmInputSnapshot;
    private int startNode;
    private boolean structureSelectionEnabled = true;
    private Long algorithmSelectedNodeId;
    private Long algorithmSelectedEdgeId;
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
        UNDIRECTED,
        DIRECTED
    }

    public GraphController() {
        super(new GraphVisualizer(), "/fxml/GraphControls.fxml");
        undirectedGraph = randomWeightedGraph(10, 16, false);
        directedGraph = randomWeightedGraph(10, 16, true);
        startNode = firstVertexValue(undirectedGraph);
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
        return (GraphFamilyAlgorithm<Integer>) algorithm(algorithmId, Integer.class, GraphFamilyAlgorithm.class);
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
        Vertex<Integer> fromVertex = graph.vertex(from);
        Vertex<Integer> toVertex = graph.vertex(to);
        if (fromVertex == null || toVertex == null) {
            logI18n("message.error.graph_node_missing");
            return;
        }
        if (!graph.containsEdge(fromVertex, toVertex)) {
            logI18n("message.graph.edge_not_found", from, to);
            return;
        }
        GraphSnapshotState<Integer> previousSnapshot = currentSnapshot();
        List<Long> previousEdgeOrder = snapshotEdgeIds(previousSnapshot);
        Long removedEdgeId = snapshotEdgeIdBetween(previousSnapshot, fromVertex.id(), toVertex.id());
        int removedIndex = -1;
        if (removedEdgeId != null) {
            removedIndex = previousEdgeOrder.indexOf(removedEdgeId);
        }
        if (!executeStructureOperation("remove-edge", () -> graph.removeEdge(fromVertex, toVertex))) {
            return;
        }
        renderGraph();
        selectGraphEdgeAfterRemoval(previousEdgeOrder, removedIndex);
        logI18n("message.graph.edge_deleted", from, to);
    }

    @FXML
    private void handleSetWeight() {
        Integer from = parseNode(fromField.getText());
        Integer to = parseNode(toField.getText());
        Double weight = parseWeight(weightField.getText());
        if (from == null || to == null || weight == null) {
            return;
        }
        WeightedGraph<Integer> graph = currentWeightedGraph();
        Vertex<Integer> fromVertex = graph.vertex(from);
        Vertex<Integer> toVertex = graph.vertex(to);
        if (fromVertex == null || toVertex == null) {
            logI18n("message.error.graph_node_missing");
            return;
        }
        Edge<Integer> edge = graph.edge(fromVertex, toVertex);
        if (edge == null) {
            logI18n("message.graph.edge_not_found", from, to);
            return;
        }
        if (executeStructureOperation("set-weight", () -> graph.setWeight(edge, weight))) {
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
        Vertex<Integer> addedVertex = graph.vertex(id);
        if (addedVertex != null) {
            graphVisualizer().selectNode(addedVertex.id());
        }
        logI18n("message.graph.node_added", id);
    }

    private void deleteNode(String text) {
        Integer id = parseNode(text);
        GraphStructure<Integer> graph = currentGraph();
        if (id == null) {
            return;
        }
        Vertex<Integer> removedVertex = graph.vertex(id);
        if (removedVertex == null) {
            logI18n("message.graph.not_found", id);
            return;
        }
        List<Long> previousNodeOrder = snapshotVertexIds(currentSnapshot());
        int removedIndex = previousNodeOrder.indexOf(removedVertex.id());
        if (!executeStructureOperation("remove-vertex", () -> graph.removeVertex(removedVertex))) {
            return;
        }
        syncStartNode(graph);
        renderGraph();
        selectGraphNodeAfterRemoval(previousNodeOrder, removedIndex);
        logI18n("message.graph.node_deleted", id);
    }

    private void linkNodes(String fromText, String toText) {
        Integer from = parseNode(fromText);
        Integer to = parseNode(toText);
        WeightedGraph<Integer> graph = currentWeightedGraph();
        if (from == null || to == null || graph.vertex(from) == null || graph.vertex(to) == null) {
            logI18n("message.error.graph_node_missing");
            return;
        }
        Double weight = parseWeight(weightField.getText());
        if (weight == null) {
            return;
        }
        if (graph.containsEdge(graph.vertex(from), graph.vertex(to))) {
            logI18n("message.graph.edge_exists", from, to);
            return;
        }
        if (!executeStructureOperation("add-edge", () -> {
            graph.addEdge(graph.vertex(from), graph.vertex(to), weight);
            return null;
        })) {
            return;
        }
        renderGraph();
        Vertex<Integer> fromVertex = graph.vertex(from);
        Vertex<Integer> toVertex = graph.vertex(to);
        Long addedEdgeId = snapshotEdgeIdBetween(currentSnapshot(), fromVertex.id(), toVertex.id());
        if (addedEdgeId != null) {
            graphVisualizer().selectEdge(addedEdgeId);
        }
        if (graph.isDirected()) {
            logI18n("message.graph.link.directed", from, to, 1);
        } else {
            logI18n("message.graph.link.undirected", from, to, 1);
        }
    }

    private void selectGraphNodeAfterRemoval(List<Long> previousOrder, int removedIndex) {
        GraphSnapshotState<Integer> snapshot = currentSnapshot();
        List<Long> currentOrder = snapshotVertexIds(snapshot);
        if (currentOrder.isEmpty()) {
            clearVisualSelection();
            nodeField.clear();
            fromField.clear();
            toField.clear();
            if (weightField != null) {
                weightField.clear();
            }
            return;
        }
        Long candidate = survivingGraphSelection(previousOrder, removedIndex, currentOrder);
        if (candidate == null) {
            clearVisualSelection();
            return;
        }
        graphVisualizer().selectNode(candidate);
    }

    private void selectGraphEdgeAfterRemoval(List<Long> previousOrder, int removedIndex) {
        GraphSnapshotState<Integer> snapshot = currentSnapshot();
        List<Long> currentOrder = snapshotEdgeIds(snapshot);
        if (currentOrder.isEmpty()) {
            clearVisualSelection();
            fromField.clear();
            toField.clear();
            if (weightField != null) {
                weightField.clear();
            }
            return;
        }
        Long candidate = survivingGraphSelection(previousOrder, removedIndex, currentOrder);
        if (candidate == null) {
            clearVisualSelection();
            return;
        }
        graphVisualizer().selectEdge(candidate);
    }

    private Long survivingGraphSelection(List<Long> previousOrder, int removedIndex, List<Long> currentOrder) {
        int startIndex = removedIndex;
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int index = startIndex; index < previousOrder.size(); index++) {
            Long candidate = previousOrder.get(index);
            if (currentOrder.contains(candidate)) {
                return candidate;
            }
        }
        for (int index = startIndex - 1; index >= 0; index--) {
            Long candidate = previousOrder.get(index);
            if (currentOrder.contains(candidate)) {
                return candidate;
            }
        }
        return currentOrder.get(0);
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
        WeightedGraphSnapshot<Integer> state = asWeightedSnapshot(snapshot.state());
        if (state.directed()) {
            directedGraph = WeightedGraph.fromSnapshot(state);
            activateVariant(GraphVariant.DIRECTED);
        } else {
            undirectedGraph = WeightedGraph.fromSnapshot(state);
            activateVariant(GraphVariant.UNDIRECTED);
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
        if (state.directed()) {
            activateVariant(GraphVariant.DIRECTED);
        } else {
            activateVariant(GraphVariant.UNDIRECTED);
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
    public void previewStructureSnapshot(StructureSnapshot<GraphSnapshotState<Integer>> snapshot) {
        requireGraphSnapshot(snapshot);
        clearVisualSelection();
        renderPreviewState(GraphViewState.initial(snapshot.state()));
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
    public String snapshotPrimaryCount(GraphSnapshotState<Integer> state) {
        if (state instanceof GraphSnapshot<?> basic) {
            return Integer.toString(basic.vertices().size());
        }
        if (state instanceof WeightedGraphSnapshot<?> weighted) {
            return Integer.toString(weighted.vertices().size());
        }
        return "—";
    }

    @Override
    public String snapshotSecondaryCount(GraphSnapshotState<Integer> state) {
        if (state instanceof GraphSnapshot<?> basic) {
            return Integer.toString(basic.edges().size());
        }
        if (state instanceof WeightedGraphSnapshot<?> weighted) {
            return Integer.toString(weighted.edges().size());
        }
        return "—";
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
                        I18N.text("label.graph.structure.undirected"),
                        I18N.text("label.graph.structure.directed")),
                I18N.localeProperty()));
        structureSelector.getSelectionModel().selectedIndexProperty().addListener((observable, previous, current) -> {
            if (current == null || current.intValue() < 0) {
                return;
            }
            if (current.intValue() == 0) {
                activateVariant(GraphVariant.UNDIRECTED);
            } else {
                activateVariant(GraphVariant.DIRECTED);
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
        setVisibleManaged(weightField, true);
        setVisibleManaged(setWeightBtn, true);
        refreshAlgorithmControls();
    }

    private void refreshAlgorithmIds() {
        if (activeVariant == GraphVariant.UNDIRECTED) {
            algorithmIds = AlgorithmCatalog.weightedGraphAlgorithms();
        } else {
            algorithmIds = AlgorithmCatalog.basicGraphAlgorithms();
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
        if (activeVariant == GraphVariant.UNDIRECTED) {
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

    private WeightedGraph<Integer> randomWeightedGraph(int nodeCount, int edgeCount, boolean directed) {
        WeightedGraph<Integer> result = new WeightedGraph<>(directed);
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
        int weightIndex = 0;
        for (String edge : edges) {
            String[] parts = edge.split(":", 2);
            int from = Integer.parseInt(parts[0]);
            int to = Integer.parseInt(parts[1]);
            double weight = 1.0d + ((weightIndex * 7) % 19);
            result.addEdge(result.vertex(from), result.vertex(to), weight);
            weightIndex++;
        }
        return result;
    }

    @Override
    protected boolean supportsDataTools() {
        return true;
    }

    @Override
    protected String bulkInputPromptKey() {
        return "prompt.data.bulk.graph";
    }

    @Override
    protected void applyBulkData(String input) {
        GraphBatch batch = parseGraphBatch(input);
        if (batch == null) {
            return;
        }
        replaceGraphData(batch, "bulk-replace", "message.data.bulk_applied");
    }

    @Override
    protected void randomizeData() {
        replaceGraphData(randomGraphBatch(10, 16), "randomize", "message.data.randomized");
    }

    private GraphBatch parseGraphBatch(String input) {
        if (input == null || input.isBlank()) {
            logI18n("message.error.bulk_input_empty");
            return null;
        }
        String[] sections = input.split("\\|", -1);
        if (sections.length > 2) {
            logI18n("message.error.bulk_input_invalid");
            return null;
        }
        List<Integer> nodes = parseIntegerBatchInput(sections[0]);
        if (nodes == null) {
            return null;
        }
        Set<Integer> nodeSet = new LinkedHashSet<>(nodes);
        if (nodeSet.size() != nodes.size()) {
            logI18n("message.error.bulk_duplicates");
            return null;
        }
        List<GraphBatchEdge> edges = new ArrayList<>();
        if (sections.length == 2 && !sections[1].isBlank()) {
            String[] edgeTokens = sections[1].trim().split("[,;\\s]+");
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "^(-?\\d+)\\s*(?:->|>|-)\\s*(-?\\d+)$");
            for (String token : edgeTokens) {
                if (token.isBlank()) {
                    continue;
                }
                String[] weighted = token.split(":", 2);
                java.util.regex.Matcher matcher = pattern.matcher(weighted[0]);
                if (!matcher.matches()) {
                    logI18n("message.error.bulk_input_invalid");
                    return null;
                }
                int from;
                int to;
                double weight = 1.0d;
                try {
                    from = Integer.parseInt(matcher.group(1));
                    to = Integer.parseInt(matcher.group(2));
                    if (weighted.length == 2) {
                        weight = Double.parseDouble(weighted[1]);
                    }
                } catch (NumberFormatException exception) {
                    logI18n("message.error.bulk_input_invalid");
                    return null;
                }
                if (!Double.isFinite(weight)) {
                    logI18n("message.error.invalid_graph_weight");
                    return null;
                }
                if (!nodeSet.contains(from) || !nodeSet.contains(to)) {
                    logI18n("message.error.graph_bulk_endpoint", token);
                    return null;
                }
                edges.add(new GraphBatchEdge(from, to, weight));
            }
        }
        return new GraphBatch(List.copyOf(nodes), List.copyOf(edges));
    }

    private GraphBatch randomGraphBatch(int nodeCount, int edgeCount) {
        List<Integer> nodes = new ArrayList<>();
        for (int node = 0; node < nodeCount; node++) {
            nodes.add(node);
        }
        Random random = new Random();
        Set<String> edges = new LinkedHashSet<>();
        for (int node = 1; node < nodeCount; node++) {
            edges.add((node - 1) + ":" + node);
        }
        boolean directed = activeVariant == GraphVariant.DIRECTED;
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
        List<GraphBatchEdge> batchEdges = new ArrayList<>();
        for (String edge : edges) {
            String[] parts = edge.split(":", 2);
            batchEdges.add(new GraphBatchEdge(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    1.0d + random.nextInt(20)));
        }
        return new GraphBatch(List.copyOf(nodes), List.copyOf(batchEdges));
    }

    private void replaceGraphData(GraphBatch batch, String operationId, String messageKey) {
        WeightedGraph<Integer> graph = currentWeightedGraph();
        clearVisualSelection();
        if (!executeStructureOperation(operationId, () -> {
            List<Vertex<Integer>> existing = new ArrayList<>();
            for (Vertex<Integer> vertex : graph.vertices()) {
                existing.add(vertex);
            }
            for (Vertex<Integer> vertex : existing) {
                graph.removeVertex(vertex);
            }
            for (Integer node : batch.nodes()) {
                graph.addVertex(node);
            }
            for (GraphBatchEdge edge : batch.edges()) {
                graph.addEdge(graph.vertex(edge.from()), graph.vertex(edge.to()), edge.weight());
            }
            return null;
        })) {
            return;
        }
        syncStartNode(graph);
        renderGraph();
        refreshStatsDisplay();
        if (!batch.nodes().isEmpty()) {
            Vertex<Integer> selected = graph.vertex(batch.nodes().get(0));
            if (selected != null) {
                graphVisualizer().selectNode(selected.id());
            }
        }
        logI18n(messageKey, batch.nodes().size());
    }

    private record GraphBatch(List<Integer> nodes, List<GraphBatchEdge> edges) {
    }

    private record GraphBatchEdge(int from, int to, double weight) {
    }

    public void setSelectionListener(Consumer<Selection> listener) {
        if (listener == null) {
            selectionListener = ignored -> { };
        } else {
            selectionListener = listener;
        }
    }

    public void setStructureSelectionEnabled(boolean enabled) {
        if (structureSelectionEnabled != enabled) {
            clearVisualSelection();
        }
        structureSelectionEnabled = enabled;
    }

    private void handleVisualNodeSelection(long nodeId) {
        if (!structureSelectionEnabled) {
            handleAlgorithmNodeSelection(nodeId);
            return;
        }
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
        selectionListener.accept(new NodeSelection(nodeId, VisualValue.of(value), degree));
    }

    private void handleAlgorithmNodeSelection(long nodeId) {
        GraphViewState state = latestViewState();
        if (state == null) {
            return;
        }
        algorithmSelectedNodeId = nodeId;
        algorithmSelectedEdgeId = null;
        if (!publishAlgorithmNodeSelection(state, nodeId)) {
            clearVisualSelection();
        }
    }

    private boolean publishAlgorithmNodeSelection(GraphViewState state, long nodeId) {
        GraphViewState.Node selected = null;
        for (GraphViewState.Node node : state.nodes()) {
            if (node.id() == nodeId) {
                selected = node;
                break;
            }
        }
        if (selected == null) {
            return false;
        }
        int degree = 0;
        for (GraphViewState.Edge edge : state.edges()) {
            if (edge.fromId() == nodeId || edge.toId() == nodeId) {
                degree++;
            }
        }
        selectionListener.accept(new NodeSelection(nodeId, selected.value(), degree));
        return true;
    }

    private void handleVisualEdgeSelection(long edgeId) {
        if (!structureSelectionEnabled) {
            handleAlgorithmEdgeSelection(edgeId);
            return;
        }
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
        selectionListener.accept(new EdgeSelection(edgeId, VisualValue.of(from), VisualValue.of(to), snapshot.directed()));
    }

    private void handleAlgorithmEdgeSelection(long edgeId) {
        GraphViewState state = latestViewState();
        if (state == null) {
            return;
        }
        algorithmSelectedEdgeId = edgeId;
        algorithmSelectedNodeId = null;
        if (!publishAlgorithmEdgeSelection(state, edgeId)) {
            clearVisualSelection();
        }
    }

    private boolean publishAlgorithmEdgeSelection(GraphViewState state, long edgeId) {
        GraphViewState.Edge selected = null;
        for (GraphViewState.Edge edge : state.edges()) {
            if (edge.id() == edgeId) {
                selected = edge;
                break;
            }
        }
        if (selected == null) {
            return false;
        }
        GraphViewState.Node from = state.nodesById().get(selected.fromId());
        GraphViewState.Node to = state.nodesById().get(selected.toId());
        if (from == null || to == null) {
            return false;
        }
        selectionListener.accept(new EdgeSelection(edgeId, from.value(), to.value(), state.directed()));
        return true;
    }

    @Override
    protected void onPresentationStateChanged(GraphViewState state) {
        if (structureSelectionEnabled) {
            return;
        }
        if (algorithmSelectedNodeId != null) {
            long nodeId = algorithmSelectedNodeId;
            if (!graphVisualizer().showNodeSelection(nodeId) || !publishAlgorithmNodeSelection(state, nodeId)) {
                clearVisualSelection();
            }
            return;
        }
        if (algorithmSelectedEdgeId != null) {
            long edgeId = algorithmSelectedEdgeId;
            if (!graphVisualizer().showEdgeSelection(edgeId) || !publishAlgorithmEdgeSelection(state, edgeId)) {
                clearVisualSelection();
            }
        }
    }

    private String formatWeight(double weight) {
        if (weight == Math.rint(weight)) {
            return Long.toString((long) weight);
        }
        return Double.toString(weight);
    }

    private void clearVisualSelection() {
        algorithmSelectedNodeId = null;
        algorithmSelectedEdgeId = null;
        graphVisualizer().clearSelection();
        selectionListener.accept(null);
    }

    private GraphVisualizer graphVisualizer() {
        return (GraphVisualizer) visualizer;
    }

    public sealed interface Selection permits NodeSelection, EdgeSelection {
    }

    public record NodeSelection(long id, VisualValue value, int degree) implements Selection {
    }

    public record EdgeSelection(long id, VisualValue fromValue, VisualValue toValue, boolean directed) implements Selection {
    }

    private record SnapshotEdge(long id, long fromId, long toId, Double weight) {
    }

    private GraphStructure<Integer> currentGraph() {
        return currentWeightedGraph();
    }

    private WeightedGraph<Integer> currentWeightedGraph() {
        if (activeVariant == GraphVariant.UNDIRECTED) {
            return undirectedGraph;
        }
        return directedGraph;
    }

    private GraphSnapshotState<Integer> currentSnapshot() {
        return currentWeightedGraph().snapshot();
    }

    private GraphSnapshotState<Integer> selectedAlgorithmSnapshot() {
        GraphSnapshotState<Integer> state;
        if (algorithmInputSnapshot == null) {
            state = currentSnapshot();
        } else {
            state = algorithmInputSnapshot.state();
        }
        if (!snapshotMatchesActiveVariant(state)) {
            throw new IllegalStateException("algorithm input graph direction does not match active structure");
        }
        return asWeightedSnapshot(state);
    }

    private boolean snapshotMatchesActiveVariant(GraphSnapshotState<Integer> state) {
        boolean directed = activeVariant == GraphVariant.DIRECTED;
        return state.directed() == directed;
    }

    private void requireGraphSnapshot(StructureSnapshot<GraphSnapshotState<Integer>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
    }

    private GraphStructure<Integer> graphFromSnapshot(GraphSnapshotState<Integer> snapshot) {
        return WeightedGraph.fromSnapshot(asWeightedSnapshot(snapshot));
    }

    private WeightedGraphSnapshot<Integer> asWeightedSnapshot(GraphSnapshotState<Integer> snapshot) {
        if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            @SuppressWarnings("unchecked")
            WeightedGraphSnapshot<Integer> typed = (WeightedGraphSnapshot<Integer>) weighted;
            return typed;
        }
        if (snapshot instanceof GraphSnapshot<?> basic) {
            @SuppressWarnings("unchecked")
            GraphSnapshot<Integer> typed = (GraphSnapshot<Integer>) basic;
            List<WeightedGraphSnapshot.Vertex<Integer>> vertices = typed.vertices().stream()
                    .map(vertex -> new WeightedGraphSnapshot.Vertex<>(vertex.id(), vertex.value()))
                    .toList();
            List<WeightedGraphSnapshot.Edge> edges = typed.edges().stream()
                    .map(edge -> new WeightedGraphSnapshot.Edge(
                            edge.id(), edge.fromId(), edge.toId(), 1.0d))
                    .toList();
            return new WeightedGraphSnapshot<>(typed.directed(), vertices, edges);
        }
        throw new IllegalArgumentException("unsupported graph snapshot type: " + snapshot.getClass().getName());
    }

    private List<Long> snapshotVertexIds(GraphSnapshotState<Integer> snapshot) {
        List<Long> ids = new ArrayList<>();
        if (snapshot instanceof GraphSnapshot<?> basic) {
            for (GraphSnapshot.Vertex<?> vertex : basic.vertices()) {
                ids.add(vertex.id());
            }
        } else if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            for (WeightedGraphSnapshot.Vertex<?> vertex : weighted.vertices()) {
                ids.add(vertex.id());
            }
        }
        return List.copyOf(ids);
    }

    private List<Long> snapshotEdgeIds(GraphSnapshotState<Integer> snapshot) {
        List<Long> ids = new ArrayList<>();
        if (snapshot instanceof GraphSnapshot<?> basic) {
            for (GraphSnapshot.Edge edge : basic.edges()) {
                ids.add(edge.id());
            }
        } else if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            for (WeightedGraphSnapshot.Edge edge : weighted.edges()) {
                ids.add(edge.id());
            }
        }
        return List.copyOf(ids);
    }

    private Long snapshotEdgeIdBetween(GraphSnapshotState<Integer> snapshot, long fromId, long toId) {
        if (snapshot instanceof GraphSnapshot<?> basic) {
            for (GraphSnapshot.Edge edge : basic.edges()) {
                if (edgeConnects(snapshot.directed(), edge.fromId(), edge.toId(), fromId, toId)) {
                    return edge.id();
                }
            }
        } else if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            for (WeightedGraphSnapshot.Edge edge : weighted.edges()) {
                if (edgeConnects(snapshot.directed(), edge.fromId(), edge.toId(), fromId, toId)) {
                    return edge.id();
                }
            }
        }
        return null;
    }

    private boolean edgeConnects(boolean directed, long edgeFrom, long edgeTo, long fromId, long toId) {
        if (edgeFrom == fromId && edgeTo == toId) {
            return true;
        }
        if (!directed && edgeFrom == toId && edgeTo == fromId) {
            return true;
        }
        return false;
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
