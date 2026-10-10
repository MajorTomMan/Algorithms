package com.majortom.algorithms.visualization.impl.visualizer.indexed;

import com.majortom.algorithms.visualization.animation.fx.AnimationSceneAdapter.NodeTarget;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import javafx.geometry.Point2D;
import javafx.scene.Node;

/**
 * Shared animation-scene lifecycle for index-addressed strip cells.
 *
 * <p>Structure-specific adapters still decide how a mutation maps old indexes to logical ids. This
 * helper owns only captured centers, exit-cell detachment, target lookup and final stabilization.</p>
 */
public final class IndexedStripAnimationSupport<C extends Node & IndexedStripCell> {
  private final IndexedStripSupport<C> strip;
  private final VisualizationSurface surface;
  private final IntFunction<String> logicalId;
  private final IntFunction<String> layoutId;
  private final ToIntFunction<String> activeIndex;
  private final ToIntFunction<String> exitIndex;

  private final Map<String, Point2D> capturedCenters = new LinkedHashMap<>();
  private final Map<String, C> exitingCells = new LinkedHashMap<>();
  private LayoutResult targetPatch;

  public IndexedStripAnimationSupport(
      IndexedStripSupport<C> strip,
      VisualizationSurface surface,
      IntFunction<String> logicalId,
      IntFunction<String> layoutId,
      ToIntFunction<String> activeIndex,
      ToIntFunction<String> exitIndex) {
    this.strip = Objects.requireNonNull(strip, "strip");
    this.surface = Objects.requireNonNull(surface, "surface");
    this.logicalId = Objects.requireNonNull(logicalId, "logicalId");
    this.layoutId = Objects.requireNonNull(layoutId, "layoutId");
    this.activeIndex = Objects.requireNonNull(activeIndex, "activeIndex");
    this.exitIndex = Objects.requireNonNull(exitIndex, "exitIndex");
  }

  public void begin(LayoutResult patch) {
    capturedCenters.clear();
    targetPatch = Objects.requireNonNull(patch, "patch");
  }

  public void capture(String logicalId, C cell) {
    capturedCenters.put(
        Objects.requireNonNull(logicalId, "logicalId"),
        IndexedStripSupport.visualCenter(Objects.requireNonNull(cell, "cell")));
  }

  public void detachExit(String logicalId) {
    int previousIndex = exitIndex.applyAsInt(logicalId);
    if (previousIndex < 0) {
      return;
    }
    C cell = strip.detach(previousIndex);
    if (cell != null) {
      exitingCells.put(logicalId, cell);
    }
  }

  public Collection<Node> exitingNodes() {
    return List.copyOf(exitingCells.values());
  }

  public Optional<NodeTarget> node(String logicalId) {
    int exitingIndex = exitIndex.applyAsInt(logicalId);
    if (exitingIndex >= 0) {
      C exiting = exitingCells.get(logicalId);
      Point2D center = capturedCenters.get(logicalId);
      return exiting == null || center == null
          ? Optional.empty()
          : Optional.of(new NodeTarget(logicalId, exiting, center, List.of()));
    }

    int index = activeIndex.applyAsInt(logicalId);
    if (index < 0) {
      return Optional.empty();
    }
    C cell = strip.get(index);
    ElementGeometry geometry =
        targetPatch == null ? null : targetPatch.elements().get(layoutId.apply(index));
    if (cell == null || geometry == null) {
      return Optional.empty();
    }
    return Optional.of(new NodeTarget(
        logicalId, cell, IndexedStripSupport.center(geometry), List.of()));
  }

  public Optional<Point2D> capturedNodeCenter(String logicalId) {
    return Optional.ofNullable(capturedCenters.get(logicalId));
  }

  public Collection<NodeTarget> activeNodes() {
    List<NodeTarget> result = new ArrayList<>(strip.size());
    for (Map.Entry<Integer, C> entry : strip.cells().entrySet()) {
      ElementGeometry geometry =
          targetPatch == null ? null : targetPatch.elements().get(layoutId.apply(entry.getKey()));
      if (geometry != null) {
        result.add(new NodeTarget(
            logicalId.apply(entry.getKey()),
            entry.getValue(),
            IndexedStripSupport.center(geometry),
            List.of()));
      }
    }
    return result;
  }

  public void discardExitedVisuals() {
    exitingCells.values().forEach(cell -> surface.nodeLayer().getChildren().remove(cell));
    exitingCells.clear();
  }

  public void stabilize() {
    for (NodeTarget target : activeNodes()) {
      Node node = target.node();
      node.setTranslateX(0.0d);
      node.setTranslateY(0.0d);
      node.setOpacity(1.0d);
      node.setScaleX(1.0d);
      node.setScaleY(1.0d);
    }
    discardExitedVisuals();
    capturedCenters.clear();
  }
}
