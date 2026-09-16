package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.CircleGeometry;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.visualizer.tree.TreeElkLayout;
import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.fx.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.fx.RenderCommitContext;
import com.majortom.algorithms.visualization.render.layout.DetachedMetrics;
import com.majortom.algorithms.visualization.render.runtime.DefaultRenderFramework;
import com.majortom.algorithms.visualization.render.runtime.RenderRuntime;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.render.viewport.CameraState;
import com.majortom.algorithms.visualization.render.viewport.ViewportSnapshot;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import javafx.geometry.Point2D;

/**
 * General/binary/AVL tree renderer using measured JavaFX nodes, transient ELK layout and GestureFX
 * viewport.
 */
public final class TreeVisualizer
    extends BaseVisualizer<TreeViewState> implements FxSurfaceAdapter<TreeViewState> {
  private static final double MIN_RADIUS = 24.0d;
  private static final double LABEL_PADDING = 18.0d;

  private static final RenderSessionId SESSION_ID = RenderSessionId.of("TREE");
  private final VisualizationSurface surface = new VisualizationSurface();
  private final DefaultRenderFramework renderFramework = RenderRuntime.shared();
  private final Map<Long, NodeView> nodeViews = new LinkedHashMap<>();
  private final Map<EdgeKey, EdgeView> edgeViews = new LinkedHashMap<>();

  private volatile TreeViewState lastSubmittedState;
  private Long selectedNodeId;
  private Long pendingSelectedNodeId;
  private LongConsumer selectionListener = ignored -> {};

  public TreeVisualizer() {
    getChildren().setAll(surface);
    surface.prefWidthProperty().bind(widthProperty());
    surface.prefHeightProperty().bind(heightProperty());
    surface.setFrameworkManagedCamera(true);
    renderFramework.registerSurface(SESSION_ID, this);
  }
  @Override
  protected synchronized void submitFrameworkRender(TreeViewState state) {
    TreeViewState previous = lastSubmittedState;
    boolean initial = previous == null;
    boolean structural = initial || requiresStructuralLayout(previous, state);
    lastSubmittedState = state;
    if (structural) {
      renderFramework.submit(new StructuralRenderIntent<>(SESSION_ID, state,
          initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE, initial));
    } else {
      renderFramework.submit(new PresentationRenderIntent<>(SESSION_ID, state));
    }
  }

  @Override
  public LayoutRequest captureLayout(TreeViewState state, RenderCaptureContext context) {
    List<Long> order = orderedNodeIds(state);
    List<LayoutElement> nodes = new ArrayList<>(order.size());
    for (Long id : order) {
      TreeViewState.Node node = state.nodes().get(id);
      if (node == null)
        continue;
      double diameter = Math.max(MIN_RADIUS * 2.0d,
          DetachedMetrics.boxWidth(
              node.value().text(), context.contentStyle(), MIN_RADIUS * 2.0d, LABEL_PADDING));
      nodes.add(
          new LayoutElement(TreeElkLayout.nodeId(id), quantize(diameter), quantize(diameter)));
    }

    List<LayoutLink> links = new ArrayList<>();
    for (Long id : order) {
      TreeViewState.Node node = state.nodes().get(id);
      if (node == null)
        continue;
      if (state.kind() == TreeViewState.Kind.GENERAL) {
        for (int index = 0; index < node.childIds().size(); index++) {
          Long targetId = node.childIds().get(index);
          if (targetId == null || !state.nodes().containsKey(targetId))
            continue;
          EdgeKey key = new EdgeKey(node.id(), targetId, Relation.CHILD, index);
          links.add(new LayoutLink(routeId(key), TreeElkLayout.nodeId(node.id()),
              TreeElkLayout.nodeId(targetId), "CHILD", index));
        }
      } else {
        addLayoutLink(links, state, node.id(), node.leftId(), Relation.LEFT, 0);
        addLayoutLink(links, state, node.id(), node.rightId(), Relation.RIGHT, 1);
      }
    }

    return new LayoutRequest(context.requestId(), context.sessionId(), context.modelRevision(),
        context.geometryRevision(), TreeElkLayout.ID, nodes, links,
        Map.of("kind", state.kind().name()));
  }

  private void addLayoutLink(List<LayoutLink> links, TreeViewState state, long sourceId,
      Long targetId, Relation relation, int index) {
    if (targetId == null || !state.nodes().containsKey(targetId))
      return;
    EdgeKey key = new EdgeKey(sourceId, targetId, relation, index);
    links.add(new LayoutLink(routeId(key), TreeElkLayout.nodeId(sourceId),
        TreeElkLayout.nodeId(targetId), relation.name(), index));
  }

  @Override
  public CompletionStage<Void> commitLayout(
      TreeViewState state, LayoutPatch patch, RenderCommitContext context) {
    reconcileNodes(state);
    reconcileEdges(state);
    applyPendingSelection(state);
    applyPresentation(state);

    for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
      ElementGeometry bounds = patch.elements().get(TreeElkLayout.nodeId(entry.getKey()));
      if (bounds == null)
        continue;
      NodeView view = entry.getValue();
      view.setGeometry(new CircleGeometry(Math.max(MIN_RADIUS, bounds.width() / 2.0d)));
      view.setCenter(bounds.x() + bounds.width() / 2.0d, bounds.y() + bounds.height() / 2.0d);
      view.setOpacity(1.0d);
      view.setScaleX(1.0d);
      view.setScaleY(1.0d);
    }
    applyRoutes(patch);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletionStage<Void> commitPresentation(
      TreeViewState state, RenderCommitContext context) {
    reconcileNodes(state);
    reconcileEdges(state);
    applyPendingSelection(state);
    applyPresentation(state);
    return CompletableFuture.completedFuture(null);
  }

  private void reconcileNodes(TreeViewState state) {
    List<Long> removed =
        nodeViews.keySet().stream().filter(id -> !state.nodes().containsKey(id)).toList();
    for (Long nodeId : removed) {
      if (java.util.Objects.equals(selectedNodeId, nodeId))
        selectedNodeId = null;
      NodeView view = nodeViews.remove(nodeId);
      if (view != null)
        surface.nodeLayer().getChildren().remove(view);
    }
    for (TreeViewState.Node node : state.nodes().values()) {
      if (nodeViews.containsKey(node.id()))
        continue;
      NodeView view = new NodeView(new CircleGeometry(MIN_RADIUS), node.value().text());
      long nodeId = node.id();
      view.setOnMouseClicked(event -> {
        selectNode(nodeId);
        event.consume();
      });
      nodeViews.put(node.id(), view);
      surface.nodeLayer().getChildren().add(view);
    }
  }

  private void reconcileEdges(TreeViewState state) {
    Map<EdgeKey, EdgeSpec> expected = new LinkedHashMap<>();
    for (TreeViewState.Node node : state.nodes().values()) {
      if (state.kind() == TreeViewState.Kind.GENERAL) {
        for (int index = 0; index < node.childIds().size(); index++) {
          addExpectedEdge(
              expected, state, node.id(), node.childIds().get(index), Relation.CHILD, index);
        }
      } else {
        addExpectedEdge(expected, state, node.id(), node.leftId(), Relation.LEFT, 0);
        addExpectedEdge(expected, state, node.id(), node.rightId(), Relation.RIGHT, 1);
      }
    }

    List<EdgeKey> removed =
        edgeViews.keySet().stream().filter(key -> !expected.containsKey(key)).toList();
    for (EdgeKey key : removed) {
      EdgeView edge = edgeViews.remove(key);
      if (edge != null) {
        edge.dispose();
        surface.edgeLayer().getChildren().remove(edge);
      }
    }

    for (Map.Entry<EdgeKey, EdgeSpec> entry : expected.entrySet()) {
      if (edgeViews.containsKey(entry.getKey()))
        continue;
      EdgeSpec spec = entry.getValue();
      NodeView source = nodeViews.get(spec.sourceId());
      NodeView target = nodeViews.get(spec.targetId());
      if (source == null || target == null)
        continue;
      EdgeView edge = new EdgeView(source, target, false);
      edge.setCurved(source == target);
      edge.getStyleClass().add(entry.getKey().relation().styleClass());
      edgeViews.put(entry.getKey(), edge);
      surface.edgeLayer().getChildren().add(edge);
    }
  }

  private void applyPresentation(TreeViewState state) {
    for (TreeViewState.Node node : state.nodes().values()) {
      NodeView view = nodeViews.get(node.id());
      if (view == null)
        continue;
      view.setText(node.value().text());
      view.setCurrent(state.currentNodeIds().contains(node.id()));
      view.setHighlighted(state.observedNodeIds().contains(node.id()));
      view.setVisited(state.visitedNodeIds().contains(node.id()));
    }
    syncSelectionState();
  }

  private void applyPendingSelection(TreeViewState state) {
    if (pendingSelectedNodeId == null)
      return;
    if (state.nodes().containsKey(pendingSelectedNodeId))
      selectedNodeId = pendingSelectedNodeId;
    pendingSelectedNodeId = null;
  }

  private void applyRoutes(LayoutPatch patch) {
    Map<String, EdgeGeometry> routes = new LinkedHashMap<>();
    for (EdgeGeometry route : patch.edges()) routes.put(route.id(), route);
    for (Map.Entry<EdgeKey, EdgeView> entry : edgeViews.entrySet()) {
      EdgeGeometry route = routes.get(routeId(entry.getKey()));
      if (route == null || route.points().size() < 2) {
        entry.getValue().clearRoute();
      } else {
        entry.getValue().setRoute(
            route.points().stream().map(point -> new Point2D(point.x(), point.y())).toList());
      }
    }
  }

  private void addExpectedEdge(Map<EdgeKey, EdgeSpec> expected, TreeViewState state, long sourceId,
      Long targetId, Relation relation, int index) {
    if (targetId == null || !state.nodes().containsKey(targetId)) {
      return;
    }
    EdgeKey key = new EdgeKey(sourceId, targetId, relation, index);
    expected.put(key, new EdgeSpec(sourceId, targetId));
  }

  private List<Long> orderedNodeIds(TreeViewState state) {
    List<Long> order = new ArrayList<>();
    Set<Long> visited = new HashSet<>();
    if (state.rootId() != null) {
      visit(state.rootId(), state, visited, order);
    }
    state.nodes()
        .keySet()
        .stream()
        .sorted(Comparator.naturalOrder())
        .forEach(id -> visit(id, state, visited, order));
    return order;
  }

  private void visit(long id, TreeViewState state, Set<Long> visited, List<Long> order) {
    if (!state.nodes().containsKey(id) || !visited.add(id)) {
      return;
    }
    order.add(id);
    TreeViewState.Node node = state.nodes().get(id);
    if (state.kind() == TreeViewState.Kind.GENERAL) {
      for (Long childId : node.childIds()) {
        if (childId != null) {
          visit(childId, state, visited, order);
        }
      }
    } else {
      if (node.leftId() != null) {
        visit(node.leftId(), state, visited, order);
      }
      if (node.rightId() != null) {
        visit(node.rightId(), state, visited, order);
      }
    }
  }

  public void setSelectionListener(LongConsumer listener) {
    if (listener == null) {
      selectionListener = ignored -> {};
    } else {
      selectionListener = listener;
    }
  }

  public void clearSelection() {
    selectedNodeId = null;
    pendingSelectedNodeId = null;
    submitCurrentPresentation();
  }

  public Long selectedNodeId() {
    return selectedNodeId;
  }

  public void selectNode(long nodeId) {
    if (!showSelection(nodeId)) {
      return;
    }
    selectionListener.accept(nodeId);
  }

  public boolean showSelection(long nodeId) {
    TreeViewState state = currentState();
    if (state == null || !state.nodes().containsKey(nodeId)) {
      return false;
    }
    selectedNodeId = nodeId;
    pendingSelectedNodeId = nodeViews.containsKey(nodeId) ? null : nodeId;
    submitCurrentPresentation();
    return true;
  }

  private void submitCurrentPresentation() {
    TreeViewState state = currentState();
    if (state != null && isModuleAttached() && !isDisposed()) {
      renderFramework.submit(new PresentationRenderIntent<>(SESSION_ID, state));
    }
  }

  private void syncSelectionState() {
    for (Map.Entry<Long, NodeView> entry : nodeViews.entrySet()) {
      entry.getValue().setSelected(selectedNodeId != null && selectedNodeId.equals(entry.getKey()));
    }
  }

  @Override
  public void applyPrimaryContentBounds(BoundsSnapshot bounds) {
    surface.setPrimaryContentBounds(bounds);
  }
  @Override
  public ViewportSnapshot viewportSnapshot() {
    return surface.viewportSnapshot();
  }
  @Override
  public CameraState cameraState() {
    return surface.cameraState();
  }
  @Override
  public void applyCameraState(CameraState cameraState) {
    surface.applyCameraState(cameraState);
  }
  @Override
  public boolean userControlledCamera() {
    return surface.isUserViewportChanged();
  }
  @Override
  public void prepareInitialFrame() {
    surface.markViewportPristine();
  }
  @Override
  public void revealFrame() {
    surface.setWorldVisible(true);
  }
  @Override
  public void setViewportListener(Consumer<ViewportSnapshot> listener) {
    surface.setViewportListener(listener);
  }

  @Override
  public void onModuleAttached(String moduleId) {
    renderFramework.activateSession(SESSION_ID);
    super.onModuleAttached(moduleId);
  }

  @Override
  public void onModuleDetached(String moduleId) {
    renderFramework.deactivateSession(SESSION_ID);
    super.onModuleDetached(moduleId);
  }

  @Override
  public void setViewportObstructionInsets(javafx.geometry.Insets insets) {
    surface.setObstructionInsets(insets);
  }

  @Override
  public void onVisualizationReset() {
    nodeViews.clear();
    edgeViews.values().forEach(EdgeView::dispose);
    edgeViews.clear();
    surface.nodeLayer().getChildren().clear();
    surface.edgeLayer().getChildren().clear();
    surface.decorationLayer().getChildren().clear();
    lastSubmittedState = null;
    selectedNodeId = null;
    pendingSelectedNodeId = null;
    surface.reset();
    surface.markViewportPristine();
  }

  @Override
  public void dispose() {
    edgeViews.values().forEach(EdgeView::dispose);
    renderFramework.unregisterSurface(SESSION_ID, this);
    surface.prefWidthProperty().unbind();
    surface.prefHeightProperty().unbind();
    super.dispose();
  }

  private static boolean requiresStructuralLayout(TreeViewState previous, TreeViewState current) {
    return previous == null || previous.kind() != current.kind()
        || !java.util.Objects.equals(previous.rootId(), current.rootId())
        || !previous.nodes().equals(current.nodes());
  }

  private static String routeId(EdgeKey key) {
    return "tree:" + key.relation().name().toLowerCase() + ":" + key.index() + ":" + key.sourceId()
        + ":" + key.targetId();
  }

  private static double quantize(double value) {
    return Math.rint(value * 100.0d) / 100.0d;
  }

  private enum Relation {
    CHILD("tree-child-edge"),
    LEFT("tree-left-edge"),
    RIGHT("tree-right-edge");

    private final String styleClass;

    Relation(String styleClass) {
      this.styleClass = styleClass;
    }

    private String styleClass() {
      return styleClass;
    }
  }

  private record EdgeKey(long sourceId, long targetId, Relation relation, int index) {}
  private record EdgeSpec(long sourceId, long targetId) {}
}
