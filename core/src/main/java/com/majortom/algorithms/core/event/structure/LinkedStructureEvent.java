package com.majortom.algorithms.core.event.structure;


public sealed interface LinkedStructureEvent extends StructureEvent
        permits LinkedStructureEvent.NodeInserted, LinkedStructureEvent.NodeRemoved,
        LinkedStructureEvent.ValueChanged, LinkedStructureEvent.NextChanged,
        LinkedStructureEvent.PreviousChanged {

    record NodeInserted(long nodeId, Object value) implements LinkedStructureEvent {}

    record NodeRemoved(long nodeId, Object value) implements LinkedStructureEvent {}

    record ValueChanged(long nodeId, Object previousValue, Object value) implements LinkedStructureEvent {}

    record NextChanged(long nodeId, Long previousNextNodeId, Long nextNodeId) implements LinkedStructureEvent {}

    record PreviousChanged(long nodeId, Long previousPreviousNodeId, Long previousNodeId) implements LinkedStructureEvent {}
}
