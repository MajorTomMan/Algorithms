package com.majortom.algorithms.visualization.runtime.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.algorithm.graph.impl.KruskalMinimumSpanning;
import com.majortom.algorithms.core.event.structure.GraphStructureEvent;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.runtime.ExecutionStatus;
import com.majortom.algorithms.core.snapshot.WeightedGraphSnapshot;
import com.majortom.algorithms.structure.graph.WeightedGraph;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class KruskalPresentationRegressionTest {
  @Test
  void buildsOnlyResultEdgesOnOriginalVerticesWithoutSecondGraph() {
    WeightedGraphSnapshot<Integer> input = new WeightedGraphSnapshot<>(false,
        List.of(new WeightedGraphSnapshot.Vertex<>(101L, 1),
            new WeightedGraphSnapshot.Vertex<>(102L, 2),
            new WeightedGraphSnapshot.Vertex<>(103L, 3),
            new WeightedGraphSnapshot.Vertex<>(104L, 4)),
        List.of(new WeightedGraphSnapshot.Edge(201L, 101L, 102L, 1d),
            new WeightedGraphSnapshot.Edge(202L, 102L, 103L, 2d),
            new WeightedGraphSnapshot.Edge(203L, 103L, 104L, 3d),
            new WeightedGraphSnapshot.Edge(204L, 101L, 104L, 9d),
            new WeightedGraphSnapshot.Edge(205L, 101L, 103L, 8d)));
    WeightedGraph<Integer> source = WeightedGraph.fromSnapshot(input);
    GraphEventReducer reducer = new GraphEventReducer(
        new WeightedGraphSnapshot<>(false, input.vertices(), List.of()));
    GraphViewState[] presented = {reducer.initialState()};
    int[] edgeAdded = {0};
    int[] vertexAdded = {0};
    assertEquals(0, presented[0].edges().size());

    ExecutionResult execution = new ExecutionRuntime().execute("kruskal-regression", envelope -> {
      if (envelope.event() instanceof GraphStructureEvent.VertexAdded) vertexAdded[0]++;
      if (envelope.event() instanceof GraphStructureEvent.EdgeAdded) edgeAdded[0]++;
      presented[0] = reducer.reduce(presented[0], envelope).state();
      assertEquals(4, presented[0].nodes().size());
      assertTrue(presented[0].edges().size() <= 3);
    }, () -> new KruskalMinimumSpanning().build(source));

    assertEquals(ExecutionStatus.COMPLETED, execution.status());
    assertEquals(0, vertexAdded[0]);
    assertEquals(3, edgeAdded[0]);
    assertEquals(3, presented[0].edges().size());
    assertTrue(presented[0].completed());
    assertEquals(Set.of(101L, 102L, 103L, 104L),
        presented[0].nodes().stream().map(GraphViewState.Node::id).collect(Collectors.toSet()));
    assertFalse(presented[0].edges().stream().anyMatch(edge -> edge.fromId() > 104 || edge.toId() > 104));
    WeightedGraphSnapshot<?> output = (WeightedGraphSnapshot<?>) execution.output().orElseThrow();
    assertEquals(input.vertices().stream().map(WeightedGraphSnapshot.Vertex::id).toList(),
        output.vertices().stream().map(WeightedGraphSnapshot.Vertex::id).toList());
    assertEquals(3, output.edges().size());
    assertEquals(5, source.snapshot().edges().size());
  }
}
