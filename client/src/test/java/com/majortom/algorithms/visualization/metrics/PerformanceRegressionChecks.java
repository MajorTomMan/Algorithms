package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.structure.maze.GridPoint;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Standalone regression checks for derived metrics, without additional test dependencies.
 *
 * Run from the project root:
 * mvn -pl client -am test-compile
 * mvn -pl client exec:java -Dexec.classpathScope=test
 *     -Dexec.mainClass=com.majortom.algorithms.visualization.metrics.PerformanceRegressionChecks
 */
public final class PerformanceRegressionChecks {
  private PerformanceRegressionChecks() {}

  public static void main(String[] args) {
    mazeCounts();
    treeTopology();
    graphTopology();
    System.out.println("Performance metric regression checks passed.");
  }

  private static void mazeCounts() {
    MazeViewState start = MazeViewState.empty(3, 3);
    require(start.openCellCount() == 0, "initial maze must have no open cells");
    MazeViewState one = start.open(new GridPoint(1, 1));
    require(one.openCellCount() == 1, "opening a cell increments the count");
    require(start.openCellCount() == 0, "previous snapshots must remain immutable");
    MazeViewState repeated = one.open(new GridPoint(1, 1));
    require(repeated.openCellCount() == 1, "opening the same cell twice changes nothing");
    MazeViewState two = repeated.open(new GridPoint(1, 2));
    require(two.openCellCount() == 2, "multiple open cells must be counted");
    expectMetric(new MazeMetricsProvider().metrics(two, context("maze")),
        "openCells", "2");
  }

  private static void treeTopology() {
    Map<Long, TreeViewState.Node> nodes = Map.of(
        1L, TreeViewState.Node.binary(1L, 1, 2L, null),
        2L, TreeViewState.Node.binary(2L, 2, 3L, null),
        3L, TreeViewState.Node.binary(3L, 3, null, null));
    TreeViewState original = new TreeViewState(TreeViewState.Kind.BINARY,
        1L, nodes, Set.of(), Set.of(), Set.of(), false);
    TreeMetricsProvider provider = new TreeMetricsProvider();
    List<MetricItem> metrics = provider.metrics(original, context(StructureIds.AVL_TREE));
    expectMetric(metrics, "height", "3");
    expectMetric(metrics, "leaves", "1");
    expectMetric(metrics, "maxBalance", "2");

    TreeViewState observed = new TreeViewState(TreeViewState.Kind.BINARY,
        1L, original.nodes(), Set.of(2L), Set.of(), Set.of(1L, 2L), false);
    expectMetric(provider.metrics(observed, context(StructureIds.AVL_TREE)), "height", "3");
    require(provider.samples(observed).get(StateMetricKeys.VISITED) == 2L,
        "visited-node sample must remain up-to-date");

    TreeViewState changed = new TreeViewState(TreeViewState.Kind.BINARY,
        1L, Map.of(1L, TreeViewState.Node.binary(1L, 1, null, null)),
        Set.of(), Set.of(), Set.of(), false);
    expectMetric(provider.metrics(changed, context(StructureIds.AVL_TREE)), "height", "1");
    expectMetric(provider.metrics(changed, context(StructureIds.AVL_TREE)), "maxBalance", "0");
  }

  private static void graphTopology() {
    List<GraphViewState.Node> nodes = List.of(
        new GraphViewState.Node(1L, VisualValue.of(1)),
        new GraphViewState.Node(2L, VisualValue.of(2)),
        new GraphViewState.Node(3L, VisualValue.of(3)));
    List<GraphViewState.Edge> firstEdges = List.of(
        new GraphViewState.Edge(10L, 1L, 2L, 3.0d));
    GraphViewState original = new GraphViewState(GraphDirection.UNDIRECTED,
        nodes, firstEdges, Set.of(), GraphViewState.Observation.none(), false);
    GraphMetricsProvider provider = new GraphMetricsProvider();
    expectMetric(provider.metrics(original, context("graph")), "components", "2");
    expectMetric(provider.metrics(original, context("graph")), "weight", "3");

    GraphViewState visited = new GraphViewState(GraphDirection.UNDIRECTED,
        original.nodes(), original.edges(), Set.of(1L), GraphViewState.Observation.none(), false);
    expectMetric(provider.metrics(visited, context("graph")), "components", "2");

    GraphViewState connected = new GraphViewState(GraphDirection.UNDIRECTED,
        nodes, List.of(firstEdges.getFirst(), new GraphViewState.Edge(11L, 2L, 3L, 7.0d)),
        Set.of(), GraphViewState.Observation.none(), false);
    expectMetric(provider.metrics(connected, context("graph")), "components", "1");
    expectMetric(provider.metrics(connected, context("graph")), "weight", "10");
  }

  private static StructureMetricsContext context(String structureId) {
    return new StructureMetricsContext(structureId, List.of());
  }

  private static void expectMetric(List<MetricItem> metrics, String key, String expected) {
    for (MetricItem metric : metrics) {
      if (metric.key().equals(key)) {
        require(metric.value().equals(expected),
            "metric " + key + " expected " + expected + ", got " + metric.value());
        return;
      }
    }
    throw new AssertionError("Missing metric: " + key);
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
