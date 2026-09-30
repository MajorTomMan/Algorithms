package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.animation.api.AnimationControl;
import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.runtime.StructureAnimationRuntime;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.RectangleGeometry;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.visualizer.semantic.HashTableStructureVisualization;
import com.majortom.algorithms.visualization.impl.visualizer.hash.HashVisualIds;
import com.majortom.algorithms.visualization.impl.visualizer.hash.animation.HashTableAnimationPlanner;
import com.majortom.algorithms.visualization.impl.visualizer.hash.animation.HashTableAnimationSceneAdapter;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.runtime.hash.HashTableViewState;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.LongConsumer;
import javafx.geometry.Point2D;

/** Bucket/entry hash-table renderer; collision strategy is represented only by factual placement. */
public final class HashTableVisualizer extends BaseVisualizer<HashTableViewState> {
  private static final RenderSessionId SESSION_ID = RenderSessionId.of("HASH");
  private static final StructureVisualization<HashTableViewState> STRUCTURE_VISUALIZATION =
      new HashTableStructureVisualization();

  private final VisualizationSurface surface = new VisualizationSurface();
  private final Map<String, NodeView> nodes = new LinkedHashMap<>();
  private final Map<String, EdgeView> edges = new LinkedHashMap<>();
  private final StructureAnimationRuntime<HashTableViewState> animationRuntime =
      new StructureAnimationRuntime<>(new HashTableAnimationPlanner());
  private final HashTableAnimationSceneAdapter animationScene =
      new HashTableAnimationSceneAdapter(surface, nodes, edges);
  private Long selectedEntryId;
  private LongConsumer selectionListener = ignored -> {};

  public HashTableVisualizer() {
    installSurface(surface, new javafx.geometry.Insets(28.0d, 16.0d, 62.0d, 16.0d));
  }

  @Override
  public RenderSessionId sessionId() {
    return SESSION_ID;
  }

  @Override
  public CompletionStage<Void> commitLayout(
      HashTableViewState state, LayoutPatch patch, RenderCommitContext context) {
    boolean animate = context.modelChange() && !context.initialFrame();
    AnimationPlan plan = animationRuntime.beginTransition(state, patch, animate);
    animationScene.prepare(plan, patch);

    reconcileNodes(state);
    reconcileEdges(state);
    applyPresentation(state);
    applyLayout(patch);
    applyRoutes(patch);

    animationRuntime.play(plan, animationScene, context.presentationProgress()::publish);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletionStage<Void> commitPresentation(
      HashTableViewState state, RenderCommitContext context) {
    reconcileNodes(state);
    reconcileEdges(state);
    applyPresentation(state);
    return CompletableFuture.completedFuture(null);
  }

  public void setSelectionListener(LongConsumer listener) {
    selectionListener = listener == null ? ignored -> {} : listener;
  }

  public void clearSelection() {
    selectedEntryId = null;
  }

  public boolean showSelection(long entryId) {
    if (entryId <= 0L || nodes.get(HashVisualIds.entry(entryId)) == null) {
      return false;
    }
    selectedEntryId = entryId;
    return true;
  }

  public void selectEntry(long entryId) {
    if (showSelection(entryId)) {
      selectionListener.accept(entryId);
    }
  }

  private void reconcileNodes(HashTableViewState state) {
    Set<String> expected = new LinkedHashSet<>();
    for (HashTableViewState.Bucket bucket : state.buckets()) {
      String bucketId = HashVisualIds.bucket(bucket.index());
      expected.add(bucketId);
      nodes.computeIfAbsent(bucketId, ignored -> {
        NodeView view = new NodeView(new RectangleGeometry(1.0d, 1.0d), "#" + bucket.index());
        view.getStyleClass().add("hash-bucket");
        surface.nodeLayer().getChildren().add(view);
        return view;
      }).setText("#" + bucket.index());

      for (HashTableViewState.Entry entry : bucket.entries()) {
        String entryId = HashVisualIds.entry(entry.id());
        expected.add(entryId);
        NodeView view = nodes.computeIfAbsent(entryId, ignored -> {
          NodeView created = new NodeView(new RectangleGeometry(1.0d, 1.0d), entryText(entry));
          created.getStyleClass().add("hash-entry");
          created.setOnMouseClicked(event -> {
            selectEntry(entry.id());
            event.consume();
          });
          surface.nodeLayer().getChildren().add(created);
          return created;
        });
        view.setText(entryText(entry));
      }
    }

    List<String> stale = nodes.keySet().stream()
        .filter(id -> !expected.contains(id))
        .toList();
    for (String id : stale) {
      NodeView removed = nodes.remove(id);
      if (removed != null) {
        surface.nodeLayer().getChildren().remove(removed);
      }
    }
  }

  private void reconcileEdges(HashTableViewState state) {
    Map<String, Link> expected = links(state);
    List<String> stale = edges.keySet().stream()
        .filter(id -> !expected.containsKey(id))
        .toList();
    for (String id : stale) {
      EdgeView removed = edges.remove(id);
      if (removed != null) {
        removed.dispose();
        surface.edgeLayer().getChildren().remove(removed);
      }
    }

    for (Map.Entry<String, Link> entry : expected.entrySet()) {
      if (edges.containsKey(entry.getKey())) {
        continue;
      }
      NodeView source = nodes.get(entry.getValue().sourceId());
      NodeView target = nodes.get(entry.getValue().targetId());
      if (source == null || target == null) {
        continue;
      }
      EdgeView edge = new EdgeView(source, target, true);
      edge.getStyleClass().add("hash-link");
      edges.put(entry.getKey(), edge);
      surface.edgeLayer().getChildren().add(edge);
    }
  }

  private void applyPresentation(HashTableViewState state) {
    long mutatedId = state.mutation().entryId();
    for (HashTableViewState.Bucket bucket : state.buckets()) {
      NodeView bucketView = nodes.get(HashVisualIds.bucket(bucket.index()));
      if (bucketView != null) {
        bucketView.setHighlighted(
            state.mutation().bucketIndex() == bucket.index()
                && state.mutation().type() != HashTableViewState.Type.NONE);
        bucketView.setSelected(false);
      }
      for (HashTableViewState.Entry entry : bucket.entries()) {
        NodeView entryView = nodes.get(HashVisualIds.entry(entry.id()));
        if (entryView == null) continue;
        entryView.setHighlighted(entry.id() == mutatedId);
        entryView.setSelected(selectedEntryId != null && selectedEntryId == entry.id());
      }
    }
  }

  private void applyLayout(LayoutPatch patch) {
    for (Map.Entry<String, NodeView> entry : nodes.entrySet()) {
      ElementGeometry geometry = patch.elements().get(entry.getKey());
      if (geometry == null) continue;
      NodeView view = entry.getValue();
      view.setGeometry(new RectangleGeometry(geometry.width(), geometry.height()));
      view.setCenter(
          geometry.x() + geometry.width() / 2.0d,
          geometry.y() + geometry.height() / 2.0d);
      view.setOpacity(1.0d);
    }
  }

  private void applyRoutes(LayoutPatch patch) {
    Map<String, EdgeGeometry> routes = new LinkedHashMap<>();
    for (EdgeGeometry route : patch.edges()) {
      routes.put(route.id(), route);
    }
    for (Map.Entry<String, EdgeView> entry : edges.entrySet()) {
      EdgeGeometry route = routes.get(entry.getKey());
      if (route == null) {
        entry.getValue().clearRoute();
        continue;
      }
      entry.getValue().setRoute(route.points().stream()
          .map(point -> new Point2D(point.x(), point.y()))
          .toList());
    }
  }

  private static Map<String, Link> links(HashTableViewState state) {
    Map<String, Link> result = new LinkedHashMap<>();
    for (HashTableViewState.Bucket bucket : state.buckets()) {
      String previous = HashVisualIds.bucket(bucket.index());
      for (HashTableViewState.Entry entry : bucket.entries()) {
        String target = HashVisualIds.entry(entry.id());
        String linkId = HashVisualIds.link(previous, target);
        result.put(linkId, new Link(previous, target));
        previous = target;
      }
    }
    return result;
  }

  private static String entryText(HashTableViewState.Entry entry) {
    return entry.key().text() + " → " + entry.value().text();
  }

  @Override
  protected AnimationControl animationControl() {
    return animationRuntime;
  }

  @Override
  public StructureVisualization<HashTableViewState> structureVisualization() {
    return STRUCTURE_VISUALIZATION;
  }

  @Override
  public FxSurfaceAdapter fxSurfaceAdapter() {
    return surface;
  }

  @Override
  public void onVisualizationReset() {
    super.onVisualizationReset();
    edges.values().forEach(EdgeView::dispose);
    edges.clear();
    nodes.clear();
    selectedEntryId = null;
    surface.nodeLayer().getChildren().clear();
    surface.edgeLayer().getChildren().clear();
    surface.decorationLayer().getChildren().clear();
    surface.reset();
    surface.markViewportPristine();
  }

  @Override
  public void dispose() {
    if (isDisposed()) return;
    super.dispose();
    edges.values().forEach(EdgeView::dispose);
  }

  private record Link(String sourceId, String targetId) {}
}
