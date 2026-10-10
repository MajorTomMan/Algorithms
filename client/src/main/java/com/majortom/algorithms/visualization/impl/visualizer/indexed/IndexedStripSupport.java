package com.majortom.algorithms.visualization.impl.visualizer.indexed;

import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;

/**
 * Shared FX infrastructure for index-addressed strip visualizers.
 *
 * <p>This class deliberately knows nothing about Array/String mutation or presentation semantics.
 * It owns only cell registration, index remapping, selection, layout application and scene order.</p>
 */
public final class IndexedStripSupport<C extends Node & IndexedStripCell> {
  private final VisualizationSurface surface;
  private final Map<Integer, C> cells = new LinkedHashMap<>();

  private IntConsumer selectionListener = ignored -> {};
  private int selectedIndex = -1;
  private int pendingSelectedIndex = -1;

  public IndexedStripSupport(VisualizationSurface surface) {
    this.surface = Objects.requireNonNull(surface, "surface");
  }

  public Map<Integer, C> cells() {
    return Collections.unmodifiableMap(cells);
  }

  public C get(int index) {
    return cells.get(index);
  }

  public boolean contains(int index) {
    return cells.containsKey(index);
  }

  public int size() {
    return cells.size();
  }

  public C detach(int index) {
    return cells.remove(index);
  }

  public C remove(int index) {
    C removed = cells.remove(index);
    if (removed != null) {
      surface.nodeLayer().getChildren().remove(removed);
    }
    return removed;
  }

  public void reconcile(int size, IntFunction<C> factory) {
    Objects.requireNonNull(factory, "factory");
    List<Integer> stale = cells.keySet().stream()
        .filter(index -> index < 0 || index >= size)
        .toList();
    for (Integer index : stale) {
      remove(index);
    }

    for (int index = 0; index < size; index++) {
      if (cells.containsKey(index)) {
        continue;
      }
      C cell = Objects.requireNonNull(factory.apply(index), "factory returned null");
      cell.setSelectionHandler(this::selectIndex);
      cells.put(index, cell);
      surface.nodeLayer().getChildren().add(cell);
    }
  }

  public void applyLayout(LayoutResult patch, IntFunction<String> layoutId) {
    Objects.requireNonNull(patch, "patch");
    Objects.requireNonNull(layoutId, "layoutId");
    for (Map.Entry<Integer, C> entry : cells.entrySet()) {
      ElementGeometry target = patch.elements().get(layoutId.apply(entry.getKey()));
      if (target == null) {
        continue;
      }
      C cell = entry.getValue();
      cell.setLayoutSize(target.width(), target.height());
      cell.relocate(target.x(), target.y());
    }
  }

  public void shiftIndexes(int fromInclusive, int toInclusive, int delta) {
    if (delta == 0 || fromInclusive > toInclusive) {
      return;
    }
    if (delta > 0) {
      for (int oldIndex = toInclusive; oldIndex >= fromInclusive; oldIndex--) {
        C cell = cells.remove(oldIndex);
        if (cell != null) {
          cells.put(oldIndex + delta, cell);
        }
      }
    } else {
      for (int oldIndex = fromInclusive; oldIndex <= toInclusive; oldIndex++) {
        C cell = cells.remove(oldIndex);
        if (cell != null) {
          cells.put(oldIndex + delta, cell);
        }
      }
    }
  }

  public void swap(int left, int right) {
    if (left == right) {
      return;
    }
    C leftCell = cells.get(left);
    C rightCell = cells.get(right);
    if (leftCell != null && rightCell != null) {
      cells.put(left, rightCell);
      cells.put(right, leftCell);
    }
  }

  public void removeRange(int fromInclusive, int toExclusive) {
    for (int index = fromInclusive; index < toExclusive; index++) {
      remove(index);
    }
  }

  public void normalizeOrder(Collection<? extends Node> trailingNodes) {
    List<Node> ordered = new ArrayList<>(cells.size()
        + (trailingNodes == null ? 0 : trailingNodes.size()));
    cells.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(Map.Entry::getValue)
        .forEach(ordered::add);
    if (trailingNodes != null) {
      ordered.addAll(trailingNodes);
    }
    surface.nodeLayer().getChildren().setAll(ordered);
  }

  public void clearCells() {
    cells.clear();
    surface.nodeLayer().getChildren().clear();
  }

  public void setSelectionListener(IntConsumer listener) {
    selectionListener = listener == null ? ignored -> {} : listener;
  }

  public void clearSelection() {
    selectedIndex = -1;
    pendingSelectedIndex = -1;
  }

  public int selectedIndex() {
    return selectedIndex;
  }

  public boolean isSelected(int index) {
    return selectedIndex == index;
  }

  public void selectIndex(int index) {
    if (showSelection(index)) {
      selectionListener.accept(index);
    }
  }

  public boolean showSelection(int index) {
    if (index < 0) {
      return false;
    }
    selectedIndex = index;
    pendingSelectedIndex = cells.containsKey(index) ? -1 : index;
    return true;
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
      selectionListener.accept(selectedIndex);
    }
    pendingSelectedIndex = -1;
  }

  public void resetSelection() {
    clearSelection();
  }

  public static Point2D visualCenter(Node node) {
    Bounds bounds = node.getBoundsInParent();
    return new Point2D(
        bounds.getMinX() + bounds.getWidth() / 2.0d,
        bounds.getMinY() + bounds.getHeight() / 2.0d);
  }

  public static Point2D center(ElementGeometry geometry) {
    return new Point2D(
        geometry.x() + geometry.width() / 2.0d,
        geometry.y() + geometry.height() / 2.0d);
  }
}
