package com.majortom.algorithms.visualization.impl.visualizer.hash.animation;

import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.fx.AnimationSceneAdapter;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javafx.geometry.Point2D;

/** FX animation boundary for Hash nodes/links. */
public final class HashTableAnimationSceneAdapter implements AnimationSceneAdapter {
  private final VisualizationSurface surface;
  private final Map<String, NodeView> nodes;
  private final Map<String, EdgeView> edges;
  private final Map<String, Point2D> capturedCenters = new LinkedHashMap<>();
  private final Map<String, List<Point2D>> capturedRoutes = new LinkedHashMap<>();
  private final Map<String, NodeView> exitingNodes = new LinkedHashMap<>();
  private final Map<String, EdgeView> exitingEdges = new LinkedHashMap<>();
  private LayoutResult targetPatch;

  public HashTableAnimationSceneAdapter(
      VisualizationSurface surface,
      Map<String, NodeView> nodes,
      Map<String, EdgeView> edges) {
    this.surface = Objects.requireNonNull(surface, "surface");
    this.nodes = Objects.requireNonNull(nodes, "nodes");
    this.edges = Objects.requireNonNull(edges, "edges");
  }

  public void prepare(AnimationPlan plan, LayoutResult patch) {
    capturedCenters.clear();
    capturedRoutes.clear();
    nodes.forEach((id, node) -> capturedCenters.put(id, node.visualCenter()));
    edges.forEach((id, edge) -> capturedRoutes.put(id, edge.routeSnapshot()));
    targetPatch = Objects.requireNonNull(patch, "patch");

    for (var timed : plan.steps()) {
      if (timed.step() instanceof AnimationStep.EdgeRemove remove) {
        detachEdge(remove.targetId());
      }
    }
    for (var timed : plan.steps()) {
      if (timed.step() instanceof AnimationStep.NodeExit exit) {
        detachNode(exit.targetId());
      }
    }
  }

  private void detachNode(String logicalId) {
    NodeView node = nodes.remove(logicalId);
    if (node != null) {
      exitingNodes.put(logicalId, node);
    }
  }

  private void detachEdge(String logicalId) {
    EdgeView edge = edges.remove(logicalId);
    if (edge != null) {
      exitingEdges.put(logicalId, edge);
    }
  }

  @Override
  public Optional<NodeTarget> node(String logicalId) {
    NodeView exiting = exitingNodes.get(logicalId);
    if (exiting != null) {
      Point2D center = capturedCenters.get(logicalId);
      return center == null
          ? Optional.empty()
          : Optional.of(new NodeTarget(logicalId, exiting, center, List.of()));
    }

    NodeView active = nodes.get(logicalId);
    ElementGeometry geometry =
        targetPatch == null ? null : targetPatch.elements().get(logicalId);
    if (active == null || geometry == null) {
      return Optional.empty();
    }
    return Optional.of(new NodeTarget(logicalId, active, center(geometry), List.of()));
  }

  @Override
  public Optional<EdgeTarget> edge(String logicalId) {
    EdgeView exiting = exitingEdges.get(logicalId);
    if (exiting != null) {
      return Optional.of(new EdgeTarget(logicalId, exiting));
    }
    EdgeView active = edges.get(logicalId);
    return active == null ? Optional.empty() : Optional.of(new EdgeTarget(logicalId, active));
  }

  @Override
  public Optional<Point2D> capturedNodeCenter(String logicalId) {
    return Optional.ofNullable(capturedCenters.get(logicalId));
  }

  @Override
  public Optional<List<Point2D>> capturedEdgeRoute(String logicalId) {
    return Optional.ofNullable(capturedRoutes.get(logicalId));
  }

  @Override
  public Collection<NodeTarget> activeNodes() {
    List<NodeTarget> result = new ArrayList<>(nodes.size());
    for (Map.Entry<String, NodeView> entry : nodes.entrySet()) {
      ElementGeometry geometry =
          targetPatch == null ? null : targetPatch.elements().get(entry.getKey());
      if (geometry != null) {
        result.add(new NodeTarget(
            entry.getKey(), entry.getValue(), center(geometry), List.of()));
      }
    }
    return result;
  }

  @Override
  public Collection<EdgeTarget> activeEdges() {
    return edges.entrySet().stream()
        .map(entry -> new EdgeTarget(entry.getKey(), entry.getValue()))
        .toList();
  }

  @Override
  public void discardExitedVisuals() {
    exitingEdges.values().forEach(edge -> {
      surface.edgeLayer().getChildren().remove(edge);
      edge.dispose();
    });
    exitingEdges.clear();
    exitingNodes.values().forEach(node -> surface.nodeLayer().getChildren().remove(node));
    exitingNodes.clear();
  }

  @Override
  public void stabilize(AnimationPlan plan) {
    for (NodeTarget target : activeNodes()) {
      target.node().setTranslateX(0.0d);
      target.node().setTranslateY(0.0d);
      target.node().setOpacity(1.0d);
      target.node().setScaleX(1.0d);
      target.node().setScaleY(1.0d);
    }
    for (EdgeTarget target : activeEdges()) {
      target.edge().setRevealProgress(1.0d);
      target.edge().setOpacity(1.0d);
    }
    discardExitedVisuals();
    capturedCenters.clear();
    capturedRoutes.clear();
  }

  private static Point2D center(ElementGeometry geometry) {
    return new Point2D(
        geometry.x() + geometry.width() / 2.0d,
        geometry.y() + geometry.height() / 2.0d);
  }
}
