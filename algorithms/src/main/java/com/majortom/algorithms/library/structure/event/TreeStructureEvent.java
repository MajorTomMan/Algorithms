package com.majortom.algorithms.library.structure.event;

import com.majortom.algorithms.core.event.ExecutionEvent;

public sealed interface TreeStructureEvent extends ExecutionEvent
        permits TreeStructureEvent.Inserted, TreeStructureEvent.Removed,
        TreeStructureEvent.RotatedLeft, TreeStructureEvent.RotatedRight {
    record Inserted(long nodeId, Object value) implements TreeStructureEvent {}
    record Removed(long nodeId, Object value) implements TreeStructureEvent {}
    record RotatedLeft(long rootId, long replacementId) implements TreeStructureEvent {}
    record RotatedRight(long rootId, long replacementId) implements TreeStructureEvent {}
}
