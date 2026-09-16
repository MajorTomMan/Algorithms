package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.RectangleGeometry;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.fx.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.fx.RenderCommitContext;
import com.majortom.algorithms.visualization.render.layout.DetachedMetrics;
import com.majortom.algorithms.visualization.render.layout.LinearLayoutEngine;
import com.majortom.algorithms.visualization.render.runtime.DefaultRenderFramework;
import com.majortom.algorithms.visualization.render.runtime.RenderRuntime;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.render.viewport.CameraState;
import com.majortom.algorithms.visualization.render.viewport.ViewportSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javafx.geometry.Point2D;
import javafx.scene.text.Text;

/** Logical FIFO visualization: a horizontal flow lane from FRONT to REAR. */
public final class QueueVisualizer extends BaseVisualizer<LinearStructureViewState>
    implements FxSurfaceAdapter<LinearStructureViewState> {
  private static final RenderSessionId SESSION_ID = RenderSessionId.of("QUEUE");
  private static final double ITEM_MIN_WIDTH = 90.0d;
  private static final double ITEM_HEIGHT = 50.0d;
  private static final double ITEM_HORIZONTAL_PADDING = 28.0d;

  private final VisualizationSurface surface = new VisualizationSurface();
  private final DefaultRenderFramework renderFramework = RenderRuntime.shared();
  private final Map<Integer, NodeView> items = new LinkedHashMap<>();
  private final Text frontLabel = new Text();
  private final Text rearLabel = new Text();
  private final Text dequeueLabel = new Text();
  private final Text enqueueLabel = new Text();
  private final javafx.beans.InvalidationListener localeListener =
      observable -> submitCurrentPresentation();

  private volatile LinearStructureViewState lastSubmittedState;
  private Map<String, ElementGeometry> lastGeometry = Map.of();
  private int selectedIndex = -1;
  private int pendingSelectedIndex = -1;
  private IntConsumer selectionListener = ignored -> {};

  public QueueVisualizer() {
    getChildren().setAll(surface);
    surface.prefWidthProperty().bind(widthProperty());
    surface.prefHeightProperty().bind(heightProperty());
    surface.setSafeInsets(new javafx.geometry.Insets(34.0d, 16.0d, 82.0d, 16.0d));
    surface.setFrameworkManagedCamera(true);
    frontLabel.getStyleClass().addAll("linear-role-label", "queue-front-label");
    rearLabel.getStyleClass().addAll("linear-role-label", "queue-rear-label");
    dequeueLabel.getStyleClass().addAll("linear-flow-label", "queue-dequeue-label");
    enqueueLabel.getStyleClass().addAll("linear-flow-label", "queue-enqueue-label");
    dequeueLabel.textProperty().bind(I18N.createStringBinding("label.visual.queue.dequeue"));
    enqueueLabel.textProperty().bind(I18N.createStringBinding("label.visual.queue.enqueue"));
    I18N.localeProperty().addListener(localeListener);
    surface.decorationLayer().getChildren().addAll(
        frontLabel, rearLabel, dequeueLabel, enqueueLabel);
    renderFramework.registerSurface(SESSION_ID, this);
  }

  @Override
  protected synchronized void submitFrameworkRender(LinearStructureViewState state) {
    LinearStructureViewState previous = lastSubmittedState;
    boolean initial = previous == null;
    boolean structural = initial || !previous.values().equals(state.values());
    lastSubmittedState = state;
    if (structural) {
      renderFramework.submit(new StructuralRenderIntent<>(SESSION_ID, state,
          initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE, initial));
    } else {
      renderFramework.submit(new PresentationRenderIntent<>(SESSION_ID, state));
    }
  }

  @Override
  public LayoutRequest captureLayout(LinearStructureViewState state, RenderCaptureContext context) {
    List<LayoutElement> elements = new ArrayList<>(state.values().size());
    for (int index = 0; index < state.values().size(); index++) {
      String text = state.values().get(index).text();
      double width = DetachedMetrics.boxWidth(
          text, context.contentStyle(), ITEM_MIN_WIDTH, ITEM_HORIZONTAL_PADDING);
      elements.add(new LayoutElement(id(index), width, ITEM_HEIGHT));
    }
    return new LayoutRequest(context.requestId(), context.sessionId(), context.modelRevision(),
        context.geometryRevision(), LinearLayoutEngine.ID, elements,
        Map.of("structure", "queue", "direction", "RIGHT", "padding", "30", "spacing", "0"));
  }

  @Override
  public CompletionStage<Void> commitLayout(
      LinearStructureViewState state, LayoutPatch patch, RenderCommitContext context) {
    if (context.modelChange())
      applyModelIdentity(state.mutation());
    reconcileItems(state);
    applyPendingSelection(state.values().size());
    applyPresentation(state);
    for (Map.Entry<Integer, NodeView> entry : items.entrySet()) {
      ElementGeometry bounds = patch.elements().get(id(entry.getKey()));
      if (bounds == null)
        continue;
      NodeView item = entry.getValue();
      item.setGeometry(new RectangleGeometry(bounds.width(), bounds.height()));
      Point2D target = center(bounds);
      item.setCenter(target.getX(), target.getY());
      item.setOpacity(1.0d);
    }
    lastGeometry = Map.copyOf(patch.elements());
    positionLabels(lastGeometry);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletionStage<Void> commitPresentation(
      LinearStructureViewState state, RenderCommitContext context) {
    applyPendingSelection(state.values().size());
    reconcileItems(state);
    applyPresentation(state);
    positionLabels(lastGeometry);
    return CompletableFuture.completedFuture(null);
  }

  private void applyModelIdentity(LinearStructureViewState.Mutation mutation) {
    if (mutation == null || mutation.type() != LinearStructureViewState.Type.DEQUEUE
        || items.isEmpty()) {
      return;
    }
    NodeView removed = items.remove(0);
    if (removed != null)
      surface.nodeLayer().getChildren().remove(removed);
    Map<Integer, NodeView> shifted = new LinkedHashMap<>();
    items.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> shifted.put(entry.getKey() - 1, entry.getValue()));
    items.clear();
    items.putAll(shifted);
  }

  private void reconcileItems(LinearStructureViewState state) {
    int size = state.values().size();
    List<Integer> stale = items.keySet().stream().filter(index -> index >= size).toList();
    for (Integer index : stale) {
      NodeView removed = items.remove(index);
      if (removed != null)
        surface.nodeLayer().getChildren().remove(removed);
    }
    for (int index = 0; index < size; index++) {
      NodeView item = items.get(index);
      if (item == null) {
        item = createItem(index, state.values().get(index).text());
        items.put(index, item);
        surface.nodeLayer().getChildren().add(item);
      } else {
        item.setText(state.values().get(index).text());
        installSelectionHandler(item, index);
      }
    }
  }

  private NodeView createItem(int index, String text) {
    NodeView item = new NodeView(new RectangleGeometry(ITEM_MIN_WIDTH, ITEM_HEIGHT), text);
    item.getStyleClass().add("queue-item");
    installSelectionHandler(item, index);
    return item;
  }

  private void installSelectionHandler(NodeView item, int index) {
    item.setOnMouseClicked(event -> {
      selectedIndex = index;
      submitCurrentPresentation();
      selectionListener.accept(index);
      event.consume();
    });
  }

  private void applyPresentation(LinearStructureViewState state) {
    for (int index = 0; index < state.values().size(); index++) {
      NodeView item = items.get(index);
      if (item == null)
        continue;
      item.setText(state.values().get(index).text());
      item.setHighlighted(false);
      item.setSelected(index == selectedIndex);
    }
  }

  private void positionLabels(Map<String, ElementGeometry> geometry) {
    ElementGeometry front = geometry.get(id(0));
    ElementGeometry rear = geometry.get(id(items.size() - 1));
    if (front == null || rear == null) {
      positionEmptyLabels();
      return;
    }
    double top = Math.min(front.y(), rear.y());
    double bottom = Math.max(front.y() + front.height(), rear.y() + rear.height());
    if (items.size() == 1) {
      frontLabel.setText(I18N.text("label.visual.queue.front_rear"));
      frontLabel.setVisible(true);
      rearLabel.setVisible(false);
      relocateCentered(frontLabel, front.x() + front.width() / 2.0d, Math.max(2.0d, top - 28.0d));
      dequeueLabel.relocate(front.x(), bottom + 14.0d);
      enqueueLabel.relocate(
          Math.max(front.x(), front.x() + front.width() - textWidth(enqueueLabel)), bottom + 32.0d);
      return;
    }
    frontLabel.setText(I18N.text("label.visual.queue.front"));
    rearLabel.setText(I18N.text("label.visual.queue.rear"));
    frontLabel.setVisible(true);
    rearLabel.setVisible(true);
    relocateCentered(frontLabel, front.x() + front.width() / 2.0d, Math.max(2.0d, top - 28.0d));
    relocateCentered(rearLabel, rear.x() + rear.width() / 2.0d, Math.max(2.0d, top - 28.0d));
    dequeueLabel.relocate(front.x(), bottom + 14.0d);
    enqueueLabel.relocate(
        Math.max(rear.x(), rear.x() + rear.width() - textWidth(enqueueLabel)), bottom + 14.0d);
  }

  private void positionEmptyLabels() {
    frontLabel.setText(I18N.text("label.visual.queue.front"));
    rearLabel.setText(I18N.text("label.visual.queue.rear"));
    frontLabel.setVisible(true);
    rearLabel.setVisible(true);
    frontLabel.relocate(40.0d, 30.0d);
    rearLabel.relocate(130.0d, 30.0d);
    dequeueLabel.relocate(40.0d, 86.0d);
    enqueueLabel.relocate(130.0d, 86.0d);
  }

  private static void relocateCentered(Text label, double centerX, double y) {
    label.applyCss();
    label.relocate(centerX - textWidth(label) / 2.0d, y);
  }

  private static double textWidth(Text label) {
    label.applyCss();
    return Math.max(0.0d, label.getLayoutBounds().getWidth());
  }

  public void setSelectionListener(IntConsumer listener) {
    selectionListener = listener == null ? ignored -> {} : listener;
  }

  public void clearSelection() {
    selectedIndex = -1;
    pendingSelectedIndex = -1;
    submitCurrentPresentation();
  }

  public void selectIndex(int index) {
    if (showSelection(index))
      selectionListener.accept(index);
  }

  public boolean showSelection(int index) {
    if (index < 0)
      return false;
    LinearStructureViewState state = currentState();
    int currentSize = state == null ? items.size() : state.values().size();
    if (index >= currentSize)
      return false;
    selectedIndex = index;
    pendingSelectedIndex = items.containsKey(index) ? -1 : index;
    submitCurrentPresentation();
    return true;
  }

  private void submitCurrentPresentation() {
    LinearStructureViewState state = currentState();
    if (state != null && isModuleAttached() && !isDisposed()) {
      renderFramework.submit(new PresentationRenderIntent<>(SESSION_ID, state));
    }
  }

  private void applyPendingSelection(int size) {
    if (selectedIndex >= size)
      selectedIndex = -1;
    if (pendingSelectedIndex < 0)
      return;
    if (pendingSelectedIndex < size)
      selectedIndex = pendingSelectedIndex;
    pendingSelectedIndex = -1;
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
  public void setViewportObstructionInsets(javafx.geometry.Insets insets) {
    surface.setObstructionInsets(insets);
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
  public void onVisualizationReset() {
    items.clear();
    surface.nodeLayer().getChildren().clear();
    surface.edgeLayer().getChildren().clear();
    selectedIndex = -1;
    pendingSelectedIndex = -1;
    lastSubmittedState = null;
    lastGeometry = Map.of();
    surface.reset();
    surface.decorationLayer().getChildren().setAll(
        frontLabel, rearLabel, dequeueLabel, enqueueLabel);
    positionEmptyLabels();
    surface.markViewportPristine();
  }

  @Override
  public void dispose() {
    I18N.localeProperty().removeListener(localeListener);
    renderFramework.unregisterSurface(SESSION_ID, this);
    surface.prefWidthProperty().unbind();
    surface.prefHeightProperty().unbind();
    super.dispose();
  }

  private static Point2D center(ElementGeometry bounds) {
    return new Point2D(bounds.x() + bounds.width() / 2.0d, bounds.y() + bounds.height() / 2.0d);
  }

  private static String id(int index) {
    return "queue:" + index;
  }
}
