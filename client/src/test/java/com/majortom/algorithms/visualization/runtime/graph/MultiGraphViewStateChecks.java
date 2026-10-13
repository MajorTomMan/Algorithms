package com.majortom.algorithms.visualization.runtime.graph;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.snapshot.MultiGraphSnapshot;
import java.util.List;

/** Validates that identical endpoints retain distinct edge identities and labels. */
public final class MultiGraphViewStateChecks {
  record Railway(String name) {}
  record Road(String name) {}
  public static void main(String[] args) {
    var snapshot = new MultiGraphSnapshot<>(GraphDirection.DIRECTED,
        List.of(new MultiGraphSnapshot.Vertex<>(1, "深圳"),
                new MultiGraphSnapshot.Vertex<>(2, "广州"),
                new MultiGraphSnapshot.Vertex<>(3, "东莞")),
        List.of(new MultiGraphSnapshot.Edge(10, 1, 2, new Railway("高铁"), 102),
                new MultiGraphSnapshot.Edge(11, 1, 2, new Road("高速"), 125)));
    GraphViewState view = GraphViewState.initial(snapshot);
    if (view.nodes().size() != 3 || view.edges().size() != 2) throw new AssertionError("topology");
    if (!view.edges().getFirst().label().equals("Railway")
        || !view.edges().get(1).label().equals("Road"))
      throw new AssertionError("edge payload distinction");
    if (!com.majortom.algorithms.visualization.impl.visualizer.graph.GraphVisualText
        .edgeLabel(view.edges().getFirst()).contains("102"))
      throw new AssertionError("edge weight");
    System.out.println("MultiGraphViewStateChecks passed");
  }
}
