package com.majortom.algorithms.core.event.structure;

public sealed interface GraphStructureEvent
    extends StructureEvent permits GraphStructureEvent.VertexAdded,
            GraphStructureEvent.VertexRemoved, GraphStructureEvent.EdgeAdded,
            GraphStructureEvent.EdgeRemoved, GraphStructureEvent.EdgeWeightChanged {
  record VertexAdded(long vertexId, Object value) implements GraphStructureEvent {}

  record VertexRemoved(long vertexId, Object value) implements GraphStructureEvent {}

  record EdgeAdded(long edgeId, long fromId, long toId, double weight)
      implements GraphStructureEvent {
    public EdgeAdded {
      if (edgeId <= 0 || fromId <= 0 || toId <= 0 || !Double.isFinite(weight)) {
        throw new IllegalArgumentException("invalid graph edge");
      }
    }
  }

  record EdgeRemoved(long edgeId, long fromId, long toId) implements GraphStructureEvent {}

  record EdgeWeightChanged(long edgeId, double previousWeight, double weight)
      implements GraphStructureEvent {
    public EdgeWeightChanged {
      if (edgeId <= 0 || !Double.isFinite(previousWeight) || !Double.isFinite(weight)) {
        throw new IllegalArgumentException("invalid edge weight change");
      }
    }
  }
}
