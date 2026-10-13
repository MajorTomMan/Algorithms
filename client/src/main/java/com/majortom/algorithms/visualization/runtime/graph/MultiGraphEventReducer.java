package com.majortom.algorithms.visualization.runtime.graph;

import com.majortom.algorithms.core.event.structure.MultiGraphStructureEvent;
import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.domain.observation.GraphObservationDomains;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.snapshot.MultiGraphSnapshot;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Independent multigraph reducer; normal Graph events must never consume parallel-edge changes.
 * Uses a single immutable GraphViewState so the existing layout and JavaFX renderer are reused.
 */
public final class MultiGraphEventReducer implements EventReducer<GraphViewState> {
  private final MultiGraphSnapshot<?> initial;
  public MultiGraphEventReducer(MultiGraphSnapshot<?> initial) { this.initial = initial; }

  @Override public GraphViewState initialState() { return GraphViewState.initial(initial); }

  @Override public Reduction<GraphViewState> reduce(GraphViewState previous, EventEnvelope envelope) {
    Object event = envelope.event();
    if (event instanceof AlgorithmEvent.Visited visited
        && visited.ref() instanceof AlgorithmEvent.EntityRef ref
        && GraphObservationDomains.VERTEX.equals(ref.domain())) {
      java.util.Set<Long> updated = new java.util.HashSet<>(previous.visitedNodeIds());
      updated.add(ref.id());
      return Reduction.changed(new GraphViewState(previous.direction(), previous.nodes(),
          previous.edges(), updated, GraphViewState.Observation.visited(ref.id()), false),
          EventImportance.TRANSIENT, true);
    }
    if (event instanceof AlgorithmEvent.Examined examined
        && examined.fromRef() instanceof AlgorithmEvent.EntityRef from
        && examined.toRef() instanceof AlgorithmEvent.EntityRef to
        && GraphObservationDomains.VERTEX.equals(from.domain())
        && GraphObservationDomains.VERTEX.equals(to.domain())) {
      return Reduction.changed(new GraphViewState(previous.direction(), previous.nodes(),
          previous.edges(), previous.visitedNodeIds(),
          GraphViewState.Observation.examined(from.id(), to.id()), false),
          EventImportance.TRANSIENT, true);
    }
    if (event instanceof RunCompletedEvent) {
      return Reduction.changed(new GraphViewState(previous.direction(), previous.nodes(),
          previous.edges(), previous.visitedNodeIds(),
          GraphViewState.Observation.none(), true), EventImportance.TERMINAL, true);
    }
    if (event instanceof MultiGraphStructureEvent.Reset reset)
      return changed(GraphViewState.initial(reset.snapshot()));

    if (event instanceof MultiGraphStructureEvent.VertexAdded added) {
      List<GraphViewState.Node> nodes = new ArrayList<>(previous.nodes());
      nodes.add(new GraphViewState.Node(added.vertexId(), VisualValue.of(added.value())));
      return changed(state(previous, nodes, previous.edges()));
    }
    if (event instanceof MultiGraphStructureEvent.VertexRemoved removed) {
      List<GraphViewState.Node> nodes = previous.nodes().stream()
          .filter(node -> node.id() != removed.vertexId()).toList();
      List<GraphViewState.Edge> edges = previous.edges().stream()
          .filter(edge -> edge.fromId() != removed.vertexId()
              && edge.toId() != removed.vertexId()).toList();
      return changed(state(previous, nodes, edges));
    }
    if (event instanceof MultiGraphStructureEvent.EdgeAdded added)
      return changed(addEdges(previous, List.of(added)));
    if (event instanceof MultiGraphStructureEvent.EdgesAdded pair)
      return changed(addEdges(previous, List.of(pair.forward(), pair.reverse())));
    if (event instanceof MultiGraphStructureEvent.EdgeRemoved removed) {
      List<GraphViewState.Edge> edges = previous.edges().stream()
          .filter(edge -> edge.id() != removed.edgeId()).toList();
      return changed(state(previous, previous.nodes(), edges));
    }
    if (event instanceof MultiGraphStructureEvent.EdgeWeightChanged updated) {
      List<GraphViewState.Edge> edges = previous.edges().stream()
          .map(edge -> edge.id() == updated.edgeId()
              ? edge.withWeight(updated.current()) : edge).toList();
      return changed(state(previous, previous.nodes(), edges));
    }
    return Reduction.unchanged(previous, EventImportance.TRANSIENT);
  }

  private static GraphViewState addEdges(GraphViewState old,
      List<MultiGraphStructureEvent.EdgeAdded> additions) {
    List<GraphViewState.Edge> edges = new ArrayList<>(old.edges());
    for (var edge : additions) {
      String label = edge.value() == null ? null : ValueAdapters.project(edge.value()).summary();
      edges.add(new GraphViewState.Edge(edge.edgeId(), edge.fromId(), edge.toId(),
                                        edge.weight(), label));
    }
    return state(old, old.nodes(), edges);
  }
  private static GraphViewState state(GraphViewState old,
      List<GraphViewState.Node> nodes, List<GraphViewState.Edge> edges) {
    return new GraphViewState(old.direction(), nodes, edges,
        Set.of(), GraphViewState.Observation.none(), false);
  }
  private static Reduction<GraphViewState> changed(GraphViewState state) {
    return Reduction.changed(state, EventImportance.STATE_CHANGE, true);
  }
}
