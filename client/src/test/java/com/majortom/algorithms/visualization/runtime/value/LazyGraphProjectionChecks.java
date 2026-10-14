package com.majortom.algorithms.visualization.runtime.value;

import com.majortom.algorithms.core.annotation.ValueDisplay;
import com.majortom.algorithms.core.annotation.ValueField;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Verifies large-graph import never calls DETAIL-only getters until the selected
 * object is explicitly projected for the inspector.
 */
public final class LazyGraphProjectionChecks {
  private static final AtomicInteger DETAIL_READS = new AtomicInteger();
  record City(@ValueField(value = ValueDisplay.SUMMARY) String name) {
    @ValueField(value = ValueDisplay.DETAIL)
    int expensiveDetails() {
      return DETAIL_READS.incrementAndGet();
    }
  }
  record Railway(@ValueField(value = ValueDisplay.SUMMARY) String name) {
    @ValueField(value = ValueDisplay.DETAIL)
    int expensiveDetails() {
      return DETAIL_READS.incrementAndGet();
    }
  }

  public static void main(String[] args) {
    City a = new City("深圳");
    City b = new City("广州");
    Railway rail = new Railway("广深铁路");
    GraphSnapshot<City> snapshot = new GraphSnapshot<>(GraphDirection.DIRECTED,
        List.of(new GraphSnapshot.Vertex<>(1, a),
                new GraphSnapshot.Vertex<>(2, b)),
        List.of(new GraphSnapshot.Edge(3, 1, 2, rail, 102.0)));
    GraphViewState view = GraphViewState.initial(snapshot);
    if (DETAIL_READS.get() != 0) throw new AssertionError("Eager DETAIL reflection");
    if (!view.nodes().getFirst().value().text().equals("深圳")
        || !view.edges().getFirst().label().equals("广深铁路"))
      throw new AssertionError("Missing compact summary labels");

    var inspected = ValueAdapters.project(a);
    var inspectedEdge = ValueAdapters.project(rail);
    if (DETAIL_READS.get() != 2 || !inspected.details().contains("expensiveDetails")
        || !inspectedEdge.details().contains("expensiveDetails"))
      throw new AssertionError("DETAIL fields not populated on demand");
    System.out.println("LazyGraphProjectionChecks passed");
  }
}
