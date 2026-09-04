package com.majortom.algorithms.core.event.structure;


public sealed interface GraphStructureEvent extends StructureEvent
        permits GraphStructureEvent.VertexAdded, GraphStructureEvent.VertexRemoved,
        GraphStructureEvent.EdgeAdded, GraphStructureEvent.EdgeRemoved {

    record VertexAdded(long vertexId, Object value) implements GraphStructureEvent {}

    record VertexRemoved(long vertexId, Object value) implements GraphStructureEvent {}

    record EdgeAdded(long edgeId, long fromId, long toId) implements GraphStructureEvent {}

    record EdgeRemoved(long edgeId, long fromId, long toId) implements GraphStructureEvent {}
}
