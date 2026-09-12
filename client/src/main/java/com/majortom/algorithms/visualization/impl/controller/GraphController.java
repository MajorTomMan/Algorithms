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
import com.majortom.algorithms.visualization.impl.visualizer.GraphVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.graph.GraphEventReducer;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.structure.SnapshotAlgorithmInputSupport;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import com.majortom.algorithms.visualization.structure.RuntimeValueTypeSupport;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapter;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
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
        implements AlgorithmSelectionSupport, StructureSnapshotSupport<GraphSnapshotState<Object>>,
        SnapshotAlgorithmInputSupport<GraphSnapshotState<Object>>, RuntimeValueTypeSupport {

    private WeightedGraph<Object> undirectedGraph;
    private WeightedGraph<Object> directedGraph;
    private GraphVariant activeVariant = GraphVariant.UNDIRECTED;
    private List<String> algorithmIds = List.of();
    private StructureSnapshot<GraphSnapshotState<Object>> algorithmInputSnapshot;
    private Object startNode;
    private Class<?> runtimeValueType = Integer.class;
    private ValueAdapter<Object> valueAdapter = ValueAdapters.requireObjectAdapter(Integer.class);
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
            startField.setText(valueAdapter.format(startNode));
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
        GraphSnapshotState<Object> selectedSnapshot = selectedAlgorithmSnapshot();
        GraphFamilyAlgorithm<Object> algorithm = graphAlgorithm(algorithmId);
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
    private GraphFamilyAlgorithm<Object> graphAlgorithm(String algorithmId) {
        return (GraphFamilyAlgorithm<Object>) algorithm(algorithmId, runtimeValueType, GraphFamilyAlgorithm.class);
    }

    @SuppressWarnings("unchecked")
    private void runTraversal(
            String algorithmId,
            GraphFamilyAlgorithm<Object> algorithm,
            GraphSnapshotState<Object> inputSnapshot) {
        GraphTraversal<Object> traversal = (GraphTraversal<Object>) algorithm;
        GraphStructure<Object> inputGraph = graphFromSnapshot(inputSnapshot);
        if (inputGraph.isEmpty()) {
            return;
        }
        Object algorithmStartNode = startNode;
        if (inputGraph.vertex(algorithmStartNode) == null) {
            algorithmStartNode = firstVertexValue(inputGraph);
        }
        Object finalStartNode = algorithmStartNode;
        startAlgorithm(
                algorithmId,
                inputSnapshot,
                () -> traversal.traverse(inputGraph, finalStartNode),
                () -> new GraphEventReducer(inputSnapshot));
    }

    @SuppressWarnings("unchecked")
    private void runMinimumSpanning(
            String algorithmId,
            GraphFamilyAlgorithm<Object> algorithm,
            GraphSnapshotState<Object> inputSnapshot) {
        if (!(inputSnapshot instanceof WeightedGraphSnapshot<?> weighted)) {
            throw new IllegalArgumentException("minimum spanning algorithms require a weighted graph snapshot");
        }
        WeightedGraphSnapshot<Object> sourceSnapshot = (WeightedGraphSnapshot<Object>) weighted;
        WeightedGraph<Object> source = WeightedGraph.fromSnapshot(sourceSnapshot);
        WeightedGraphSnapshot<Object> resultSnapshot = new WeightedGraphSnapshot<>(
                false,
                sourceSnapshot.vertices(),
                List.of());
        WeightedGraph<Object> result = WeightedGraph.fromSnapshot(resultSnapshot);
        MinimumSpanningAlgorithm<Object> spanning = (MinimumSpanningAlgorithm<Object>) algorithm;
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
        Object from = parseNode(fromField.getText());
        Object to = parseNode(toField.getText());
        GraphStructure<Object> graph = currentGraph();
        if (from == null || to == null) {
            return;
        }
        Vertex<Object> fromVertex = graph.vertex(from);
        Vertex<Object> toVertex = graph.vertex(to);
        if (fromVertex == null || toVertex == null) {
            logI18n("message.error.graph_node_missing");
            return;
        }
        if (!graph.containsEdge(fromVertex, toVertex)) {
            logI18n("message.graph.edge_not_found", from, to);
            return;
        }
        GraphSnapshotState<Object> previousSnapshot = currentSnapshot();
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
        Object from = parseNode(fromField.getText());
        Object to = parseNode(toField.getText());
        Double weight = parseWeight(weightField.getText());
        if (from == null || to == null || weight == null) {
            return;
        }
        WeightedGraph<Object> graph = currentWeightedGraph();
        Vertex<Object> fromVertex = graph.vertex(from);
        Vertex<Object> toVertex = graph.vertex(to);
        if (fromVertex == null || toVertex == null) {
            logI18n("message.error.graph_node_missing");
            return;
        }
        Edge<Object> edge = graph.edge(fromVertex, toVertex);
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
        Object id = parseNode(text);
        GraphStructure<Object> graph = currentGraph();
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
            startField.setText(valueAdapter.format(startNode));
        }
        renderGraph();
        Vertex<Object> addedVertex = graph.vertex(id);
        if (addedVertex != null) {
            graphVisualizer().selectNode(addedVertex.id());
        }
        logI18n("message.graph.node_added", id);
    }

    private void deleteNode(String text) {
        Object id = parseNode(text);
        GraphStructure<Object> graph = currentGraph();
        if (id == null) {
            return;
        }
        Vertex<Object> removedVertex = graph.vertex(id);
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
        Object from = parseNode(fromText);
        Object to = parseNode(toText);
        WeightedGraph<Object> graph = currentWeightedGraph();
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
        Vertex<Object> fromVertex = graph.vertex(from);
        Vertex<Object> toVertex = graph.vertex(to);
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
        GraphSnapshotState<Object> snapshot = currentSnapshot();
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
        GraphSnapshotState<Object> snapshot = currentSnapshot();
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
        Object id = parseNode(text);
        GraphStructure<Object> graph = currentGraph();
        if (id == null || graph.vertex(id) == null) {
            appendLog(I18N.text("message.error.invalid_graph_start", text));
            return;
        }
        if (!java.util.Objects.equals(startNode, id)) {
            invalidateExecutionForInputChange();
            startNode = id;
            renderGraph();
        }
        appendLog(I18N.text("message.graph.start_set", id));
    }

    private Object parseNode(String text) {
        try {
            return valueAdapter.parse(text);
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

    private void syncStartNode(GraphStructure<Object> graph) {
        if (graph.isEmpty()) {
            startNode = null;
            if (startField != null) {
                startField.clear();
            }
            return;
        }
        if (graph.vertex(startNode) == null) {
            startNode = firstVertexValue(graph);
        }
        if (startField != null) {
            startField.setText(valueAdapter.format(startNode));
        }
    }

    private void renderGraph() {
        renderStructureState(GraphViewState.initial(currentSnapshot()));
    }

    @Override
    public StructureSnapshot<GraphSnapshotState<Object>> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), runtimeValueType, currentSnapshot());
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<GraphSnapshotState<Object>> snapshot) {
        requireGraphSnapshot(snapshot);
        WeightedGraphSnapshot<Object> state = asWeightedSnapshot(snapshot.state());
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
    public void useSnapshotAsAlgorithmInput(StructureSnapshot<GraphSnapshotState<Object>> snapshot) {
        requireGraphSnapshot(snapshot);
        GraphSnapshotState<Object> state = snapshot.state();
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
    public void previewStructureSnapshot(StructureSnapshot<GraphSnapshotState<Object>> snapshot) {
        requireGraphSnapshot(snapshot);
        clearVisualSelection();
        renderPreviewState(GraphViewState.initial(snapshot.state()));
    }

    @Override
    public String describeStructureSnapshot(GraphSnapshotState<Object> state) {
        if (state instanceof GraphSnapshot<?> basic) {
            return I18N.text("snapshot.graph.detail", basic.vertices().size(), basic.edges().size());
        }
        if (state instanceof WeightedGraphSnapshot<?> weighted) {
            return I18N.text("snapshot.graph.detail", weighted.vertices().size(), weighted.edges().size());
        }
        throw new IllegalArgumentException("unsupported graph snapshot type: " + state.getClass().getName());
    }

    @Override
    public String snapshotPrimaryCount(GraphSnapshotState<Object> state) {
        if (state instanceof GraphSnapshot<?> basic) {
            return Integer.toString(basic.vertices().size());
        }
        if (state instanceof WeightedGraphSnapshot<?> weighted) {
            return Integer.toString(weighted.vertices().size());
        }
        return "—";
    }

    @Override
    public String snapshotSecondaryCount(GraphSnapshotState<Object> state) {
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
        GraphStructure<Object> graph = currentGraph();
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
            algorithmIds = AlgorithmCatalog.weightedGraphAlgorithms(runtimeValueType);
        } else {
            algorithmIds = AlgorithmCatalog.basicGraphAlgorithms(runtimeValueType);
        }
    }

    private void refreshAlgorithmSelector() {
        if (algorithmSelector == null) {
            return;
        }
        String previousId = selectedAlgorithmId();
        javafx.collections.ObservableList<String> labels = FXCollections.observableArrayList();
        for (String id : algorithmIds) {
            labels.add(AlgorithmCatalog.name(id));
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
        boolean traversal = algorithmId != null && AlgorithmCatalog.graphTraversals(runtimeValueType).contains(algorithmId);
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

    private WeightedGraph<Object> randomWeightedGraph(int nodeCount, int edgeCount, boolean directed) {
        WeightedGraph<Object> result = new WeightedGraph<>(directed);
        GraphBatch batch = randomGraphBatch(nodeCount, edgeCount, directed, new Random(0x5EEDL));
        result.initializeWeighted(weightedAdjacency(batch, directed));
        return result;
    }

    private List<Object> defaultGraphValues(int nodeCount) {
        List<Object> values = new ArrayList<>(nodeCount);
        for (int index = 0; index < nodeCount; index++) {
            if (runtimeValueType == Integer.class) {
                values.add(index);
            } else if (runtimeValueType == String.class) {
                values.add("N" + index);
            } else {
                throw new IllegalStateException("Unsupported Graph value type: " + runtimeValueType.getName());
            }
        }
        return List.copyOf(values);
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
        List<Object> nodes = parseBatchInput(sections[0], valueAdapter);
        if (nodes == null) {
            return null;
        }
        Set<Object> nodeSet = new LinkedHashSet<>(nodes);
        if (nodeSet.size() != nodes.size()) {
            logI18n("message.error.bulk_duplicates");
            return null;
        }
        List<GraphBatchEdge> edges = new ArrayList<>();
        if (sections.length == 2 && !sections[1].isBlank()) {
            String[] edgeTokens = sections[1].trim().split("[,;\\s]+");
            for (String token : edgeTokens) {
                if (token.isBlank()) {
                    continue;
                }
                GraphBatchEdge edge = parseGraphEdge(token);
                if (edge == null) {
                    return null;
                }
                if (!nodeSet.contains(edge.from()) || !nodeSet.contains(edge.to())) {
                    logI18n("message.error.graph_bulk_endpoint", token);
                    return null;
                }
                edges.add(edge);
            }
        }
        return new GraphBatch(List.copyOf(nodes), List.copyOf(edges));
    }

    private GraphBatchEdge parseGraphEdge(String token) {
        String relation = token;
        double weight = 1.0d;
        int weightSeparator = token.lastIndexOf(':');
        if (weightSeparator >= 0) {
            relation = token.substring(0, weightSeparator);
            try {
                weight = Double.parseDouble(token.substring(weightSeparator + 1));
            } catch (RuntimeException exception) {
                logI18n("message.error.bulk_input_invalid");
                return null;
            }
        }
        if (!Double.isFinite(weight)) {
            logI18n("message.error.invalid_graph_weight");
            return null;
        }

        String left;
        String right;
        int separator = relation.indexOf("->");
        int separatorLength = 2;
        if (separator < 0) {
            separator = relation.indexOf('>');
            separatorLength = 1;
        }
        if (separator >= 0) {
            left = relation.substring(0, separator);
            right = relation.substring(separator + separatorLength);
        } else if (runtimeValueType == Integer.class) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("^(-?\\d+)\\s*-\\s*(-?\\d+)$")
                    .matcher(relation);
            if (!matcher.matches()) {
                logI18n("message.error.bulk_input_invalid");
                return null;
            }
            left = matcher.group(1);
            right = matcher.group(2);
        } else {
            logI18n("message.error.bulk_input_invalid");
            return null;
        }

        try {
            return new GraphBatchEdge(valueAdapter.parse(left), valueAdapter.parse(right), weight);
        } catch (RuntimeException exception) {
            logI18n("message.error.bulk_input_invalid");
            return null;
        }
    }

    private GraphBatch randomGraphBatch(int nodeCount, int edgeCount) {
        return randomGraphBatch(nodeCount, edgeCount, activeVariant == GraphVariant.DIRECTED, new Random());
    }

    private GraphBatch randomGraphBatch(int nodeCount, int edgeCount, boolean directed, Random random) {
        List<Object> nodes = defaultGraphValues(nodeCount);
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
        List<GraphBatchEdge> batchEdges = new ArrayList<>();
        for (String edge : edges) {
            String[] parts = edge.split(":", 2);
            batchEdges.add(new GraphBatchEdge(
                    nodes.get(Integer.parseInt(parts[0])),
                    nodes.get(Integer.parseInt(parts[1])),
                    1.0d + random.nextInt(20)));
        }
        return new GraphBatch(List.copyOf(nodes), List.copyOf(batchEdges));
    }

    private java.util.Map<Object, java.util.Map<Object, Double>> weightedAdjacency(
            GraphBatch batch, boolean directed) {
        java.util.LinkedHashMap<Object, java.util.Map<Object, Double>> adjacency = new java.util.LinkedHashMap<>();
        for (Object node : batch.nodes()) {
            adjacency.put(node, new java.util.LinkedHashMap<>());
        }
        for (GraphBatchEdge edge : batch.edges()) {
            adjacency.get(edge.from()).put(edge.to(), edge.weight());
            if (!directed) {
                adjacency.get(edge.to()).put(edge.from(), edge.weight());
            }
        }
        return adjacency;
    }

    private void replaceGraphData(GraphBatch batch, String operationId, String messageKey) {
        WeightedGraph<Object> graph = currentWeightedGraph();
        clearVisualSelection();
        if (!executeStructureOperation(operationId, () -> {
            graph.initializeWeighted(weightedAdjacency(batch, graph.isDirected()));
            return null;
        })) {
            return;
        }
        syncStartNode(graph);
        renderGraph();
        refreshStatsDisplay();
        if (!batch.nodes().isEmpty()) {
            Vertex<Object> selected = graph.vertex(batch.nodes().getFirst());
            if (selected != null) {
                graphVisualizer().selectNode(selected.id());
            }
        }
        logI18n(messageKey, batch.nodes().size());
    }

    private record GraphBatch(List<Object> nodes, List<GraphBatchEdge> edges) {
    }

    private record GraphBatchEdge(Object from, Object to, double weight) {
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
        GraphSnapshotState<Object> snapshot = currentSnapshot();
        Object value = snapshotVertexValue(snapshot, nodeId);
        if (value == null) {
            clearVisualSelection();
            return;
        }
        int degree = snapshotDegree(snapshot, nodeId);
        String text = valueAdapter.format(value);
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
        GraphSnapshotState<Object> snapshot = currentSnapshot();
        SnapshotEdge edge = snapshotEdge(snapshot, edgeId);
        if (edge == null) {
            clearVisualSelection();
            return;
        }
        Object from = snapshotVertexValue(snapshot, edge.fromId());
        Object to = snapshotVertexValue(snapshot, edge.toId());
        if (from == null || to == null) {
            clearVisualSelection();
            return;
        }
        if (fromField != null) {
            fromField.setText(valueAdapter.format(from));
        }
        if (toField != null) {
            toField.setText(valueAdapter.format(to));
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

    private GraphStructure<Object> currentGraph() {
        return currentWeightedGraph();
    }

    private WeightedGraph<Object> currentWeightedGraph() {
        if (activeVariant == GraphVariant.UNDIRECTED) {
            return undirectedGraph;
        }
        return directedGraph;
    }

    private GraphSnapshotState<Object> currentSnapshot() {
        return currentWeightedGraph().snapshot();
    }

    private GraphSnapshotState<Object> selectedAlgorithmSnapshot() {
        GraphSnapshotState<Object> state;
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

    private boolean snapshotMatchesActiveVariant(GraphSnapshotState<Object> state) {
        boolean directed = activeVariant == GraphVariant.DIRECTED;
        return state.directed() == directed;
    }

    private void requireGraphSnapshot(StructureSnapshot<GraphSnapshotState<Object>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        snapshot.requireValueType(runtimeValueType);
    }

    private GraphStructure<Object> graphFromSnapshot(GraphSnapshotState<Object> snapshot) {
        return WeightedGraph.fromSnapshot(asWeightedSnapshot(snapshot));
    }

    private WeightedGraphSnapshot<Object> asWeightedSnapshot(GraphSnapshotState<Object> snapshot) {
        if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            @SuppressWarnings("unchecked")
            WeightedGraphSnapshot<Object> typed = (WeightedGraphSnapshot<Object>) weighted;
            return typed;
        }
        if (snapshot instanceof GraphSnapshot<?> basic) {
            @SuppressWarnings("unchecked")
            GraphSnapshot<Object> typed = (GraphSnapshot<Object>) basic;
            List<WeightedGraphSnapshot.Vertex<Object>> vertices = typed.vertices().stream()
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

    private List<Long> snapshotVertexIds(GraphSnapshotState<Object> snapshot) {
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

    private List<Long> snapshotEdgeIds(GraphSnapshotState<Object> snapshot) {
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

    private Long snapshotEdgeIdBetween(GraphSnapshotState<Object> snapshot, long fromId, long toId) {
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

    private Object snapshotVertexValue(GraphSnapshotState<Object> snapshot, long nodeId) {
        if (snapshot instanceof GraphSnapshot<?> basic) {
            for (GraphSnapshot.Vertex<?> vertex : basic.vertices()) {
                if (vertex.id() == nodeId) {
                    return vertex.value();
                }
            }
            return null;
        }
        if (snapshot instanceof WeightedGraphSnapshot<?> weighted) {
            for (WeightedGraphSnapshot.Vertex<?> vertex : weighted.vertices()) {
                if (vertex.id() == nodeId) {
                    return vertex.value();
                }
            }
            return null;
        }
        return null;
    }

    private int snapshotDegree(GraphSnapshotState<Object> snapshot, long nodeId) {
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

    private SnapshotEdge snapshotEdge(GraphSnapshotState<Object> snapshot, long edgeId) {
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

    private Object firstVertexValue(GraphStructure<Object> source) {
        for (Vertex<Object> vertex : source.vertices()) {
            return vertex.value();
        }
        throw new IllegalStateException("graph is empty");
    }

    @Override
    public Class<?> runtimeValueType() {
        return runtimeValueType;
    }

    @Override
    public List<Class<?>> supportedValueTypes() {
        return ValueAdapters.supportedTypes();
    }

    @Override
    public void setRuntimeValueType(Class<?> valueType) {
        if (!supportedValueTypes().contains(valueType)) {
            throw new IllegalArgumentException("Unsupported Graph value type: " + valueType.getName());
        }
        if (runtimeValueType.equals(valueType)) {
            return;
        }
        runtimeValueType = valueType;
        valueAdapter = ValueAdapters.requireObjectAdapter(valueType);
        algorithmInputSnapshot = null;
        clearVisualSelection();
        undirectedGraph = randomWeightedGraph(10, 16, false);
        directedGraph = randomWeightedGraph(10, 16, true);
        startNode = firstVertexValue(currentGraph());
        refreshAlgorithmIds();
        invalidateExecutionForStructureChange();
        if (startField != null) {
            startField.setText(valueAdapter.format(startNode));
        }
        if (controlPanel != null) {
            refreshVariantControls();
            renderGraph();
            refreshStatsDisplay();
        }
    }

}
