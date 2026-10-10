package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.statistics.MetricKeys;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.visualization.render.runtime.RenderContext;
import com.majortom.algorithms.visualization.render.fx.FxDispatch;

import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.structure.graph.Edge;
import com.majortom.algorithms.structure.graph.Vertex;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.graph.GraphData;
import com.majortom.algorithms.structure.graph.GraphInitializer;
import com.majortom.algorithms.structure.graph.GraphStructure;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.impl.visualizer.GraphVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.presenter.GraphPresenter;
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
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import static com.majortom.algorithms.visualization.impl.controller.GraphSnapshotQueries.*;
import com.majortom.algorithms.visualization.impl.controller.GraphSnapshotQueries.SnapshotEdge;
import com.majortom.algorithms.visualization.impl.controller.GraphBatchParser.GraphBatch;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public final class GraphController extends BaseModuleController<GraphViewState>
        implements AlgorithmSelectionSupport, StructureSnapshotSupport<GraphSnapshot<Object>>,
        SnapshotAlgorithmInputSupport<GraphSnapshot<Object>>, RuntimeValueTypeSupport {

    private Graph<Object> undirectedGraph;
    private Graph<Object> directedGraph;
    private GraphDirection activeDirection = GraphDirection.UNDIRECTED;
    private List<String> algorithmIds = List.of();
    private StructureSnapshot<GraphSnapshot<Object>> algorithmInputSnapshot;
    private Class<?> runtimeValueType = Integer.class;
    private ValueAdapter<Object> valueAdapter = ValueAdapters.requireObjectAdapter(Integer.class);
    private boolean structureSelectionEnabled = true;
    private Long algorithmSelectedNodeId;
    private Long algorithmSelectedEdgeId;
    private Consumer<Selection> selectionListener = ignored -> { };
    private Consumer<String> algorithmSelectionListener = ignored -> { };
    private AlgorithmSelectorBinder algorithmBinder;

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
    @FXML private Button addNodeBtn;
    @FXML private Button deleteNodeBtn;
    @FXML private Button addEdgeBtn;
    @FXML private Button deleteEdgeBtn;
    @FXML private Button setWeightBtn;
    @FXML private Button runBtn;

    public GraphController(RenderContext renderContext) {
        super(new GraphVisualizer(), new GraphPresenter(), "/fxml/GraphControls.fxml", renderContext);
        undirectedGraph = GraphDataFactory.randomGraph(runtimeValueType, 10, 16, GraphDirection.UNDIRECTED);
        directedGraph = GraphDataFactory.randomGraph(runtimeValueType, 10, 16, GraphDirection.DIRECTED);
        graphVisualizer().setNodeSelectionListener(this::handleVisualNodeSelection);
        graphVisualizer().setEdgeSelectionListener(this::handleVisualEdgeSelection);
        refreshAlgorithmIds();
        renderGraph();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bindSelectors();
        if (weightField != null && weightField.getText().isBlank()) {
            weightField.setText("1");
        }
        EffectUtils.applyDynamicEffect(
                addNodeBtn, deleteNodeBtn, addEdgeBtn,
                deleteEdgeBtn, setWeightBtn, runBtn);
        refreshDirectionControls();
    }

    @Override
    @FXML
    public void handleAlgorithmStart() {
        if (!ensureReplayableValue(runtimeValueType)) return;
        if (isRunning()) {
            return;
        }
        String algorithmId = selectedAlgorithmId();
        if (algorithmId == null) {
            return;
        }
        GraphSnapshot<Object> inputSnapshot = selectedAlgorithmSnapshot();
        GraphStructure<Object> inputGraph = graphFromSnapshot(inputSnapshot);
        if (inputGraph.isEmpty()) {
            return;
        }
        AlgorithmDescriptor descriptor = algorithm(algorithmId, AlgorithmTypeSignature.of(runtimeValueType));
        // A minimum spanning tree is built in a separate graph. Present the original
        // vertices without the source edges, so each emitted EdgeAdded builds the
        // result instead of overlaying it on the input graph.
        GraphSnapshot<Object> presentationSnapshot = inputSnapshot;
        if ("kruskal-minimum-spanning".equals(algorithmId)) {
            presentationSnapshot = new GraphSnapshot<>(
                    GraphDirection.UNDIRECTED, inputSnapshot.vertices(), List.of());
        }
        GraphSnapshot<Object> initialPresentation = presentationSnapshot;
        startAlgorithm(
                algorithmId,
                inputSnapshot,
                () -> descriptor.invoke(inputGraph),
                () -> new GraphEventReducer(initialPresentation));
    }

    @Override
    public boolean selectAlgorithm(String algorithmId) {
        if (algorithmId == null || !algorithmIds.contains(algorithmId)) {
            return false;
        }
        if (algorithmBinder != null) {
            algorithmBinder.select(algorithmId);
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
        GraphSnapshot<Object> previousSnapshot = currentSnapshot();
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
        Graph<Object> graph = currentGraph();
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
        if (!executeStructureOperation("add-vertex", () -> {
            graph.addVertex(id);
            return null;
        })) {
            return;
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
        renderGraph();
        selectGraphNodeAfterRemoval(previousNodeOrder, removedIndex);
        logI18n("message.graph.node_deleted", id);
    }

    private void linkNodes(String fromText, String toText) {
        Object from = parseNode(fromText);
        Object to = parseNode(toText);
        Graph<Object> graph = currentGraph();
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
        GraphSnapshot<Object> snapshot = currentSnapshot();
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
        GraphSnapshot<Object> snapshot = currentSnapshot();
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

    private void activateDirection(GraphDirection direction) {
        if (activeDirection == direction) {
            refreshDirectionControls();
            return;
        }
        algorithmInputSnapshot = null;
        invalidateExecutionForInputChange();
        activeDirection = direction;
        clearVisualSelection();
        refreshAlgorithmIds();
        syncStructureSelectorSelection();
        refreshDirectionControls();
        renderGraph();
        refreshStatsDisplay();
    }

    private void renderGraph() {
        renderStructureState(GraphViewState.initial(currentSnapshot()));
    }

    @Override
    public StructureSnapshot<GraphSnapshot<Object>> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), runtimeValueType, currentSnapshot());
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<GraphSnapshot<Object>> snapshot) {
        requireGraphSnapshot(snapshot);
        GraphSnapshot<Object> state = snapshot.state();
        if (state.direction().isDirected()) {
            directedGraph = Graph.fromSnapshot(state);
            activateDirection(GraphDirection.DIRECTED);
        } else {
            undirectedGraph = Graph.fromSnapshot(state);
            activateDirection(GraphDirection.UNDIRECTED);
        }
        algorithmInputSnapshot = null;
        invalidateExecutionForStructureChange();
        clearVisualSelection();
        renderGraph();
        refreshStatsDisplay();
    }

    @Override
    public void useSnapshotAsAlgorithmInput(StructureSnapshot<GraphSnapshot<Object>> snapshot) {
        requireGraphSnapshot(snapshot);
        GraphSnapshot<Object> state = snapshot.state();
        if (state.direction().isDirected()) {
            activateDirection(GraphDirection.DIRECTED);
        } else {
            activateDirection(GraphDirection.UNDIRECTED);
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
    public void previewStructureSnapshot(StructureSnapshot<GraphSnapshot<Object>> snapshot) {
        requireGraphSnapshot(snapshot);
        clearVisualSelection();
        renderPreviewState(GraphViewState.initial(snapshot.state()));
    }

    @Override
    public String describeStructureSnapshot(GraphSnapshot<Object> state) {
        return I18N.text("snapshot.graph.detail", state.vertices().size(), state.edges().size());
    }

    @Override
    public String snapshotPrimaryCount(GraphSnapshot<Object> state) {
        return Integer.toString(state.vertices().size());
    }

    @Override
    public String snapshotSecondaryCount(GraphSnapshot<Object> state) {
        return Integer.toString(state.edges().size());
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
                formatMetric("stats.action", stats.metric(MetricKeys.NODES_VISITED)
                        + stats.metric(MetricKeys.EDGES_EXAMINED)));
    }

    @Override
    protected Object structureMemoryRoot() {
        return currentGraph();
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
        bindLabel(traversalLabel, "label.panel.execution");
        bindPrompt(nodeField, "prompt.graph.node");
        bindPrompt(fromField, "prompt.graph.from");
        bindPrompt(toField, "prompt.graph.to");
        bindPrompt(weightField, "prompt.graph.weight");
        bindButton(addNodeBtn, "action.graph.add");
        bindButton(deleteNodeBtn, "action.graph.delete");
        bindButton(addEdgeBtn, "action.graph.link");
        bindButton(deleteEdgeBtn, "action.graph.delete_edge");
        bindButton(setWeightBtn, "action.graph.set_weight");
        bindButton(runBtn, "action.graph.run");
    }

    @Override
    protected String moduleId() {
        return StructureIds.GRAPH;
    }

    @Override
    public String selectedAlgorithmId() {
        if (algorithmBinder != null) return algorithmBinder.selectedId();
        return algorithmIds.isEmpty() ? null : algorithmIds.getFirst();
    }

    private void bindSelectors() {
        structureSelector.setItems(FXCollections.observableArrayList(
                "label.graph.structure.undirected", "label.graph.structure.directed"));
        localizeChoiceCells(structureSelector, I18N::text);
        algorithmBinder = new AlgorithmSelectorBinder(algorithmSelector, this::algorithmIds,
                ignored -> notifyAlgorithmSelection());
        structureSelector.getSelectionModel().selectedIndexProperty().addListener((observable, previous, current) -> {
            if (current == null || current.intValue() < 0) {
                return;
            }
            if (current.intValue() == 0) {
                activateDirection(GraphDirection.UNDIRECTED);
            } else {
                activateDirection(GraphDirection.DIRECTED);
            }
        });
        FxDispatch.defer(() -> {
            syncStructureSelectorSelection();
            refreshAlgorithmSelector();
        });
    }

    private void refreshDirectionControls() {
        refreshAlgorithmIds();
        refreshAlgorithmSelector();
        setVisibleManaged(weightField, true);
        setVisibleManaged(setWeightBtn, true);
    }

    private void refreshAlgorithmIds() {
        algorithmIds = AlgorithmCatalog.compatibleAlgorithms(
                GraphStructure.class, AlgorithmTypeSignature.of(runtimeValueType));
    }

    private void refreshAlgorithmSelector() {
        if (algorithmBinder != null) algorithmBinder.refresh();
        notifyAlgorithmSelection();
    }

    private void notifyAlgorithmSelection() {
        algorithmSelectionListener.accept(selectedAlgorithmId());
    }


    private void syncStructureSelectorSelection() {
        if (structureSelector == null) {
            return;
        }
        if (activeDirection == GraphDirection.UNDIRECTED) {
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
        GraphBatch batch = GraphBatchParser.parse(input, valueAdapter, runtimeValueType,
                text -> parseBatchInput(text, valueAdapter), this::logI18n);
        if (batch == null) {
            return;
        }
        replaceGraphData(batch, "bulk-replace", "message.data.bulk_applied");
    }

    @Override
    protected boolean canGenerateRandomData() {
        return ValueAdapters.canGenerate(runtimeValueType)
                && ValueAdapters.canGenerateDistinct(runtimeValueType, 1);
    }

    @Override
    protected void randomizeData() {
        int vertices = Math.min(10, ValueAdapters.maxDistinctSamples(runtimeValueType));
        int maxEdges = vertices * (vertices - 1) / 2;
        if (activeDirection.isDirected()) {
            maxEdges = vertices * (vertices - 1);
        }
        int edges = Math.min(16, maxEdges);
        replaceGraphData(GraphDataFactory.randomGraphBatch(runtimeValueType, vertices, edges,
                activeDirection, new Random()), "randomize", "message.data.randomized");
    }

    private void replaceGraphData(GraphBatch batch, String operationId, String messageKey) {
        GraphData<Object> data = GraphDataFactory.graphData(batch, activeDirection);
        Graph<Object> replacement;
        try {
            replacement = new GraphInitializer<Object>().create(data);
        } catch (IllegalArgumentException exception) {
            logI18n("message.error.bulk_input_invalid");
            return;
        }

        // The replacement is fully validated before changing the live structure.
        if (!executeStructureOperation(operationId, () -> {
            if (activeDirection == GraphDirection.DIRECTED) {
                directedGraph = replacement;
            } else {
                undirectedGraph = replacement;
            }
            return null;
        })) {
            return;
        }
        clearVisualSelection();
        renderGraph();
        refreshStatsDisplay();
        if (!batch.nodes().isEmpty()) {
            Vertex<Object> selected = replacement.vertex(batch.nodes().getFirst());
            if (selected != null) {
                graphVisualizer().selectNode(selected.id());
            }
        }
        logI18n(messageKey, batch.nodes().size());
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
        requestPresentationRender();
        if (!structureSelectionEnabled) {
            handleAlgorithmNodeSelection(nodeId);
            return;
        }
        GraphSnapshot<Object> snapshot = currentSnapshot();
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
        requestPresentationRender();
        if (!structureSelectionEnabled) {
            handleAlgorithmEdgeSelection(edgeId);
            return;
        }
        GraphSnapshot<Object> snapshot = currentSnapshot();
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
        if (weightField != null) {
            weightField.setText(formatWeight(edge.weight()));
        }
        selectionListener.accept(new EdgeSelection(edgeId, VisualValue.of(from), VisualValue.of(to), snapshot.direction().isDirected()));
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
        selectionListener.accept(new EdgeSelection(edgeId, from.value(), to.value(), state.direction().isDirected()));
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
            } else {
                requestPresentationRender();
            }
            return;
        }
        if (algorithmSelectedEdgeId != null) {
            long edgeId = algorithmSelectedEdgeId;
            if (!graphVisualizer().showEdgeSelection(edgeId) || !publishAlgorithmEdgeSelection(state, edgeId)) {
                clearVisualSelection();
            } else {
                requestPresentationRender();
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
        requestPresentationRender();
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

    private Graph<Object> currentGraph() {
        if (activeDirection == GraphDirection.UNDIRECTED) {
            return undirectedGraph;
        }
        return directedGraph;
    }

    private GraphSnapshot<Object> currentSnapshot() {
        return currentGraph().snapshot();
    }

    private GraphSnapshot<Object> selectedAlgorithmSnapshot() {
        GraphSnapshot<Object> state;
        if (algorithmInputSnapshot == null) {
            state = currentSnapshot();
        } else {
            state = algorithmInputSnapshot.state();
        }
        if (!snapshotMatchesActiveDirection(state)) {
            throw new IllegalStateException("algorithm input graph direction does not match active structure");
        }
        return state;
    }

    private boolean snapshotMatchesActiveDirection(GraphSnapshot<Object> state) {
        return state.direction() == activeDirection;
    }

    private void requireGraphSnapshot(StructureSnapshot<GraphSnapshot<Object>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        snapshot.requireValueType(runtimeValueType);
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

    @Override public boolean hasValues() {
        return undirectedGraph.vertices().iterator().hasNext() || directedGraph.vertices().iterator().hasNext();
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
        ValueAdapter<Object> nextAdapter = ValueAdapters.requireObjectAdapter(valueType);
        runtimeValueType = valueType;
        valueAdapter = nextAdapter;
        refreshRandomDataButton();
        algorithmInputSnapshot = null;
        clearVisualSelection();
        undirectedGraph = new Graph<>(GraphDirection.UNDIRECTED);
        directedGraph = new Graph<>(GraphDirection.DIRECTED);
        refreshAlgorithmIds();
        invalidateExecutionForStructureChange();
        if (controlPanel != null) {
            refreshDirectionControls();
            renderGraph();
            refreshStatsDisplay();
        }
    }

}
