package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.annotation.*;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.runtime.graph.MultiGraphEventReducer;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.snapshot.MultiGraphSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.structure.graph.*;
import com.majortom.algorithms.visualization.impl.visualizer.GraphVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.presenter.GraphPresenter;
import com.majortom.algorithms.visualization.render.runtime.RenderContext;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/** Separate MultiGraph workbench reusing Graph rendering while preserving simple Graph behavior. */
public final class MultiGraphController extends BaseModuleController<GraphViewState>
    implements StructureSnapshotSupport<MultiGraphSnapshot<Object>>, AlgorithmSelectionSupport {
  private MultiGraph<Object> graph = new MultiGraph<>(GraphDirection.DIRECTED);
  private Object lastBatch;
  private static final AlgorithmTypeSignature SIGNATURE = AlgorithmTypeSignature.of(Object.class);
  private final List<String> algorithmIds = AlgorithmCatalog.compatibleAlgorithms(
      MultiGraphStructure.class, SIGNATURE);
  private Consumer<String> algorithmSelectionListener = ignored -> {};
  @FXML private ComboBox<String> algorithmSelector;
  private boolean structureSelectionEnabled = true;
  private Consumer<GraphController.Selection> selectionListener = ignored -> {};
  @FXML private ComboBox<GraphDirection> directionSelector;
  @FXML private Button sampleButton;
  @FXML private Label selectionInfo;

  public MultiGraphController(RenderContext context) {
    super(new GraphVisualizer(), new GraphPresenter(), "/fxml/MultiGraphControls.fxml", context);
    graphVisualizer().setNodeSelectionListener(this::selectNode);
    graphVisualizer().setEdgeSelectionListener(this::selectEdge);
    renderGraph();
  }
  @Override public void initialize(URL location, ResourceBundle resources) {
    super.initialize(location, resources);
    directionSelector.setItems(FXCollections.observableArrayList(
        GraphDirection.DIRECTED, GraphDirection.UNDIRECTED));
    directionSelector.getSelectionModel().select(graph.direction());
    directionSelector.valueProperty().addListener((observable, oldValue, next) -> {
      if (next != null && next != graph.direction()) changeDirection(next);
    });
    sampleButton.setOnAction(event -> loadSample());
    algorithmSelector.setItems(FXCollections.observableArrayList(algorithmIds));
    localizeChoiceCells(algorithmSelector, AlgorithmCatalog::name);
    algorithmSelector.getSelectionModel().selectFirst();
    algorithmSelector.getSelectionModel().selectedItemProperty().addListener(
        (observable, before, after) -> algorithmSelectionListener.accept(after));
  }
  private GraphVisualizer graphVisualizer() { return (GraphVisualizer) visualizer; }

  /** Library consumers can provide their own annotated batch object directly. */
  public void loadBatch(Object data) {
    if (data == null) throw new IllegalArgumentException("batch data cannot be null");
    if (executeStructureOperation("batch-import", () -> {
      graph = MultiGraph.from(graph.direction(), data);
      return null;
    })) {
      lastBatch = data;
      clearSelection();
      renderGraph();
      refreshStatsDisplay();
    }
  }

  private void changeDirection(GraphDirection next) {
    if (lastBatch != null) {
      if (executeStructureOperation("change-direction", () -> {
        graph = MultiGraph.from(next, lastBatch);
        return null;
      })) {
        clearSelection();
        renderGraph();
        refreshStatsDisplay();
      }
    } else {
      graph = new MultiGraph<>(next);
      clearSelection();
      renderGraph();
      refreshStatsDisplay();
    }
    if (directionSelector != null && graph.direction() != next)
      directionSelector.getSelectionModel().select(graph.direction());
  }

  @FXML private void loadSample() {
    City sz = new City("440300", "深圳");
    City gz = new City("440100", "广州");
    City dg = new City("441900", "东莞");
    loadBatch(new TransportNetwork(List.of(sz, gz, dg),
        List.of(new Railway(sz, gz, "广深高铁", 102)),
        List.of(new Road(sz, gz, "广深高速", true, 125))));
  }

  private void selectNode(long id) {
    if (!structureSelectionEnabled) return;
    Vertex<Object> node = graph.vertex(id);
    if (node == null) return;
    int degree = 0;
    for (MultiEdge<Object> edge : graph.edges())
      if (edge.from().id() == id || edge.to().id() == id) degree++;
    VisualValue value = VisualValue.of(node.value());
    selectionInfo.setText(value.text() + "\n" + value.projection().details());
    selectionListener.accept(new GraphController.NodeSelection(id, value, degree));
    requestPresentationRender();
  }

  private void selectEdge(long id) {
    if (!structureSelectionEnabled) return;
    MultiEdge<Object> edge = graph.edge(id);
    if (edge == null) return;
    String label = edge.value() == null ? "Edge" : ValueAdapters.project(edge.value()).summary();
    String details = edge.value() == null ? "" : ValueAdapters.project(edge.value()).details();
    selectionInfo.setText(label + " · " + edge.weight() + "\n" + details);
    selectionListener.accept(new GraphController.EdgeSelection(
        id, VisualValue.of(edge.from().value()), VisualValue.of(edge.to().value()),
        graph.direction().isDirected()));
    requestPresentationRender();
  }
  public void setSelectionListener(Consumer<GraphController.Selection> listener) {
    selectionListener = listener == null ? ignored -> {} : listener;
  }
  public void setStructureSelectionEnabled(boolean enabled) {
    structureSelectionEnabled = enabled;
    if (!enabled) clearSelection();
  }
  private void clearSelection() {
    graphVisualizer().clearSelection();
    if (selectionInfo != null) selectionInfo.setText("");
    selectionListener.accept(null);
  }
  private void renderGraph() { renderStructureState(GraphViewState.initial(graph.snapshot())); }

  @Override protected String moduleId() { return StructureIds.MULTI_GRAPH; }
  @Override protected void setupI18n() {
    if (sampleButton != null) sampleButton.setText("Load sample / 加载示例");
  }
  @Override public void handleAlgorithmStart() {
    String id = selectedAlgorithmId();
    if (id == null || graph.vertexCount() == 0 || isRunning()) return;
    MultiGraphSnapshot<Object> snapshot = graph.snapshot();
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
    AlgorithmDescriptor descriptor = AlgorithmCatalog.compatibleDescriptor(
        MultiGraphStructure.class, SIGNATURE, id);
    MultiGraph<Object> input = MultiGraph.fromSnapshot(snapshot);
    startAlgorithm(id, snapshot, () -> descriptor.invoke(input),
        () -> new MultiGraphEventReducer(snapshot));
  }
  private static boolean safeToReplay(Object value) {
    if (ValueAdapters.canReplay(value.getClass())) return true;
    ValueType definition = value.getClass().getAnnotation(ValueType.class);
    return definition != null && definition.immutable();
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
    algorithmSelectionListener = listener == null ? ignored -> {} : listener;
  }
  @Override protected String formatStatsMessage() {
    return "Nodes: " + graph.vertexCount() + " | Edges: " + graph.edgeCount();
  }
  @Override public String structurePrimaryCount() { return Integer.toString(graph.vertexCount()); }
  @Override public String structureSecondaryCount() { return Integer.toString(graph.edgeCount()); }
  @Override protected Object structureMemoryRoot() { return graph; }
  @Override protected void onResetData() {
    graph = new MultiGraph<>(graph.direction());
    lastBatch = null;
    clearSelection();
    renderGraph();
  }

  @Override public StructureSnapshot<MultiGraphSnapshot<Object>> captureStructureSnapshot() {
    return StructureSnapshot.create(moduleId(), graph.snapshot());
  }
  @Override public void restoreStructureSnapshot(StructureSnapshot<MultiGraphSnapshot<Object>> snapshot) {
    requireSnapshot(snapshot);
    graph = MultiGraph.fromSnapshot(snapshot.state());
    lastBatch = null;
    if (directionSelector != null) directionSelector.getSelectionModel().select(graph.direction());
    clearSelection();
    invalidateExecutionForStructureChange();
    renderGraph();
    refreshStatsDisplay();
  }
  @Override public void previewStructureSnapshot(StructureSnapshot<MultiGraphSnapshot<Object>> snapshot) {
    requireSnapshot(snapshot);
    clearSelection();
    renderPreviewState(GraphViewState.initial(snapshot.state()));
  }
  private void requireSnapshot(StructureSnapshot<MultiGraphSnapshot<Object>> snapshot) {
    if (!moduleId().equals(snapshot.moduleId())) throw new IllegalArgumentException("not a MultiGraph snapshot");
  }
  @Override public String describeStructureSnapshot(MultiGraphSnapshot<Object> state) {
    return state.vertices().size() + " nodes · " + state.edges().size() + " edges";
  }
  @Override public String snapshotPrimaryCount(MultiGraphSnapshot<Object> state) {
    return Integer.toString(state.vertices().size());
  }
  @Override public String snapshotSecondaryCount(MultiGraphSnapshot<Object> state) {
    return Integer.toString(state.edges().size());
  }

  /** Purely illustrative input; users import their own annotated data through loadBatch. */
  @ValueType(value = MultiGraphStructure.class, name = "城市", immutable = true)
  private record City(@ValueId String code,
      @ValueField(value = {ValueDisplay.SUMMARY, ValueDisplay.DETAIL}, label = "名称") String name) {}
  @ValueType(immutable = true)
  private record Railway(@GraphFrom City from, @GraphTo City to,
      @ValueField(value = ValueDisplay.SUMMARY, label = "线路") String line,
      @GraphWeight double kilometers) {}
  @ValueType(immutable = true)
  private record Road(@GraphFrom City from, @GraphTo City to,
      @ValueField(value = ValueDisplay.SUMMARY, label = "公路") String name,
      @GraphBidirectional boolean twoWay, @GraphWeight double kilometers) {}
  private record TransportNetwork(@GraphNodes List<City> cities,
      @GraphEdges List<Railway> railways, @GraphEdges List<Road> roads) {}
}
