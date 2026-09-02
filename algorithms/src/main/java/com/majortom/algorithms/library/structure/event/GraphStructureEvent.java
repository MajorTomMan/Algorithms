package com.majortom.algorithms.library.structure.event;

import com.majortom.algorithms.core.event.ExecutionEvent;

public sealed interface GraphStructureEvent extends ExecutionEvent
        permits GraphStructureEvent.VertexAdded, GraphStructureEvent.VertexRemoved,
        GraphStructureEvent.EdgeAdded, GraphStructureEvent.EdgeRemoved {
    record VertexAdded(Object vertex) implements GraphStructureEvent {}
    record VertexRemoved(Object vertex) implements GraphStructureEvent {}
    record EdgeAdded(Object from, Object to) implements GraphStructureEvent {}
    record EdgeRemoved(Object from, Object to) implements GraphStructureEvent {}
}
