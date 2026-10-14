package com.majortom.algorithms.core.event.structure;

import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import java.util.List;

/** Facts emitted by the single graph engine, including parallel edges. */
public sealed interface GraphStructureEvent extends StructureEvent
    permits GraphStructureEvent.VertexAdded, GraphStructureEvent.VertexRemoved,
            GraphStructureEvent.EdgeAdded, GraphStructureEvent.EdgesAdded,
            GraphStructureEvent.EdgeRemoved, GraphStructureEvent.EdgeWeightChanged,
            GraphStructureEvent.Reset {
  record VertexAdded(long vertexId, Object value) implements GraphStructureEvent {}
  record VertexRemoved(long vertexId, Object value, List<Long> incidentEdges)
      implements GraphStructureEvent {
    public VertexRemoved(long vertexId, Object value) {
      this(vertexId, value, List.of());
    }
    public VertexRemoved { incidentEdges = List.copyOf(incidentEdges); }
  }
  record EdgeAdded(long edgeId, long fromId, long toId, Object value, double weight)
      implements GraphStructureEvent {
    public EdgeAdded(long edgeId, long fromId, long toId, double weight) {
      this(edgeId, fromId, toId, null, weight);
    }
  }
  record EdgesAdded(EdgeAdded forward, EdgeAdded reverse) implements GraphStructureEvent {}
  record EdgeRemoved(long edgeId, long fromId, long toId) implements GraphStructureEvent {
    public EdgeRemoved(long edgeId) { this(edgeId, 0L, 0L); }
  }
  record EdgeWeightChanged(long edgeId, double previousWeight, double weight)
      implements GraphStructureEvent {
    public double previous() { return previousWeight; }
    public double current() { return weight; }
  }
  record Reset(GraphSnapshot<?> snapshot) implements GraphStructureEvent {}
}
