package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.event.structure.MultiGraphStructureEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.ValueMetadata;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.snapshot.MultiGraphSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.structure.graph.MultiEdge;
import com.majortom.algorithms.structure.graph.MultiGraph;
import com.majortom.algorithms.structure.graph.MultiGraphStructure;
import com.majortom.algorithms.structure.graph.Vertex;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.impl.visualizer.GraphVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.presenter.GraphPresenter;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.render.fx.FxDispatch;
import com.majortom.algorithms.visualization.render.runtime.RenderContext;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.runtime.graph.MultiGraphEventReducer;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapter;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import com.majortom.algorithms.visualization.structure.RuntimeValueTypeSupport;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * MultiGraph-specific data/algorithm adapter backed by the SAME GraphControls.fxml as GraphController.
 * Bulk construction is a Java API, not a second import form or control panel.
 */
public final class MultiGraphController extends BaseModuleController<GraphViewState>
    implements StructureSnapshotSupport<MultiGraphSnapshot<Object>>,
    AlgorithmSelectionSupport, GraphImplementationSelection, RuntimeValueTypeSupport {

  private List<String> algorithmIds = AlgorithmCatalog.multiGraphAlgorithms(Integer.class);
  private MultiGraph<Object> directedGraph = new MultiGraph<>(GraphDirection.DIRECTED);
  private MultiGraph<Object> undirectedGraph = new MultiGraph<>(GraphDirection.UNDIRECTED);
  private GraphDirection activeDirection = GraphDirection.UNDIRECTED;
  private Class<?> runtimeValueType = Integer.class;
  private ValueAdapter<Object> valueAdapter = ValueAdapters.requireObjectAdapter(Integer.class);
  private Consumer<String> algorithmSelectionListener = ignored -> { };
  private Consumer<GraphImplementation> implementationChangeListener = ignored -> { };
  private Consumer<GraphController.Selection> selectionListener = ignored -> { };
  private boolean structureSelectionEnabled = true;
  private Long selectedNodeId;
  private Long selectedEdgeId;

  // These are the SAME fx:ids as GraphController; no MultiGraph-specific FXML or widgets.
  @FXML private Label structureLabel;
  @FXML private Label algorithmLabel;
  @FXML private Label nodeOperationsLabel;
  @FXML private Label edgeOperationsLabel;
  @FXML private Label traversalLabel;
  @FXML private ComboBox<GraphImplementation> implementationSelector;
  @FXML private ComboBox<String> structureSelector;
  @FXML private ComboBox<String> algorithmSelector;
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

  public MultiGraphController(RenderContext context) {
    super(new GraphVisualizer(), new GraphPresenter(), "/fxml/GraphControls.fxml", context);
    graphVisualizer().setNodeSelectionListener(this::selectNode);
    graphVisualizer().setEdgeSelectionListener(this::selectEdge);
    renderGraph();
  }

  @Override public void initialize(URL location, ResourceBundle resources) {
    super.initialize(location, resources);
    GraphImplementationSelector.install(implementationSelector, GraphImplementation.MULTI,
        choice -> implementationChangeListener.accept(choice));
    structureSelector.setItems(FXCollections.observableArrayList(
        "label.graph.structure.undirected", "label.graph.structure.directed"));
    localizeChoiceCells(structureSelector, I18N::text);
    structureSelector.getSelectionModel().selectedIndexProperty().addListener(
        (observable, previous, current) -> {
          if (current == null || current.intValue() < 0) return;
          changeDirection(current.intValue() == 0
              ? GraphDirection.UNDIRECTED : GraphDirection.DIRECTED);
        });
    algorithmSelector.setItems(FXCollections.observableArrayList(algorithmIds));
    localizeChoiceCells(algorithmSelector, AlgorithmCatalog::name);
    algorithmSelector.getSelectionModel().selectedItemProperty().addListener(
        (observable, previous, current) -> algorithmSelectionListener.accept(current));
    FxDispatch.defer(() -> {
      selectActiveDirection();
      algorithmSelector.getSelectionModel().selectFirst();
      algorithmSelectionListener.accept(selectedAlgorithmId());
    });
    if (weightField.getText().isBlank()) weightField.setText("1");
  }

  private MultiGraph<Object> currentGraph() {
    return activeDirection.isDirected() ? directedGraph : undirectedGraph;
  }

  /** Existing untyped entry point permits heterogeneous nodes when explicitly requested. */
  public void loadBatch(Object data) {
    loadBatch(Object.class, data);
  }

  /**
   * Type-safe Java API for user-defined node values (City, Place, Province, etc.).
   * All nodes are validated by MultiGraph.from(direction, nodeType, data).
   * The JavaFX controller stores an erased copy only to bridge shared controls.
   */
  public <T> void loadBatch(Class<T> nodeType, Object data) {
    Objects.requireNonNull(nodeType, "nodeType");
    Objects.requireNonNull(data, "data");
    loadGraph(nodeType, MultiGraph.from(activeDirection, nodeType, data));
  }

  /** Programmatically display a preconstructed MultiGraph<T>, without adding import UI. */
  public <T> void loadGraph(Class<T> nodeType, MultiGraph<T> graph) {
    Objects.requireNonNull(nodeType, "nodeType");
    Objects.requireNonNull(graph, "graph");
    for (Vertex<T> node : graph.vertices()) nodeType.cast(node.value());
    // Snapshot restore detaches from the caller's mutable graph; no unsafe node writes occur.
    MultiGraph<Object> replacement = eraseGraph(MultiGraph.fromSnapshot(graph.snapshot()));
    if (!executeStructureOperation("batch-import", () -> {
      setImportedNodeType(nodeType);
      if (replacement.direction().isDirected()) directedGraph = replacement;
      else undirectedGraph = replacement;
      ExecutionEvents.structure(new MultiGraphStructureEvent.Reset(replacement.snapshot()));
      return null;
    })) return;
    activeDirection = replacement.direction();
    selectActiveDirection();
    refreshAlgorithmIds();
    clearSelection();
    renderGraph();
  }

  /** Java-only snapshot entry points, including an explicit generic type overload. */
  public void loadSnapshot(MultiGraphSnapshot<Object> snapshot) {
    loadSnapshot(Object.class, snapshot);
  }

  public <T> void loadSnapshot(Class<T> nodeType, MultiGraphSnapshot<T> snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    loadGraph(nodeType, MultiGraph.fromSnapshot(snapshot));
  }

  @SuppressWarnings("unchecked")
  private static <T> MultiGraph<Object> eraseGraph(MultiGraph<T> graph) {
    return (MultiGraph<Object>) (MultiGraph<?>) graph;
  }

  private void setImportedNodeType(Class<?> type) {
    if (runtimeValueType == type) return;
    runtimeValueType = type;
    valueAdapter = ValueAdapters.supports(type) ? ValueAdapters.requireObjectAdapter(type) : null;
    directedGraph = new MultiGraph<>(GraphDirection.DIRECTED);
    undirectedGraph = new MultiGraph<>(GraphDirection.UNDIRECTED);
    invalidateExecutionForStructureChange();
  }

  private void refreshAlgorithmIds() {
    String selected = algorithmSelector == null ? null
        : algorithmSelector.getSelectionModel().getSelectedItem();
    algorithmIds = AlgorithmCatalog.multiGraphAlgorithms(runtimeValueType);
    if (algorithmSelector != null) {
      algorithmSelector.setItems(FXCollections.observableArrayList(algorithmIds));
      if (selected != null && algorithmIds.contains(selected))
        algorithmSelector.getSelectionModel().select(selected);
      else algorithmSelector.getSelectionModel().selectFirst();
    }
    algorithmSelectionListener.accept(selectedAlgorithmId());
  }

  private void replaceCurrentGraph(MultiGraph<Object> next) {
    if (activeDirection.isDirected()) directedGraph = next;
    else undirectedGraph = next;
  }

  private void changeDirection(GraphDirection direction) {
    if (direction == activeDirection) return;
    activeDirection = direction;
    invalidateExecutionForInputChange();
    clearSelection();
    renderGraph();
    refreshStatsDisplay();
  }

  private void selectActiveDirection() {
    if (structureSelector != null)
      structureSelector.getSelectionModel().select(activeDirection.isDirected() ? 1 : 0);
  }

  @FXML private void handleAddNode() {
    Object value = parseValue(nodeField.getText());
    if (value == null) return;
    if (!executeStructureOperation("add-vertex", () -> currentGraph().addVertex(value))) return;
    Vertex<Object> added = null;
    for (Vertex<Object> vertex : currentGraph().vertices()) added = vertex;
    renderGraph();
    if (added != null) graphVisualizer().selectNode(added.id());
  }

  @FXML private void handleDeleteNode() {
    Vertex<Object> vertex = resolveVertex(nodeField.getText(), selectedNodeId);
    if (vertex == null) return;
    if (!executeStructureOperation("remove-vertex", () -> currentGraph().removeVertex(vertex.id()))) return;
    clearSelection();
    renderGraph();
  }

  @FXML private void handleAddEdge() {
    Vertex<Object> from = resolveVertex(fromField.getText(), null);
    Vertex<Object> to = resolveVertex(toField.getText(), null);
    Double weight = parseWeight();
    if (from == null || to == null || weight == null) return;
    if (!executeStructureOperation("add-edge", () -> currentGraph().addEdge(from, to, null, weight)))
      return;
    MultiEdge<Object> created = null;
    for (MultiEdge<Object> edge : currentGraph().edges()) created = edge;
    renderGraph();
    if (created != null) graphVisualizer().selectEdge(created.id());
  }

  @FXML private void handleDeleteEdge() {
    MultiEdge<Object> edge = resolveEdgeFromFields();
    if (edge == null) return;
    if (!executeStructureOperation("remove-edge", () -> currentGraph().removeEdge(edge.id()))) return;
    clearSelection();
    renderGraph();
  }

  @FXML private void handleSetWeight() {
    MultiEdge<Object> edge = resolveEdgeFromFields();
    Double weight = parseWeight();
    if (edge == null || weight == null) return;
    if (executeStructureOperation("set-weight", () -> currentGraph().setWeight(edge.id(), weight)))
      renderGraph();
  }

  private Object parseValue(String text) {
    if (valueAdapter == null) {
      appendLog("Custom nodes are created in code. Select an existing node to edit connections.");
      return null;
    }
    try { return valueAdapter.parse(text); }
    catch (RuntimeException failure) {
      logI18n("message.error.invalid_graph_value");
      return null;
    }
  }

  private Double parseWeight() {
    try {
      double weight = Double.parseDouble(weightField.getText().trim());
      if (!Double.isFinite(weight)) throw new IllegalArgumentException("weight must be finite");
      return weight;
    } catch (RuntimeException failure) {
      logI18n("message.error.invalid_graph_weight");
      return null;
    }
  }

  private Vertex<Object> resolveVertex(String text, Long preferredId) {
    if (preferredId != null) {
      Vertex<Object> selected = currentGraph().vertex(preferredId);
      if (selected != null && formatValue(selected.value()).equals(text == null ? "" : text.trim()))
        return selected;
    }
    Object value = valueAdapter == null ? null : parseValue(text);
    if (valueAdapter != null && value == null) return null;
    String label = text == null ? "" : text.trim();
    Vertex<Object> matched = null;
    for (Vertex<Object> candidate : currentGraph().vertices()) {
      if (valueAdapter == null
          ? !formatValue(candidate.value()).equals(label)
          : !candidate.value().equals(value)) continue;
      if (matched != null) {
        appendLog("More than one vertex has this value. Select the intended node first.");
        return null;
      }
      matched = candidate;
    }
    if (matched == null) logI18n("message.error.graph_node_missing");
    return matched;
  }

  private MultiEdge<Object> resolveEdgeFromFields() {
    if (selectedEdgeId != null) {
      MultiEdge<Object> selected = currentGraph().edge(selectedEdgeId);
      if (selected != null && formatValue(selected.from().value()).equals(fromField.getText().trim())
          && formatValue(selected.to().value()).equals(toField.getText().trim())) return selected;
    }
    Vertex<Object> from = resolveVertex(fromField.getText(), null);
    Vertex<Object> to = resolveVertex(toField.getText(), null);
    if (from == null || to == null) return null;
    List<MultiEdge<Object>> matching = currentGraph().edges(from, to);
    if (matching.isEmpty()) {
      logI18n("message.graph.edge_not_found", from.value(), to.value());
      return null;
    }
    if (selectedEdgeId != null) {
      for (MultiEdge<Object> edge : matching)
        if (edge.id() == selectedEdgeId) return edge;
    }
    if (matching.size() > 1) {
      appendLog("Multiple edges connect these nodes. Select the specific edge on the graph first.");
      return null;
    }
    return matching.getFirst();
  }

  private GraphVisualizer graphVisualizer() { return (GraphVisualizer) visualizer; }

  private void selectNode(long id) {
    if (!structureSelectionEnabled) return;
    Vertex<Object> node = currentGraph().vertex(id);
    if (node == null) return;
    selectedNodeId = id;
    selectedEdgeId = null;
    if (nodeField != null) nodeField.setText(formatValue(node.value()));
    int degree = 0;
    for (MultiEdge<Object> edge : currentGraph().edges())
      if (edge.from().id() == id || edge.to().id() == id) degree++;
    selectionListener.accept(new GraphController.NodeSelection(id, VisualValue.of(node.value()), degree));
    requestPresentationRender();
  }

  private void selectEdge(long id) {
    if (!structureSelectionEnabled) return;
    MultiEdge<Object> edge = currentGraph().edge(id);
    if (edge == null) return;
    selectedEdgeId = id;
    selectedNodeId = null;
    if (fromField != null) fromField.setText(formatValue(edge.from().value()));
    if (toField != null) toField.setText(formatValue(edge.to().value()));
    if (weightField != null) weightField.setText(Double.toString(edge.weight()));
    selectionListener.accept(new GraphController.EdgeSelection(id,
        VisualValue.of(edge.from().value()), VisualValue.of(edge.to().value()),
        currentGraph().direction().isDirected()));
    requestPresentationRender();
  }

  private String formatValue(Object value) {
    try { return valueAdapter.format(value); }
    catch (RuntimeException unsupported) { return ValueAdapters.summary(value); }
  }

  public void setSelectionListener(Consumer<GraphController.Selection> listener) {
    selectionListener = listener == null ? ignored -> { } : listener;
  }

  public void setStructureSelectionEnabled(boolean enabled) {
    if (structureSelectionEnabled != enabled) clearSelection();
    structureSelectionEnabled = enabled;
  }

  private void clearSelection() {
    selectedNodeId = null;
    selectedEdgeId = null;
    graphVisualizer().clearSelection();
    selectionListener.accept(null);
  }

  private void renderGraph() {
    renderStructureState(GraphViewState.initial(currentGraph().snapshot()));
    refreshStatsDisplay();
  }

  @Override protected String moduleId() { return StructureIds.GRAPH; }
  @Override protected void setupI18n() {
    bindText(structureLabel, "label.common.structure");
    bindText(algorithmLabel, "label.common.algorithm");
    bindText(nodeOperationsLabel, "label.graph.node_ops");
    bindText(edgeOperationsLabel, "label.graph.edge_ops");
    bindText(traversalLabel, "label.panel.execution");
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
  private void bindText(Label label, String key) {
    if (label != null) label.textProperty().bind(I18N.createStringBinding(key));
  }
  private void bindPrompt(TextField field, String key) {
    if (field != null) field.promptTextProperty().bind(I18N.createStringBinding(key));
  }
  private void bindButton(Button button, String key) {
    if (button != null) button.textProperty().bind(I18N.createStringBinding(key));
  }

  @Override public void setImplementationChangeListener(Consumer<GraphImplementation> listener) {
    implementationChangeListener = listener == null ? ignored -> { } : listener;
  }

  @Override public void handleAlgorithmStart() {
    String id = selectedAlgorithmId();
    if (id == null || currentGraph().vertexCount() == 0 || isRunning()) return;
    MultiGraphSnapshot<Object> snapshot = currentGraph().snapshot();
    for (var node : snapshot.vertices()) {
      if (!safeToReplay(node.value())) {
        appendLog("Replay requires immutable node payload: " + node.value().getClass().getName());
        return;
      }
    }
    for (var edge : snapshot.edges()) {
      if (edge.value() != null && !safeToReplay(edge.value())) {
        appendLog("Replay requires immutable edge payload: " + edge.value().getClass().getName());
        return;
      }
    }
    AlgorithmDescriptor descriptor = AlgorithmCatalog.multiGraphDescriptor(runtimeValueType, id);
    MultiGraph<Object> input = MultiGraph.fromSnapshot(snapshot);
    startAlgorithm(id, snapshot, () -> descriptor.invoke(input),
        () -> new MultiGraphEventReducer(snapshot));
  }

  private static boolean safeToReplay(Object value) {
    return ValueAdapters.canReplay(value.getClass())
        || ValueMetadata.describe(value.getClass()).immutable();
  }

  @Override public List<String> algorithmIds() { return algorithmIds; }
  @Override public String selectedAlgorithmId() {
    if (algorithmSelector == null) return algorithmIds.isEmpty() ? null : algorithmIds.getFirst();
    return algorithmSelector.getSelectionModel().getSelectedItem();
  }
  @Override public boolean selectAlgorithm(String id) {
    if (!algorithmIds.contains(id)) return false;
    if (algorithmSelector != null) algorithmSelector.getSelectionModel().select(id);
    algorithmSelectionListener.accept(id);
    return true;
  }
  @Override public void setAlgorithmSelectionListener(Consumer<String> listener) {
    algorithmSelectionListener = listener == null ? ignored -> { } : listener;
  }

  @Override public Class<?> runtimeValueType() { return runtimeValueType; }
  @Override public List<Class<?>> supportedValueTypes() {
    List<Class<?>> types = ValueAdapters.supportedTypes();
    if (types.contains(runtimeValueType)) return types;
    java.util.ArrayList<Class<?>> result = new java.util.ArrayList<>(types);
    result.add(runtimeValueType);
    return List.copyOf(result);
  }
  @Override public boolean hasValues() {
    return directedGraph.vertexCount() > 0 || undirectedGraph.vertexCount() > 0;
  }
  @Override public void setRuntimeValueType(Class<?> type) {
    if (!supportedValueTypes().contains(type))
      throw new IllegalArgumentException("Unsupported Graph value type: " + type.getName());
    if (runtimeValueType.equals(type)) return;
    runtimeValueType = type;
    valueAdapter = ValueAdapters.supports(type) ? ValueAdapters.requireObjectAdapter(type) : null;
    directedGraph = new MultiGraph<>(GraphDirection.DIRECTED);
    undirectedGraph = new MultiGraph<>(GraphDirection.UNDIRECTED);
    clearSelection();
    invalidateExecutionForStructureChange();
    refreshAlgorithmIds();
    if (controlPanel != null) renderGraph();
  }

  @Override protected String formatStatsMessage() {
    return I18N.text("stats.graph.nodes", currentGraph().vertexCount()) + " | "
        + I18N.text("stats.graph.edges", currentGraph().edgeCount());
  }
  @Override public String structurePrimaryCount() {
    return Integer.toString(currentGraph().vertexCount());
  }
  @Override public String structureSecondaryCount() {
    return Integer.toString(currentGraph().edgeCount());
  }
  @Override protected Object structureMemoryRoot() { return currentGraph(); }
  @Override protected void onResetData() { renderGraph(); }

  @Override public StructureSnapshot<MultiGraphSnapshot<Object>> captureStructureSnapshot() {
    return StructureSnapshot.create(moduleId(), runtimeValueType, currentGraph().snapshot());
  }
  @Override public void restoreStructureSnapshot(StructureSnapshot<MultiGraphSnapshot<Object>> snapshot) {
    requireSnapshot(snapshot);
    MultiGraph<Object> replacement = MultiGraph.fromSnapshot(snapshot.state());
    if (replacement.direction().isDirected()) directedGraph = replacement;
    else undirectedGraph = replacement;
    activeDirection = replacement.direction();
    selectActiveDirection();
    clearSelection();
    invalidateExecutionForStructureChange();
    renderGraph();
  }
  @Override public void previewStructureSnapshot(StructureSnapshot<MultiGraphSnapshot<Object>> snapshot) {
    requireSnapshot(snapshot);
    clearSelection();
    renderPreviewState(GraphViewState.initial(snapshot.state()));
  }
  private void requireSnapshot(StructureSnapshot<MultiGraphSnapshot<Object>> snapshot) {
    if (!moduleId().equals(snapshot.moduleId()))
      throw new IllegalArgumentException("Not a Graph snapshot");
    snapshot.requireValueType(runtimeValueType);
  }
  @Override public String describeStructureSnapshot(MultiGraphSnapshot<Object> state) {
    return I18N.text("snapshot.graph.detail", state.vertices().size(), state.edges().size());
  }
  @Override public String snapshotPrimaryCount(MultiGraphSnapshot<Object> state) {
    return Integer.toString(state.vertices().size());
  }
  @Override public String snapshotSecondaryCount(MultiGraphSnapshot<Object> state) {
    return Integer.toString(state.edges().size());
  }
}
