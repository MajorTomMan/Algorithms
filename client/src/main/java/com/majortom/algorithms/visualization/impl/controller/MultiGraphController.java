package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.annotation.*;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.runtime.graph.MultiGraphEventReducer;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.ValueMetadata;
import com.majortom.algorithms.core.snapshot.MultiGraphSnapshot;
import com.majortom.algorithms.core.event.structure.MultiGraphStructureEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.structure.graph.*;
import com.majortom.algorithms.visualization.impl.visualizer.GraphVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.presenter.GraphPresenter;
import com.majortom.algorithms.visualization.render.runtime.RenderContext;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import com.majortom.algorithms.visualization.render.fx.FxDispatch;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
  @FXML private TextField batchClassField;
  @FXML private TextArea batchJsonField;
  @FXML private Button importJsonButton;
  @FXML private Button chooseJsonButton;
  @FXML private Label importStatus;
  private long importVersion;
  private boolean importInProgress;
  private Path selectedJsonFile;
  private record PreparedBatch(Object data, MultiGraph<Object> graph) {}

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
    importJsonButton.setOnAction(event -> loadJsonFromUi());
    chooseJsonButton.setOnAction(event -> chooseJsonFile());
    batchJsonField.textProperty().addListener((observable, previous, next) -> {
      // Typing text after picking a file means the textarea is now the data source.
      if (selectedJsonFile != null && next != null && !next.isBlank()) {
        selectedJsonFile = null;
      }
    });
    algorithmSelector.setItems(FXCollections.observableArrayList(algorithmIds));
    localizeChoiceCells(algorithmSelector, AlgorithmCatalog::name);
    algorithmSelector.getSelectionModel().selectFirst();
    algorithmSelector.getSelectionModel().selectedItemProperty().addListener(
        (observable, before, after) -> algorithmSelectionListener.accept(after));
  }
  private GraphVisualizer graphVisualizer() { return (GraphVisualizer) visualizer; }

  /** JSON decoding and bulk indexing run off the FX thread. Publish only the final graph. */
  private void loadJsonFromUi() {
    if (importInProgress) return;
    String className = batchClassField.getText();
    Path file = selectedJsonFile;
    String json = file == null ? batchJsonField.getText() : null;
    GraphDirection direction = graph.direction();
    long version = ++importVersion;
    importInProgress = true;
    setImportControlsDisabled(true);
    importStatus.setText(I18N.text("message.multi_graph.loading"));
    CompletableFuture.supplyAsync(() -> {
      String dataJson = json;
      if (file != null) {
        try { dataJson = Files.readString(file, StandardCharsets.UTF_8); }
        catch (IOException exception) { throw new UncheckedIOException(exception); }
      }
      Object data = MultiGraphJsonLoader.read(className, dataJson);
      return new PreparedBatch(data, MultiGraph.from(direction, data));
    }).whenComplete((prepared, error) -> FxDispatch.defer(() -> {
      if (importVersion != version) return;
      importInProgress = false;
      setImportControlsDisabled(false);
      if (error != null) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null
            ? error.getCause() : error;
        importStatus.setText(I18N.text("message.multi_graph.load_failed") + ": " + cause.getMessage());
        return;
      }
      try {
        boolean applied = executeStructureOperation("json-import", () -> {
          graph = prepared.graph();
          ExecutionEvents.structure(new MultiGraphStructureEvent.Reset(graph.snapshot()));
          return null;
        });
        if (!applied) {
          importStatus.setText(I18N.text("message.multi_graph.load_failed"));
          return;
        }
        lastBatch = prepared.data();
        clearSelection();
        renderGraph();
        refreshStatsDisplay();
        importStatus.setText(I18N.text("message.multi_graph.load_success",
            graph.vertexCount(), graph.edgeCount()));
      } catch (IllegalStateException disposed) {
        importStatus.setText(I18N.text("message.multi_graph.load_failed"));
      }
    }));
  }

  private void setImportControlsDisabled(boolean disabled) {
    importJsonButton.setDisable(disabled);
    chooseJsonButton.setDisable(disabled);
    directionSelector.setDisable(disabled);
    sampleButton.setDisable(disabled);
    batchClassField.setDisable(disabled);
    batchJsonField.setDisable(disabled);
  }

  private void chooseJsonFile() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(I18N.text("action.multi_graph.select_file"));
    chooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("JSON", "*.json"));
    var file = chooser.showOpenDialog(
        chooseJsonButton.getScene() == null ? null : chooseJsonButton.getScene().getWindow());
    if (file == null) return;
    // Reading a large JSON document into TextArea freezes FX and duplicates memory.
    // Preserve only the file path; the async import task will read the contents.
    batchJsonField.clear();
    selectedJsonFile = file.toPath();
    importStatus.setText(file.getName());
  }

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
    if (importInProgress) return;
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
    int degree = graph.outgoingEdges(node).size();
    if (graph.direction().isDirected()) degree += graph.incomingEdges(node).size();
    // Directed self-loops count once rather than twice in the inspector.
    if (graph.direction().isDirected()) {
      for (MultiEdge<Object> edge : graph.outgoingEdges(node))
        if (edge.to().id() == id) degree--;
    }
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
    sampleButton.textProperty().bind(I18N.createStringBinding("action.multi_graph.sample"));
    importJsonButton.textProperty().bind(I18N.createStringBinding("action.multi_graph.import"));
    chooseJsonButton.textProperty().bind(I18N.createStringBinding("action.multi_graph.select_file"));
    batchClassField.promptTextProperty().bind(
        I18N.createStringBinding("prompt.multi_graph.batch_class"));
    batchJsonField.promptTextProperty().bind(
        I18N.createStringBinding("prompt.multi_graph.batch_json"));
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
    return ValueMetadata.describe(value.getClass()).immutable();
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
    ++importVersion;
    importInProgress = false;
    if (importJsonButton != null) setImportControlsDisabled(false);
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
