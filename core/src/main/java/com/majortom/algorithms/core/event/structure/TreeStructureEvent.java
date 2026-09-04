package com.majortom.algorithms.core.event.structure;


public sealed interface TreeStructureEvent extends StructureEvent
        permits TreeStructureEvent.NodeInserted, TreeStructureEvent.NodeRemoved,
        TreeStructureEvent.ValueChanged, TreeStructureEvent.LeftChanged,
        TreeStructureEvent.RightChanged, TreeStructureEvent.RootChanged,
        TreeStructureEvent.ChildInserted, TreeStructureEvent.ChildRemoved {

    record NodeInserted(long nodeId, Object value) implements TreeStructureEvent {}

    record NodeRemoved(long nodeId, Object value) implements TreeStructureEvent {}

    record ValueChanged(long nodeId, Object previousValue, Object value) implements TreeStructureEvent {}

    record LeftChanged(long nodeId, Long previousChildId, Long childId) implements TreeStructureEvent {}

    record RightChanged(long nodeId, Long previousChildId, Long childId) implements TreeStructureEvent {}

    record RootChanged(Long previousRootId, Long rootId) implements TreeStructureEvent {}

    record ChildInserted(long parentId, int index, long childId, Object value) implements TreeStructureEvent {}

    record ChildRemoved(long parentId, int index, long childId, Object value) implements TreeStructureEvent {}
}
