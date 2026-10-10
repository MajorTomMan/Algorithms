package com.majortom.algorithms.visualization.impl.visualizer.linear;

import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.geometry.RectangleGeometry;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.impl.visualizer.semantic.LinearStructureVisualization;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntConsumer;
import javafx.geometry.Point2D;

/** Shared FX infrastructure for Stack/Queue indexed node lanes. */
public final class LinearNodeSupport {
  private final VisualizationSurface surface;
  private final LinearStructureLayoutSpec spec;
  private final String itemStyleClass;
  private final Map<Integer, NodeView> items = new LinkedHashMap<>();

  private IntConsumer selectionListener = ignored -> {};
  private int selectedIndex = -1;
  private int pendingSelectedIndex = -1;

  public LinearNodeSupport(
      VisualizationSurface surface,
      LinearStructureLayoutSpec spec,
      String itemStyleClass) {
    this.surface = Objects.requireNonNull(surface, "surface");
    this.spec = Objects.requireNonNull(spec, "spec");
    this.itemStyleClass = Objects.requireNonNull(itemStyleClass, "itemStyleClass");
  }

  public Map<Integer, NodeView> items() {
    return Collections.unmodifiableMap(items);
  }

  public NodeView get(int index) {
    return items.get(index);
  }

  public int size() {
    return items.size();
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }

  public boolean contains(int index) {
    return items.containsKey(index);
  }

  public NodeView detach(int index) {
    return items.remove(index);
  }

  public NodeView remove(int index) {
    NodeView removed = items.remove(index);
    if (removed != null) {
      surface.nodeLayer().getChildren().remove(removed);
    }
    return removed;
  }

  public void reconcile(List<VisualValue> values) {
    Objects.requireNonNull(values, "values");
    int size = values.size();
    List<Integer> stale = items.keySet().stream()
        .filter(index -> index < 0 || index >= size)
        .toList();
    for (Integer index : stale) {
      remove(index);
    }

    for (int index = 0; index < size; index++) {
      NodeView item = items.get(index);
      if (item == null) {
        item = createItem(index, values.get(index).text());
        items.put(index, item);
        surface.nodeLayer().getChildren().add(item);
      } else {
        item.setText(values.get(index).text());
        installSelectionHandler(item, index);
      }
    }
  }

  public void applyLayout(LayoutResult patch) {
    Objects.requireNonNull(patch, "patch");
    for (Map.Entry<Integer, NodeView> entry : items.entrySet()) {
      ElementGeometry bounds =
          patch.elements().get(LinearStructureVisualization.elementId(spec.structure(), entry.getKey()));
      if (bounds == null) {
        continue;
      }
      NodeView item = entry.getValue();
      item.setGeometry(new RectangleGeometry(bounds.width(), bounds.height()));
      Point2D target = center(bounds);
      item.setCenter(target.getX(), target.getY());
      item.setOpacity(1.0d);
    }
  }

  public void shiftIndexes(int fromInclusive, int toInclusive, int delta) {
    if (delta == 0 || fromInclusive > toInclusive) {
      return;
    }
    if (delta > 0) {
      for (int oldIndex = toInclusive; oldIndex >= fromInclusive; oldIndex--) {
        NodeView item = items.remove(oldIndex);
        if (item != null) {
          items.put(oldIndex + delta, item);
        }
      }
    } else {
      for (int oldIndex = fromInclusive; oldIndex <= toInclusive; oldIndex++) {
        NodeView item = items.remove(oldIndex);
        if (item != null) {
          items.put(oldIndex + delta, item);
        }
      }
    }
  }

  public void setSelectionListener(IntConsumer listener) {
    selectionListener = listener == null ? ignored -> {} : listener;
  }

  public void clearSelection() {
    selectedIndex = -1;
    pendingSelectedIndex = -1;
  }

  public boolean showSelection(int index) {
    if (index < 0) {
      return false;
    }
    selectedIndex = index;
    pendingSelectedIndex = items.containsKey(index) ? -1 : index;
    return true;
  }

  public void selectIndex(int index) {
    if (showSelection(index)) {
      selectionListener.accept(index);
    }
  }

  public void applyPendingSelection(int size) {
    if (selectedIndex >= size) {
      selectedIndex = -1;
    }
    if (pendingSelectedIndex < 0) {
      return;
    }
    if (pendingSelectedIndex < size) {
      selectedIndex = pendingSelectedIndex;
    }
    pendingSelectedIndex = -1;
  }

  public boolean isSelected(int index) {
    return selectedIndex == index;
  }

  public void clearItems() {
    items.clear();
    surface.nodeLayer().getChildren().clear();
  }

  public void reset() {
    clearItems();
    clearSelection();
  }

  private NodeView createItem(int index, String text) {
    NodeView item = new NodeView(
        new RectangleGeometry(spec.minWidth(), spec.height()), text);
    item.getStyleClass().add(itemStyleClass);
    installSelectionHandler(item, index);
    return item;
  }

  private void installSelectionHandler(NodeView item, int index) {
    item.setOnMouseClicked(event -> {
      selectedIndex = index;
      selectionListener.accept(index);
      event.consume();
    });
  }

  private static Point2D center(ElementGeometry bounds) {
    return new Point2D(
        bounds.x() + bounds.width() / 2.0d,
        bounds.y() + bounds.height() / 2.0d);
  }
}
