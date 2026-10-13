package com.majortom.algorithms.core.event.structure;
import com.majortom.algorithms.core.snapshot.MultiGraphSnapshot;
import java.util.List;
/** Separate event family so existing simple-Graph reducers cannot misinterpret parallel edges. */
public sealed interface MultiGraphStructureEvent extends StructureEvent
    permits MultiGraphStructureEvent.VertexAdded, MultiGraphStructureEvent.VertexRemoved,
            MultiGraphStructureEvent.EdgeAdded, MultiGraphStructureEvent.EdgesAdded,
            MultiGraphStructureEvent.EdgeRemoved, MultiGraphStructureEvent.EdgeWeightChanged,
            MultiGraphStructureEvent.Reset {
  record VertexAdded(long vertexId, Object value) implements MultiGraphStructureEvent {}
  record VertexRemoved(long vertexId, Object value, List<Long> incidentEdges)
      implements MultiGraphStructureEvent {
    public VertexRemoved { incidentEdges = List.copyOf(incidentEdges); }
  }
  record EdgeAdded(long edgeId, long fromId, long toId, Object value, double weight)
      implements MultiGraphStructureEvent {}
  record EdgesAdded(EdgeAdded forward, EdgeAdded reverse) implements MultiGraphStructureEvent {}
  record EdgeRemoved(long edgeId) implements MultiGraphStructureEvent {}
  record EdgeWeightChanged(long edgeId, double previous, double current)
      implements MultiGraphStructureEvent {}
  record Reset(MultiGraphSnapshot<?> snapshot) implements MultiGraphStructureEvent {}
}
