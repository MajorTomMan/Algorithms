package com.majortom.algorithms.visualization.impl.visualizer.graph;

import com.majortom.algorithms.visualization.render.api.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Headless scalability regression: a dense, cyclic multi-graph must avoid force/ELK,
 * preserve all node IDs, and keep distinct routes for parallel directed edges.
 */
public final class GraphDenseLayoutChecks {
  public static void main(String[] args) {
    int nodes = 3000;
    List<LayoutElement> vertices = new ArrayList<>(nodes);
    List<LayoutLink> links = new ArrayList<>(nodes * 2);
    for (int i = 0; i < nodes; i++) {
      vertices.add(new LayoutElement(GraphElkLayout.nodeId(i + 1), 56, 56));
      String from = GraphElkLayout.nodeId(i + 1);
      String to = GraphElkLayout.nodeId((i + 1) % nodes + 1);
      links.add(new LayoutLink(GraphElkLayout.edgeId(i * 2 + 1), from, to));
      links.add(new LayoutLink(GraphElkLayout.edgeId(i * 2 + 2), from, to));
    }
    LayoutRequest request = new LayoutRequest(new LayoutRequestId(1),
        RenderSessionId.of("GRAPH"), 1, 1, GraphElkLayout.ID, vertices, links,
        Map.of(LayoutMetadataKeys.DIRECTED, "true"));
    if (!GraphRenderScale.fastLayout(vertices.size(), links.size())
        || GraphRenderScale.nodeIds(vertices.size(), links.size())
        || GraphRenderScale.allEdgeLabels(vertices.size(), links.size()))
      throw new AssertionError("Dense policy not applied");

    GraphElkLayout engine = new GraphElkLayout();
    LayoutResult result = engine.layout(request);
    if (result.elements().size() != nodes || result.edges().size() != links.size())
      throw new AssertionError("Dense layout dropped nodes or parallel edges");
    var byEdgeId = new java.util.HashMap<String, EdgeGeometry>();
    for (var route : result.edges()) byEdgeId.put(route.id(), route);
    var first = byEdgeId.get(GraphElkLayout.edgeId(1));
    var second = byEdgeId.get(GraphElkLayout.edgeId(2));
    if (first == null || second == null
        || first.points().equals(second.points()))
      throw new AssertionError("Parallel routes collapsed");

    // Same inputs produce stable, reproducible positions and separate routes.
    LayoutResult replay = engine.layout(request);
    if (!result.elements().equals(replay.elements())
        || !result.edges().equals(replay.edges()))
      throw new AssertionError("Large graph layout is not deterministic");
    if (result.bounds().width() <= 0 || result.bounds().height() <= 0)
      throw new AssertionError("Invalid dense graph bounds");

    // Separate components and isolated vertices remain represented and grouped.
    List<LayoutElement> disconnected = new ArrayList<>();
    for (int i = 1; i <= 201; i++)
      disconnected.add(new LayoutElement(GraphElkLayout.nodeId(i), 56, 56));
    List<LayoutLink> relations = List.of(
        new LayoutLink("first", GraphElkLayout.nodeId(1), GraphElkLayout.nodeId(100)),
        new LayoutLink("second", GraphElkLayout.nodeId(2), GraphElkLayout.nodeId(101)));
    LayoutRequest independent = new LayoutRequest(new LayoutRequestId(2),
        RenderSessionId.of("GRAPH"), 1, 1, GraphElkLayout.ID, disconnected, relations,
        Map.of(LayoutMetadataKeys.DIRECTED, "true"));
    LayoutResult independentLayout = engine.layout(independent);
    if (independentLayout.elements().size() != 201
        || !independentLayout.elements().containsKey(GraphElkLayout.nodeId(201)))
      throw new AssertionError("Disconnected graph or isolated vertex missing");
    var firstPoint = independentLayout.elements().get(GraphElkLayout.nodeId(1));
    var sameComponentPoint = independentLayout.elements().get(GraphElkLayout.nodeId(100));
    if (Math.abs(firstPoint.x() - sameComponentPoint.x()) > 200
        && Math.abs(firstPoint.y() - sameComponentPoint.y()) > 200)
      throw new AssertionError("Connected vertices should be packed together");

    if (GraphRenderScale.fastLayout(5, 3) || !GraphRenderScale.nodeIds(5, 3)
        || !GraphRenderScale.allEdgeLabels(5, 3))
      throw new AssertionError("Small graph quality policy regressed");
    System.out.println("GraphDenseLayoutChecks passed");
  }
}
