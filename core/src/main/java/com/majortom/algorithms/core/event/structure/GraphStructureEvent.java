package com.majortom.algorithms.core.event.structure;


public sealed interface GraphStructureEvent extends StructureEvent
        permits GraphStructureEvent.VertexAdded, GraphStructureEvent.VertexRemoved,
        GraphStructureEvent.EdgeAdded, GraphStructureEvent.EdgeRemoved,
        GraphStructureEvent.EdgeWeightChanged {

    record VertexAdded(long vertexId, Object value) implements GraphStructureEvent {}

    record VertexRemoved(long vertexId, Object value) implements GraphStructureEvent {}

    record EdgeAdded(long edgeId, long fromId, long toId) implements GraphStructureEvent {}

    record EdgeRemoved(long edgeId, long fromId, long toId) implements GraphStructureEvent {}

    record EdgeWeightChanged(long edgeId, Double previousWeight, double weight) implements GraphStructureEvent {
        public EdgeWeightChanged {
            if (edgeId <= 0) {
                throw new IllegalArgumentException("edge id must be positive");
            }
            if (previousWeight != null && !Double.isFinite(previousWeight)) {
                throw new IllegalArgumentException("previous edge weight must be finite");
            }
            if (!Double.isFinite(weight)) {
                throw new IllegalArgumentException("edge weight must be finite");
            }
        }
    }
}
