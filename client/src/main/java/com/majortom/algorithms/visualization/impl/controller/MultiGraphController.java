package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.annotation.*;
import com.majortom.algorithms.core.metadata.GraphDirection;
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
    implements StructureSnapshotSupport<MultiGraphSnapshot<Object>> {
  private MultiGraph<Object> graph = new MultiGraph<>(GraphDirection.DIRECTED);
  private Object lastBatch;
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
    appendLog("MultiGraph algorithm registration will be added separately.");
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
  private record Railway(@GraphFrom City from, @GraphTo City to,
      @ValueField(value = ValueDisplay.SUMMARY, label = "线路") String line,
      @GraphWeight double kilometers) {}
  private record Road(@GraphFrom City from, @GraphTo City to,
      @ValueField(value = ValueDisplay.SUMMARY, label = "公路") String name,
      @GraphBidirectional boolean twoWay, @GraphWeight double kilometers) {}
  private record TransportNetwork(@GraphNodes List<City> cities,
      @GraphEdges List<Railway> railways, @GraphEdges List<Road> roads) {}
}
