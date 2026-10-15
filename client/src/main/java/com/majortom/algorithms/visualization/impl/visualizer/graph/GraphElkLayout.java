package com.majortom.algorithms.visualization.impl.visualizer.graph;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutMetadataKeys;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.layout.LayoutEngine;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.alg.layered.options.OrderingStrategy;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.options.EdgeRouting;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkBendPoint;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

/** JavaFX-neutral graph layout engine owned by RenderFramework's Layout Pool. */
public final class GraphElkLayout implements LayoutEngine {
  public static final String ID = "graph";
  private static final double PADDING = 42.0d;
  private static final double NODE_SPACING = 42.0d;
  private static final double LAYER_SPACING = 64.0d;
  private static final int RANDOM_SEED = 1;
  private final GraphTopologyLayout topologyLayout = new GraphTopologyLayout();
  private final GraphDenseLayout denseLayout = new GraphDenseLayout();
  private final GraphParallelEdgeLayout parallelLayout = new GraphParallelEdgeLayout();
  private final GraphLabelLayout labelLayout = new GraphLabelLayout();

  @Override
  public String id() {
    return ID;
  }

  @Override
  public LayoutResult layout(LayoutRequest request) {
    if (request.elements().isEmpty()) {
      return new LayoutResult(request.requestId(), request.modelRevision(), Map.of(), List.of(),
          BoundsSnapshot.empty());
    }

    // Dense imports must avoid both ELK and the quadratic force-layout branch.
    // Keep parallel-edge routing for distinct edge IDs, but omit expensive decorations.
    if (GraphRenderScale.fastLayout(request.elements().size(), request.links().size())) {
      return parallelLayout.spread(request, denseLayout.layout(request));
    }

    boolean directed = Boolean.parseBoolean(
        request.metadata().getOrDefault(LayoutMetadataKeys.DIRECTED, Boolean.FALSE.toString()));
    LayoutResult topology = directed && isDirectedAcyclic(request)
        ? layeredLayout(request)
        : topologyLayout.layout(request);
    return labelLayout.resolve(request, parallelLayout.spread(request, topology));
  }

  private LayoutResult layeredLayout(LayoutRequest request) {
    ElkNode graph = ElkGraphUtil.createGraph();
    graph.setProperty(CoreOptions.ALGORITHM, LayeredOptions.ALGORITHM_ID);
    graph.setProperty(CoreOptions.DIRECTION, Direction.DOWN);
    graph.setProperty(CoreOptions.EDGE_ROUTING, EdgeRouting.POLYLINE);
    graph.setProperty(CoreOptions.PADDING, new ElkPadding(PADDING));
    graph.setProperty(CoreOptions.RANDOM_SEED, RANDOM_SEED);
    graph.setProperty(CoreOptions.SPACING_NODE_NODE, NODE_SPACING);
    graph.setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, LAYER_SPACING);
    graph.setProperty(
        LayeredOptions.CONSIDER_MODEL_ORDER_STRATEGY, OrderingStrategy.NODES_AND_EDGES);

    Map<String, ElkNode> elkNodes = new LinkedHashMap<>();
    request.elements().stream().sorted(Comparator.comparing(LayoutElement::id)).forEach(element -> {
      ElkNode node = ElkGraphUtil.createNode(graph);
      node.setIdentifier(element.id());
      node.setDimensions(element.width(), element.height());
      elkNodes.put(element.id(), node);
    });
    for (LayoutLink link : request.links()) {
      ElkNode source = elkNodes.get(link.sourceId());
      ElkNode target = elkNodes.get(link.targetId());
      if (source == null || target == null)
        continue;
      ElkEdge edge = ElkGraphUtil.createSimpleEdge(source, target);
      edge.setIdentifier(link.id());
    }

    new RecursiveGraphLayoutEngine().layout(graph, new BasicProgressMonitor());

    Map<String, ElementGeometry> elements = new LinkedHashMap<>();
    for (Map.Entry<String, ElkNode> entry : elkNodes.entrySet()) {
      ElkNode node = entry.getValue();
      elements.put(entry.getKey(),
          new ElementGeometry(
              entry.getKey(), node.getX(), node.getY(), node.getWidth(), node.getHeight()));
    }
    List<EdgeGeometry> edges = new ArrayList<>();
    for (ElkEdge edge : graph.getContainedEdges()) {
      if (edge.getIdentifier() == null || edge.getSections().isEmpty())
        continue;
      ElkEdgeSection section = edge.getSections().getFirst();
      List<EdgeGeometry.Point> points = new ArrayList<>();
      points.add(new EdgeGeometry.Point(section.getStartX(), section.getStartY()));
      for (ElkBendPoint bendPoint : section.getBendPoints()) {
        points.add(new EdgeGeometry.Point(bendPoint.getX(), bendPoint.getY()));
      }
      points.add(new EdgeGeometry.Point(section.getEndX(), section.getEndY()));
      edges.add(new EdgeGeometry(edge.getIdentifier(), points));
    }
    return new LayoutResult(
        request.requestId(), request.modelRevision(), elements, edges, contentBounds(elements));
  }

  private boolean isDirectedAcyclic(LayoutRequest request) {
    // Kahn's algorithm only needs indegrees: its visit order does not affect
    // whether every node can be removed. Do not sort nodes or neighbors here.
    Map<String, Integer> indegree = new HashMap<>();
    Map<String, List<String>> outgoing = new HashMap<>();
    for (LayoutElement element : request.elements()) {
      indegree.put(element.id(), 0);
      outgoing.put(element.id(), new ArrayList<>());
    }
    for (LayoutLink link : request.links()) {
      if (!indegree.containsKey(link.sourceId()) || !indegree.containsKey(link.targetId()))
        continue;
      if (link.sourceId().equals(link.targetId()))
        return false;
      outgoing.get(link.sourceId()).add(link.targetId());
      indegree.compute(link.targetId(), (ignored, current) -> current + 1);
    }
    ArrayDeque<String> ready = new ArrayDeque<>();
    for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
      if (entry.getValue() == 0) ready.addLast(entry.getKey());
    }
    int visited = 0;
    while (!ready.isEmpty()) {
      String current = ready.removeFirst();
      visited++;
      for (String target : outgoing.get(current)) {
        int next = indegree.compute(target, (ignored, value) -> value - 1);
        if (next == 0) ready.addLast(target);
      }
    }
    return visited == indegree.size();
  }

  private static BoundsSnapshot contentBounds(Map<String, ElementGeometry> elements) {
    if (elements.isEmpty())
      return BoundsSnapshot.empty();
    double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
    for (ElementGeometry element : elements.values()) {
      minX = Math.min(minX, element.x());
      minY = Math.min(minY, element.y());
      maxX = Math.max(maxX, element.x() + element.width());
      maxY = Math.max(maxY, element.y() + element.height());
    }
    return new BoundsSnapshot(minX, minY, maxX - minX, maxY - minY);
  }

  public static String nodeId(long id) {
    return GraphVisualIds.node(id);
  }

  public static String edgeId(long id) {
    return GraphVisualIds.edge(id);
  }
}
